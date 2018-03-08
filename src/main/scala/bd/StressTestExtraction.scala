package bd

import scala.concurrent.duration._
import io.gatling.commons.validation._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import bd.BrownDogAPI._
import java.net.URLEncoder
import ciber.CiberQueryBuilder

class StressTestExtraction extends Simulation {

  // Data: Unlimited newly random slice of URLs, less than 20GB files, excluding SHX and SHP
  val cqbiter = new CiberQueryBuilder().makePublicURLs().minBytes(100).maxBytes(20e6.toInt).excludeExtensions("SHX", "SHP").iterator()
  val feeder = Iterator.continually({ Map("FILE_URL" -> cqbiter.next) })

  val scnExtract = scenario("browndog")
    .feed(feeder)
    .exec(initActions)
    .exec(extractByFileURL)

  setUp(
    scnLogin.inject(atOnceUsers(1),nothingFor(30 seconds)),
    scnExtract.inject(
        atOnceUsers(1),
        nothingFor(1 minutes),
        rampUsersPerSec(1) to(20) during(5 minutes)
    )).protocols(httpProtocol)
}
