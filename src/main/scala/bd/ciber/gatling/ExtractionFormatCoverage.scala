package bd.ciber.gatling

import scala.concurrent.duration._
import io.gatling.commons.validation._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import bd.ciber.gatling.BrownDogAPI._
import bd.ciber.testbed.CiberQueryBuilder
import java.net.URLEncoder

class ExtractionFormatCoverage extends Simulation {
  
  // Data: 1 file for each unique extension
  val formats = new CiberQueryBuilder().makePublicURLs().limit(2000).getUniqueFormats()
  val feeder = Iterator.continually({
    var format:String = formats.next
    Map("FILE_URL" -> new CiberQueryBuilder().makePublicURLs().limit(1).includeExtensions(format).iterator().next)
  })

  val scnExtract = scenario("ExtractionFormatCoverageScenario")
    .feed(feeder)
    .exec(initActions)
    .exec(extractByFileURL)

  setUp(
    scnLogin.inject(atOnceUsers(1),nothingFor(30 seconds)),
    scnExtract.inject(
      atOnceUsers(1),
      nothingFor(1 minutes),
      rampUsersPerSec(1) to(5) during(30 minutes)
    )).protocols(httpProtocol)
}