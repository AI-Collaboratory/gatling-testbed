#!/bin/bash

# ToDo: use set -u here and declare variables before use

echo "Running Gatling load simulation."
/usr/share/gatling-charts-highcharts-bundle-${GATLING_VERSION}/bin/gatling.sh --simulation $SIM_CLASS
echo "Piping Gatling logs into Logstash."
cat - /usr/share/gatling-charts-highcharts-bundle-${GATLING_VERSION}/results/*/simulation.log <<END | /usr/share/logstash-6.5.3/bin/logstash --path.settings /usr/share/logstash-6.5.3/config -f /gatling_logstash.conf
TESTRUN_DATA ##${TESTRUN_DATA}## \\
END
echo "All data has been piped to logstash."

