FROM docker.elastic.co/beats/metricbeat:6.0.0
COPY metricbeat.yml /usr/share/metricbeat/
USER root
RUN chown root /usr/share/metricbeat/metricbeat.yml && chmod go-w /usr/share/metricbeat/metricbeat.yml
