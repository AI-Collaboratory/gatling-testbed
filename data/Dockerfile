
# Install Logstash
RUN curl https://artifacts.elastic.co/downloads/logstash/logstash-6.5.3.tar.gz \
  | tar -xzC /usr/share/

# Add config files
COPY ciber-inventory.conf /ciber-inventory.conf

# Add run script
COPY docker/testrun.sh /testrun.sh
RUN chmod a+x /testrun.sh

# Will mount any test system log output at /logs, i.e. tomcat logs
RUN mkdir /logs

# Add the ciber-sampling library
ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/gatling-charts-highcharts-bundle-${GATLING_VERSION}/lib/${JAR_FILE}
# ADD src/main/scala /usr/share/gatling-charts-highcharts-bundle-${GATLING_VERSION}/user-files/simulations/
ENTRYPOINT ["/testrun.sh"]
