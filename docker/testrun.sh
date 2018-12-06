#!/bin/sh
echo "Running Gatling load simulation."
/usr/share/gatling-charts-highcharts-bundle-2.3.0/bin/gatling.sh --simulation $SIM_CLASS
echo "Piping Gatling logs into Logstash."
cat - /usr/share/gatling-charts-highcharts-bundle-2.3.0/results/*/simulation.log <<END | /usr/share/logstash-6.4.2/bin/logstash --path.settings /usr/share/logstash-6.4.2/config -f /gatling_logstash.conf
TESTRUN_DATA ##${TESTRUN_DATA}## \\
END
echo "All data has been piped to logstash."
touch /test-output/DONE
