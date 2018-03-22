package bd

import scala.concurrent.duration._
import io.gatling.commons.validation._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import bd.BrownDogAPI._
import ciber.CiberQueryBuilder
import java.net.URLEncoder

class ExtractionFormatCoverage extends Simulation {

  // Data: 1 file for each unique extension
  val formats = new CiberQueryBuilder().limit(2000).getUniqueFormats()
  val feeder = Iterator.continually({
    var format:String = formats.next
    Map("FILE_URL" -> new CiberQueryBuilder()
      .makePublicURLs()
      .limit(1)
      .includeExtensions(format).iterator().next)
  })

  val scnExtract = scenario("ExtractionFormatCoverageScenario")
    .feed(feeder)
    .exec(initActions)
    .exec(extractByFileURL)

  setUp(
    scnLogin.inject(atOnceUsers(1),nothingFor(30 seconds)),
    scnExtract.inject(
      constantUsersPerSec(10).during(45)
    )).protocols(httpProtocol)
}
