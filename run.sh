#!/bin/sh

docker run -e SIM_CLASS="ldp.StressTestIngest" -e LDP_URL="http://172.17.0.1:4040" \
-v ~/tmp/stress-results:/usr/share/gatling-charts-highcharts-bundle-2.3.0/results \
$1
