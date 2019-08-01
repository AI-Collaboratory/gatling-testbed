#!/bin/sh

# The output direct where the test reports will be stored
OUTPUT_DIR=~/tmp/stress-results

mkdir -p $OUTPUT_DIR

docker run -e SIM_CLASS="ldp.StressTestIngest" -e LDP_URL="http://trellis:8080" \
-e ELASTICSEARCH_URL="http://elasticsearch:9200/" \
-e INDEX_NAME="drastic-solid-server-results" \
-v $OUTPUT_DIR:/usr/share/gatling-charts-highcharts-bundle-3.0.3/results \
--name gatling-performance-tests \
--net performance-net \
--rm \
gregjan/gatling-testbed-worker:0.5.0-SNAPSHOT
