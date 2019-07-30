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


## Set up

Install Docker (see: https://docs.docker.com/install/overview/)

If you are running docker on a Linux machine you can configure the following optional post install steps:
- Manage Docker as a non-root user
- Configure Docker to start on boot

See: https://docs.docker.com/install/linux/linux-postinstall/ for more information on these setups.


## Running tests against Trellis
Run the following commands:

This builds the project and create thes docker image with the testbed:
```shell
mvn clean install
```

Create a separate docker network for the containers:
```shell
docker network create performance-net
```

Start the trellis container:
```shell
docker run --name trellis --net performance-net trellisldp/trellis
```
Find the gregjan/gatling-testbed-worker image ID:
```shell
docker image ls
```
Response:
<pre>
REPOSITORY                       TAG                 IMAGE ID            CREATED             SIZE
gregjan/gatling-testbed-worker   x.x.x-SNAPSHOT     <b>ad4540cebd41</b>        5 hours ago         993MB
</pre>

Start the testbed container
<pre>
./run.sh <<b>built testbed image id</b>>
</pre>

## Test Results
The results of the tests will be saved in your <b>~/tmp/stress-results folder</b>. Inside this directory a new folder will be created for each test run in the format <b>stresstestingest-<timestamp></b>. You can change this output directory in the the run.sh script.
  
Here will contain a <b>simulation.log</b> that contains the raw data of the test. Also, there is an <b>index.html</b> that contains graphs of the performance tests.
