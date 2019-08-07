#!/bin/sh

docker run -e SIM_CLASS="ldp.StressTestIngest" -e LDP_URL="http://trellis:8080" \
-e ELASTICSEARCH_URL="http://elasticsearch:9200/" \
-e LOCAL_PATH_PREFIX="/srv/ciber" \
-v ~/tmp/stress-test-data/:/srv/ciber/stress-test-data \
-e INDEX_NAME="drastic-solid-server-results" \
--name gatling-performance-tests \
--net performance-net \
--rm \
gregjan/gatling-testbed-worker:0.5.0-SNAPSHOT
