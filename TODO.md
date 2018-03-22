* TODO: make sure we get one ES index per test run (via sim name appended to index name)
* DONE: develop generic dashboard with from: to: variables passed in from simulation range or focus on 1 index (sim) by name
* DONE: ES query to get the time range of events in a simulation index..
* DONE: Capture scenario duration for each USER on END event. (math from start time)

Simulations to develop:
* SIM: crawl to random file, extract type, convert to preservation format
* SIM: crawl to random file, extract type, convert to browser accepted format
* SIM: check that convert/pdf list includes a goal percentage of the document formats in CiBER (query UDFR)
* SIM: feed fedora collection into Medici, put extracted metadata into Fedora

Other items:
* DONE Figure out how to fail test with scenario-specific data (transformOption returns Validation)
* DONE Add local test files and inject them into Polyglot Sim
* DONE Add Spring DI config
* DONE Add unit tests for developing sims
* TODO List and link to pages for all simulations
* TODO Simulation landing pages that explain the how and why of scenario, link reports
* TODO Add latest gatling reports to the web dashboard (AJAX reports service)
* TODO Trigger all tests nightly in webapp with scheduler
* TODO Verify nightly tests working
