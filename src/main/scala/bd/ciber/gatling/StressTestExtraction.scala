package bd.ciber.gatling

import scala.concurrent.duration._
import io.gatling.core.validation._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import io.gatling.core.validation.Validation
import io.gatling.core.validation.Validation
import io.gatling.core.validation.Validation

class StressTestExtraction extends Simulation {
  final val LOG = org.slf4j.LoggerFactory.getLogger("StressTestExtraction");
  val cdmiProxyUrl = System.getProperty("cdmiProxyUrl");
  val dtsUrl = System.getProperty("dtsUrl");
  val dtsUsername = System.getProperty("dtsUsername");
  val dtsPassword = System.getProperty("dtsPassword");
  val commkey = System.getProperty("dtsCommKey");
  
  val httpProtocol = http.baseURL(dtsUrl).disableWarmUp.basicAuth(dtsUsername, dtsPassword)
  LOG.info("Got Brown Dog AuthN username "+dtsUsername+", password "+dtsPassword)
  val headers_accept_json = Map("Accept" -> "application/json", "Content-type" -> "application/json")
  
  val randomSeed = new java.lang.Float(.000001)
  val ciberIndex = new bd.ciber.testbed.CiberIndex();
  ciberIndex.setMongoClient(new com.mongodb.MongoClient());
  val samples = ciberIndex.get(0, randomSeed, 100, 20e6.toInt);
  val feeder = Iterator.continually(Map("path" -> (samples.next)))

  val statusTransformOption = (input: Option[String], session: Session) => {
    val path = session("path").as[String]
    val extension = path.substring(path.lastIndexOf(".") + 1).toLowerCase()
    input.get match {
      case "Done" => Success(Option("Done"))
      case "Processing" => Success(Option("Processing"))
      case "Required Extractor is either busy or is not currently running. Try after some time." => 
        Failure("An extractor was busy or not running for " + extension + " at " + path)
      case x => Failure("The DTS failed to extract for "+ extension + " at " + path + " with status: "+x)
    }
  }  
  
  val scnPostFileToExtract = scenario("post-file-to-extract")
  .feed(feeder)
  .exec(http("postUrl")
      .post(dtsUrl + "/api/extractions/upload_url?key="+commkey)
      .headers(headers_accept_json)
      .body(StringBody("{ \"fileurl\": \"${path}\" }"))
      .check(jsonPath("$.id").ofType[String].saveAs("id"))
  ).exitHereIfFailed
  .exec( session => {      
    val resp = session("id").as[String]
    LOG.info(resp+" has path: "+ session("path").as[String])
    session
  })
  .exec(session => { session.set("status", "") })
  .asLongAs( session => { session("status").as[String].toLowerCase() != "done" }) (
    exec(http("pollUrl")
      .get(dtsUrl + "/api/extractions/${id}/status?key="+commkey)
      .headers(headers_accept_json)
      .check(jsonPath("$.Status").ofType[String].transformOption(statusTransformOption)
          .saveAs("status"))
    ).exitHereIfFailed
    .exec(session => {
        LOG.info(session("status").as[String])
        session
      }
    ).pause(2)
  )

  val scnFeedToBD = scenario("browndog")
    .feed(feeder).exec()
    .exec(scnPostFileToExtract)

  setUp(scnFeedToBD.inject(
    rampUsersPerSec(10) to(100) during(30 minutes)
    )).protocols(httpProtocol)
}