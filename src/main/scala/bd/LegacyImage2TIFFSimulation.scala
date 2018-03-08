package bd

import scala.concurrent.duration._
import io.gatling.commons.validation._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import bd.BrownDogAPI._
import ciber.CiberQueryBuilder

class LegacyImage2TIFFSimulation extends Simulation {

  // Data: 1100 random paths, less than 20GB files, including listed extensions
  val randomSeed = new java.lang.Float(.19855721)
  val cqbiter = new CiberQueryBuilder().limit(1000).randomSeed(randomSeed).minBytes(100).maxBytes(20e6.toInt).includeExtensions("TARGA", "PICT", "BMP", "PSD", "TGA", "PCT", "EPS", "MACPAINT", "MSP", "PCX").iterator()
  val feeder = Iterator.continually({ Map("FILE_PATH" -> cqbiter.next) })

  val scnFeedToBD = scenario("legacyImage2TIFF")
    .feed(feeder)
    .exec(initActions)
    .exec( session => {
      val path = session(FILE_PATH).as[String]
      val extension = path.substring(path.lastIndexOf(".") + 1).toLowerCase()
      session.set(INPUT_FILE_EXTENSION, extension)
      .set(OUTPUT_FILE_EXTENSION, "tiff")
    })
    .exec(assertConvertable).exitHereIfFailed
    .exec(convertByFilePath)

  setUp(
    scnLogin.inject(atOnceUsers(1),nothingFor(30 seconds)),
    scnFeedToBD.inject(
        atOnceUsers(20),
        constantUsersPerSec(1).during(80)))
    .protocols(httpProtocol)
}
