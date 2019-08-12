## Overview
This Gatling Testbed is a performance testing application that periodically runs a battery of tests simulating the use of a test system. The results are indexed into Elasticsearch for later analysis and presentation.

The Testbed samples the diverse files in the CI-BER collections to gather test data that reflects real world case studies. Each simulation targets a particular kind of user of the service. Simulations consist of many tests that are run in parallel to simulate web-scale server load.

The testbed was designed for testing Brown Dog services and for testing Fedora on distributed systems (Trellis and DRAS-TIC Fedora).

## Current Technologies
* Simulations defined in Gatling.io framework
* Results in Elasticsearch indices via logstash:
 * Gatling RUNS, USERS, GROUPS, REQUESTS are all indexed.
* Cron wakes up each hour to setup/run next test battery. (Subject to change)

## Simulations in Development
* On-demand conversion to a web format
 * Walk hierarchy to random file
 * Obtain list of conversions
 * Compare with browser acceptable formats
* Verify migration path exists and works to support the Archivematica preservation policy
 * https://www.archivematica.org/wiki/Media_type_preservation_plans
 * Data set will reflect the most frequently occurring formats in CI-BER collection
 * Note: A better data set would contain most frequently occuring subformats as identified by a characterization tool.


# Setting up in a Dev environment

## Install Docker
Install Docker (see: https://docs.docker.com/install/overview/)

If you are running docker on a Linux machine you can configure the following optional post install steps:
- Manage Docker as a non-root user
- Configure Docker to start on boot

See: https://docs.docker.com/install/linux/linux-postinstall/ for more information on these setups.

## Setting up Network

Create a separate docker network for the containers:
```shell
docker network create performance-net
```

## Setting up Elastic Search

```shell
docker pull elasticsearch:7.0.0

docker run -d -p 9200:9200 -p 9300:9300 -it -h elasticsearch -e "discovery.type=single-node" --name elasticsearch --rm --net performance-net elasticsearch:7.0.0

curl http://localhost:9200/

Create an Index (This will be created later when the tests are run so this step can be skipped)
curl -X PUT http://localhost:9200/drastic-solid-server-results
```

 
**Optional Step**

```shell
docker run -d  -p 5601:5601 -h kibana --name kibana --rm --net performance-net kibana:6.7.1
```

http://localhost:5601

### Create test data and index it in Elasticsearch with Logstash
```shell

docker build --tag=drastic/sample-data sampleData/

docker run -e ELASTICSEARCH_URL="http://elasticsearch:9200" -d -v nfs-ciber:/export/ciber --rm --net performance-net drastic/sample-data:latest
```

## Setup Cassandra container and load schema needed for Trellis
Run the following commands:

```shell
docker pull trellisldp/trellis-cassandra-init:0.8.1-SNAPSHOT

docker run --name cassandra -p 9042:9042 --network performance-net --rm -d trellisldp/trellis-cassandra-init:0.8.1-SNAPSHOT

docker exec -i cassandra cqlsh -f /load.cql
```
**Note:** you need to wait for cassandra to full start before you run the script or else you will get errors.

## Checking the schema in the Cassandra container

```shell
docker run -it --network performance-net --rm cassandra cqlsh cassandra

cqlsh> USE trellis;
cqlsh> DESCRIBE tables;

```

## Running Trellis Cassandra LDP Server

Start the trellis container:
```shell
docker run -e CASSANDRA_CONTACT_ADDRESS="cassandra" -e CASSANDRA_CONTACT_PORT="9042" --name trellis -p 6060:8080 --net performance-net --rm trellisldp/trellis-cassandra:0.8.1-SNAPSHOT
```


## Build the Test Bed

This builds the project and create a docker image containing the Gatling test suites:

The Test Bed has a dependency on a small library that is used for searching for sample test data from Elasticsearch.

```shell

```
Then you can build the Test bed.

```shell
mvn clean install
```

Start the testbed container
```shell
./run.sh
```

## Setting up Grafana

Install and run Grafana:

```shell
docker build --tag=drastic/grafana grafana/

docker run --name grafana -p 3000:3000 --rm -v $(pwd)/grafana/provisioning:/etc/grafana/provisioning --net performance-net drastic/grafana
```

http://localhost:3000


## MetricBeat

```shell
docker build --build-arg configFile=metricbeat-dev.yml --tag=drastic/metricbeat metricbeat/

docker run -d --name metricbeat --net performance-net --rm --volume=/var/run/docker.sock:/var/run/docker.sock drastic/metricbeat
```
