package umd.ciber.ciber_sampling;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;

public class CiberQueryBuilder implements Iterable<String> {
    private static final Logger log = LoggerFactory.getLogger(CiberQueryBuilder.class);
    private enum DataType { LOCAL_PATHS, PUBLIC_URLS }

    private JestClientFactory factory = null;

    private DataType outputType = DataType.LOCAL_PATHS;
    private int howMany = -1;  // default is no limit
    private float randomSeed = (float)Math.random();
    private Integer maxSize = null;
    private Integer minSize = null;
    private Set<String> excludeExtensions = new HashSet<String>();
    private Set<String> includeExtensions = new HashSet<String>();

    public CiberQueryBuilder() {
        String elasticsearchURL = System.getenv("ELASTICSEARCH_URL");
        // Construct a new Jest client according to configuration via factory
        factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig.Builder(elasticsearchURL).multiThreaded(true)
                .defaultMaxTotalConnectionPerRoute(2).maxTotalConnection(20).build());
    }

    public CiberQueryBuilder makePublicURLs() {
        this.outputType = DataType.PUBLIC_URLS;
        return this;
    }

    public CiberQueryBuilder limit(int limit) {
        if(limit < 1) {
            throw new Error("Limit must be 1 or greater.");
        }
        this.howMany = limit;
        return this;
    }

    public CiberQueryBuilder randomSeed(float seed) {
        this.randomSeed = seed;
        return this;
    }

    public CiberQueryBuilder maxBytes(int max) {
        this.maxSize = new Integer(max);
        return this;
    }

    public CiberQueryBuilder minBytes(int min) {
        this.minSize = new Integer(min);
        return this;
    }

    public CiberQueryBuilder includeExtensions(String... extensions ) {
        for(String ext : extensions) {
            this.includeExtensions.add(ext.toLowerCase());
        }
        return this;
    }

    public CiberQueryBuilder excludeExtensions(String... extensions ) {
        for(String ext : extensions) {
            this.excludeExtensions.add(ext.toLowerCase());
        }
        return this;
    }

    /**
     * Iterates through a sample of the CI-BER data. Will return a consistent
     * sample whenever a same randomSeed is supplied. then
     * @return an iterator of local paths (by default) or public URLs.
     */
    @Override
    public Iterator<String> iterator() {

        BoolQueryBuilder qbs = QueryBuilders.boolQuery();

        if (this.includeExtensions.size() > 0) {
            qbs.must(QueryBuilders.termsQuery("extension", this.includeExtensions));
        }

        if (this.excludeExtensions.size() > 0) {
            qbs.mustNot(QueryBuilders.termsQuery("extension", this.excludeExtensions));
        }

        if (this.minSize != null || this.maxSize != null) {
            qbs.must(QueryBuilders.rangeQuery("size").from(this.minSize).to(this.maxSize));
        }

        qbs.must(QueryBuilders.rangeQuery("random").from(this.randomSeed));

        log.warn("query: {}", qbs.toString());

        SearchSourceBuilder ssb = new SearchSourceBuilder().query(qbs)
                .sort("random", SortOrder.ASC);

        Iterator<String> result = new PathIterator(ssb, this.howMany, factory.getObject(), this.outputType);
        return result;
    }


    /**
     * Iterates through a sample of the CI-BER data. Will return a consistent
     * sample whenever a same randomSeed is supplied. then
     *
     * @param howMany
     *            limit the number of results or 0 for no limit
     * @param randomSeed
     *            an offset for sampling at random, or null for new random
     *            sample each time
     * @param minSize
     *            minimum size of files in bytes
     * @param maxSize
     *            maximum size of files in bytes
     * @param excludeExtensions
     *            the undesired file extensions, if any (case insensitive)
     * @return a Gatling-style iterator of mapped "fullpath"
     */
    public Iterator<String> getUniqueFormats() {
        if(this.howMany == -1) {
            this.howMany = 5000;
        }
        String query = "{" +
                "\n\"size\": 0," +
                "\n\"aggs\": {" +
                "\n\"distinct_format\": {" +
                "\n\"terms\": {" +
                "\n\"field\": \"extension\"," +
                "\n\"size\": " + this.howMany +
                "\n}" +
                "\n}" +
                "\n}" +
                "\n}";
        Search search = new Search.Builder(query)
                .addIndex("ciber-inventory")
                .build();
        List<String> formats = new ArrayList<String>();
        try {
            SearchResult result = this.factory.getObject().execute(search);
            JsonArray buckets = result.getJsonObject().getAsJsonObject("aggregations").getAsJsonObject("distinct_format").getAsJsonArray("buckets");
            for(JsonElement j : buckets) {
                String foo = j.getAsJsonObject().get("key").getAsString();
                try {
                    Integer.parseInt(foo);
                    continue;
                } catch(NumberFormatException e) {
                    if(foo.length() < 6) {
                        if(foo.startsWith("OLD") && foo.length() > 3) { continue; }
                        formats.add(foo);
                    }
                }
            }
        } catch (IOException e) {
            throw new Error("Cannot perform next inventory search", e);
        }
        System.out.println("Got this many formats: "+formats.size());
        return formats.iterator();
    }


    private static class PathIterator implements Iterator<String>, Closeable {
        private static String ftpOverHttpUrl = System.getenv("FTP_OVER_HTTP_URL");
        private static String localPathPrefix = System.getenv("LOCAL_PATH_PREFIX");

        private int howMany;
        private SearchSourceBuilder ssb;
        private JestClient client;
        private Iterator<SearchResult.Hit<Map, Void>> hits;
        private int from;
        private int count;
        private DataType outputType;

        public PathIterator(SearchSourceBuilder ssb, int howMany, JestClient client, DataType outputType) {
            this.client = client;
            this.ssb = ssb;
            this.howMany = howMany;
            this.from = 0;
            this.count = 0;
            this.outputType = outputType;
            this.hits = null;
            getMoreHits();
        }

        private void getMoreHits() {
            ssb = ssb.from(this.from).size(500);
            String searchQuery = ssb.toString();
            log.info("ciber-inventory query: {}", searchQuery);
            Search search = new Search.Builder(searchQuery)
                    .addIndex("ciber-inventory")
                    .build();
            try {
                SearchResult result = this.client.execute(search);
                // System.out.println(result.getSourceAsString());
                this.hits = result.getHits(Map.class).iterator();
            } catch (IOException e) {
                log.error("Cannot perform search", e);
                throw new Error("Cannot perform next inventory search", e);
            }
        }

        @Override
        public boolean hasNext() {
            if(this.howMany == -1) {
                if(!this.hits.hasNext()) {
                    this.getMoreHits();
                }
                return this.hits.hasNext();
            } else {
                if(this.count >= this.howMany) {
                    return false;
                } else {
                    if(!this.hits.hasNext()) {
                        this.getMoreHits();
                    }
                    return this.hits.hasNext();
                }
            }
        }

        @Override
        public String next() {
            SearchResult.Hit<Map, Void> hit = null;
            if(this.hasNext()) {
                hit = this.hits.next();
            } else {
                throw new NoSuchElementException("no more hits");
            }
            String result = (String)hit.source.get("fullpath");
            this.count = this.count + 1;
            this.from = this.from + 1;
            result = ((String) result).substring(2);
            switch(this.outputType) {
                case LOCAL_PATHS:
                    result = localPathPrefix + "/" + result;
                    break;
                case PUBLIC_URLS:
                    result = result.replaceAll(" ", "%20");
                    result = ftpOverHttpUrl + "/" + result;
                    break;
            }
            return result;
        }

        @Override
        public void close() throws IOException {
            // fulfills interface
        }

        @Override
        public void remove() {
            // fulfills interface
        }

    }

}
