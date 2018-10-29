#!/bin/sh
echo "Running Gatling load simulation."
/usr/share/gatling-charts-highcharts-bundle-2.3.0/bin/gatling.sh --simulation $SIM_CLASS
echo "Piping Gatling logs into Logstash."
/bin/bash -c "cat - /usr/share/gatling-charts-highcharts-bundle-2.3.0/results/*/simulation.log <<<\"\"\"TESTRUN_DATA ##${TESTRUN_DATA}## \\\"\"\"  | /usr/share/logstash-6.4.2/bin/logstash --path.settings / -f /gatling_logstash.conf"
echo "All data has been piped to logstash."
touch /test-output/DONE
