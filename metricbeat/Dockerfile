FROM docker.elastic.co/beats/metricbeat:6.0.0

ARG configFile=metricbeat.yml

COPY ${configFile} /usr/share/metricbeat/metricbeat.yml
USER root
RUN chown root /usr/share/metricbeat/metricbeat.yml && chmod go-w /usr/share/metricbeat/metricbeat.yml
