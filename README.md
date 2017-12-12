## Overview
This Gatling Testbed is a performance testing application that periodically runs a battery of tests simulating the use of a test system. The results are indexed into Elasticsearch for later analysis and presentation.

The Testbed samples the diverse files in the CI-BER collections to gather test data that reflects real world case studies. Each simulation targets a particular kind of user of the service. Simulations consist of many tests that are run in parallel to simulate web-scale server load.

The testbed was designed for testing Brown Dog services and for testing Fedora on distributed systems (Trellis and DRAS-TIC Fedora).

## Workflow
The server will attempt to run a queued test battery every hour. A battery may include Ansible plays for setup and teardown, as well as a list of pre-compiled simulations to run.

### Queuing Tests
* Github repositories under test may send webhooks to a specific endpoint, either /browndog or /fedora. (Facilitated by Ngrok proxy service.)
* The webhook payload is analyzed briefly by a server process on the gatling server. Server will create a test battery JSON file with setup/destroy details and the names of simulations to run. An example is provided below.
* Test batteries may also be prepared in many other ways, such as a nightly dump of test battery files from a separate directory or a hand made test battery file.

### Running Tests
* A cron job wakes up every hour and exits if same process is already running.. (locked file)
* Looks for oldest file in test battery directory.
* Logstash the test battery file as BEGIN event
* Runs the deployment playbook.
* Logstash the DEPLOYED event??
* Purge any old consolidated simulation logs.
* Runs each of the listed simulations in turn.
* Consolidate simulation logs.
* Pipe consolidated logs to Logstash.
* TODO: Pipe Docker Swarm metrics logs to Logstash.
* Logstash the test battery END event
* Run the destroy deployment playbook.

## Test Battery JSON
```JSON
{
  'subject': 'DRAS-TIC Fedora',
  'notes': 'Trellis running on Cassandra nodes',
  'deploy': {
    'git-repository': 'http://github.com/UMD-DRASTIC/drastic-deploy',
    'commit': '0923409we098we098df',
    'ansible-inventory': '/etc/hosts-drastic',
    'setup-playbook': 'ansible/trellis-swarm-up.yml',
    'destroy-playbook': 'ansible/trellis-swarm-down.yml'
  },
  'tests': [
    'bd.gatling.ciber.StressTestConversion',
    'bd.gatling.ciber.LegacyImage2TIFFConversion'
  ]
}
```

## Current Technologies
* Simulations defined in Gatling.io framework
* Results in Elasticsearch indices via logstash:
 * Gatling RUNS, USERS, GROUPS, REQUESTS are all indexed.
* Cron wakes up each hour to setup/run next test battery.

## Simulations in Development
* On-demand conversion to a web format
 * Crawl to random file
 * Obtain list of conversions
 * Compare with browser acceptable formats
* Verify migration path exists and works to support the Archivematica preservation policy
 * https://www.archivematica.org/wiki/Media_type_preservation_plans
 * Data set will reflect the most frequently occurring formats in CI-BER collection
 * Note: A better data set would contain most frequently occuring subformats as identified by a characterization tool.
