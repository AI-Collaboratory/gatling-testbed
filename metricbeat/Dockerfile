FROM docker.elastic.co/beats/metricbeat:7.2.0

ARG configFile=metricbeat.yml

COPY ${configFile} /usr/share/metricbeat/
USER root
RUN chown root /usr/share/metricbeat/metricbeat.yml && chmod go-w /usr/share/metricbeat/metricbeat.yml
