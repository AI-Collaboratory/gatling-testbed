#!/bin/sh

docker run -e SIM_CLASS="ldp.StressTestIngest" -e LDP_URL="http://enterpriseserver_trellis_1:8080" \
-e ELASTICSEARCH_URL="http://elasticsearch:9200/" \
-e LOCAL_PATH_PREFIX="/srv/ciber" \
-v nfs-ciber:/srv/ciber \
-e INDEX_NAME="drastic-solid-server-results" \
--name gatling-performance-tests \
--net performance-net \
--rm \
gregjan/gatling-testbed-worker:0.5.0-SNAPSHOT
