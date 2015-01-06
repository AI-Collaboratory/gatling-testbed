##Overview
The CI-BER Brown Dog Testbed is an application that periodically runs a battery of tests simulating the use of services offered by Brown Dog. The results are analyzed and presented, so that gaps and problems can be identified by the development team.

The Testbed samples the diverse files in the CI-BER collection to gather test data that reflects real world case studies. Each simulation targets a particular kind of user of the service. Simulations consist of many tests that are run in parallel to simulate future server load. Results are recorded in a database and used to identify functional gaps in Brown Dog and to diagnose individual service failures and performance under high load.

Nightly simulation reports with logs are currently available on the testbed web site. A dashboard is under development that will show performance trends across nightly simulations.

## Current Technologies

* Simulations defined in Gatling.io framework
 * Results in MySQL, reports in HighChart
* Nightly simulation runs
* Test data in Alloy repository
 * accessed over CDMI HTTP API

## Current Simulation
* Crawl Alloy/CDMI to random data file, check that DAP has at least 1 migration format (by file extension)

## Simulations in Development
* On-demand conversion to a web format
 * Crawl to random file
 * Obtain list of conversions
 * Compare with browser acceptable formats
* Verify migration path exists and works to support the Archivematica preservation policy
 * https://www.archivematica.org/wiki/Media_type_preservation_plans
 * Data set will reflect the most frequently occuring formats in CIBER collection
 * Note: A better data set would contain most frequently occuring subformats as identified by a characterization tool.
