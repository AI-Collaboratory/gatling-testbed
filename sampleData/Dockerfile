FROM logstash:7.3.0
MAINTAINER Jarlath Holleran <jarlathh@inrupt.com>

# Add config files
COPY ciber-inventory.conf /

# Copy over sample RDF data files
COPY rdf /local/ciber/stress/rdf

# Copy over the inventory files
COPY inventory.txt /local/ciber/

# Add run script
COPY createTestData.sh /
USER root
RUN chown root /createTestData.sh && chmod go-w /createTestData.sh

ENTRYPOINT ["/createTestData.sh"]
