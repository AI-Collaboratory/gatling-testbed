package bd.ciber.gatling

import scala.concurrent.duration._
import io.gatling.commons.validation._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import bd.ciber.gatling.BrownDogAPI._
import bd.ciber.testbed.CiberQueryBuilder

class Documents2PDFCoverageSimulation extends Simulation {
  
  // Data: 1100 random paths, less than 20GB files, including listed extensions
  val cqbiter = new CiberQueryBuilder().limit(1100).minBytes(100).maxBytes(20e6.toInt).includeExtensions("DOC", "DOCX", "ODF", "RTF", "WPD", "WP", "LWP", "WSD").iterator()
  val feeder = Iterator.continually({ Map("FILE_PATH" -> cqbiter.next) })

  val scnFeedToBD = scenario("Documents2PDFCoverageScenario")
    .feed(feeder)
    .exec(initActions)
    .exec( session => { 
      session.set("OUTPUT_FILE_EXTENSION", "pdf")
      val path = session("FILE_PATH").as[String]
      val extension = path.substring(path.lastIndexOf(".") + 1).toLowerCase()
      session.set("INPUT_FILE_EXTENSION", extension)
    })
    .exec(assertConvertable)
    .exec(convertByFilePath)

  setUp(
    scnLogin.inject(atOnceUsers(1),nothingFor(30 seconds)),
    scnFeedToBD.inject(
        atOnceUsers(20),
        constantUsersPerSec(1).during(980))
  ).protocols(httpProtocol)
}