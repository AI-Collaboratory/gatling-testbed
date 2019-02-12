FROM openjdk:8-jre
MAINTAINER Gregory Jansen <jansen@umd.edu>

# Install Logstash
RUN curl https://artifacts.elastic.co/downloads/logstash/logstash-6.5.3.tar.gz \
  | tar -xzC /usr/share/

# Install Gatling
RUN curl https://repo1.maven.org/maven2/io/gatling/highcharts/gatling-charts-highcharts-bundle/2.3.0/gatling-charts-highcharts-bundle-2.3.0-bundle.zip \
  -o /tmp/gatling.zip
RUN unzip -d /usr/share/ /tmp/gatling.zip

# Add config files
COPY docker/gatling.conf /usr/share/gatling-charts-highcharts-bundle-2.3.0/etc/gatling.conf
COPY docker/gatling_logstash.conf /gatling_logstash.conf

# Modify Gatling script
COPY docker/gatling.sh.prepend /gatling.sh.prepend
RUN cat /usr/share/gatling-charts-highcharts-bundle-2.3.0/bin/gatling.sh >> /gatling.sh.prepend
RUN cp /gatling.sh.prepend /usr/share/gatling-charts-highcharts-bundle-2.3.0/bin/gatling.sh

# Add run script
COPY docker/testrun.sh /testrun.sh
RUN chmod a+x /testrun.sh

# Will mount any test system log output at /logs, i.e. tomcat logs
RUN mkdir /logs

# Add the testbed library
ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/gatling-charts-highcharts-bundle-2.3.0/lib/testbed.jar
ENTRYPOINT ["/testrun.sh"]

# Add Git information
ARG GIT_COMMIT_ID_DESCRIBE
ARG GIT_ORIGIN_REMOTE_URL
LABEL git.url=${GIT_ORIGIN_REMOTE_URL}
LABEL git.commit=${GIT_COMMIT_ID_DESCRIBE}
