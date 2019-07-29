#!/bin/sh

# The output direct where the test reports will be stored
OUTPUT_DIR=~/tmp/stress-results

mkdir -p $OUTPUT_DIR

docker run -e SIM_CLASS="ldp.StressTestIngest" -e LDP_URL="http://trellis:8080" \
-v $OUTPUT_DIR:/usr/share/gatling-charts-highcharts-bundle-2.3.0/results \
--name gatling-performance-tests \
--net performance-net \
--rm \
$1
