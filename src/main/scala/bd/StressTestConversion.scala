package bd

import scala.concurrent.duration._
import io.gatling.commons.validation._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import bd.BrownDogAPI._
import scala.util.Random
import java.net.URLEncoder
import umd.ciber.ciber_sampling.CiberQueryBuilder

class StressTestConversion extends Simulation {

  // Data: Unlimited newly random slice as URLs, files less than 20GB, exclude SHX and SHP
  val cqbiter = new CiberQueryBuilder().makePublicURLs().limit(5000).minBytes(100).maxBytes(20e6.toInt).excludeExtensions("SHX", "SHP").iterator()
  val feeder = Iterator.continually({ Map(FILE_URL -> cqbiter.next) })

  val scnConvert = scenario("browndog")
    .feed(feeder)
    .exec(initActions)
    .exec( session => {
      val path = session(FILE_URL).as[String]
      val extension = path.substring(path.lastIndexOf(".") + 1).toLowerCase()
      session.set(INPUT_FILE_EXTENSION, extension)
    })
    .exec(getConversionOutputs)
    // Pick one of the conversion formats offered
    .exec( session => {
      val outputs = session(CONVERSION_OUTPUTS).as[Array[String]]
      session.set(OUTPUT_FILE_EXTENSION, Random.shuffle(outputs.toList).head)
    })
    // Proceed only if a conversion format is available.
    .doIf( session => { session.contains(OUTPUT_FILE_EXTENSION) && session(OUTPUT_FILE_EXTENSION).as[String].trim().length() > 0 })(
      exec(convertByFileURL))
    // Proceed only if a URL was returned (not a quota reached message)
    .doIf( session => { session.contains(OUTPUT_FILE_URL) && session(OUTPUT_FILE_URL).as[String].startsWith("http") })(exec(pollForDownload))

  setUp(
    scnLogin.inject(atOnceUsers(1),nothingFor(30 seconds)),
    scnConvert.inject(
        atOnceUsers(1),
        nothingFor(30 seconds),
        rampUsersPerSec(1) to(50) during(30 minutes)
    )).protocols(httpProtocol)
}
