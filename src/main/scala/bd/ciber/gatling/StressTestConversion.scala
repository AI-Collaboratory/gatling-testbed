package bd.ciber.gatling

import scala.concurrent.duration._
import io.gatling.core.validation._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import io.gatling.core.validation.Validation
import io.gatling.core.validation.Validation
import io.gatling.core.validation.Validation
import bd.ciber.gatling.BrownDogAPI._
import scala.util.Random
import java.net.URLEncoder

class StressTestConversion extends Simulation {
  final val LOG = org.slf4j.LoggerFactory.getLogger("StressTestConversion");
  val FtpOverHttpUrl:String = System.getProperty("ftpOverHttpUrl");
  val httpProtocol = http.disableWarmUp

  val bdUrl = System.getProperty("bdUrl");
  val bdAPIKey = System.getProperty("bdAPIKey");
  val bdUsername = System.getProperty("bdUsername");
  val bdPassword = System.getProperty("bdPassword");
  
  val randomSeed = Math.random.toFloat / 2
  val ciberIndex = new bd.ciber.testbed.CiberIndex
  ciberIndex.setMongoClient(new com.mongodb.MongoClient())
  val samples = ciberIndex.get(0, randomSeed, 100, 20e6.toInt, false, "SHX", "SHP")
  val feeder = Iterator.continually({
    var path:String = samples.next
    Map("FILE_URL" -> FtpOverHttpUrl.concat("/"+URLEncoder.encode(path, "utf-8")))
  }) 

  val scnConvert = scenario("browndog")
    .feed(feeder)
    .exec( session => {
      session.set(BD_URL, bdUrl)
      .set(BD_API_KEY, bdAPIKey)
      .set(USER_NAME, bdUsername)
      .set(USER_PASSWORD, bdPassword)
    })
    .exec(loginWithTokenCaching)
    .exec(getConvertOutputs)
    // Pick one of the conversion formats offered
    .exec( session => {
      val outputs = session(DAP_OUTPUTS).as[Array[String]]
      session.set(OUTPUT_FILE_EXTENSION, Random.shuffle(outputs.toList).head)
    })
    // Proceed only if a conversion format is available. 
    .doIf( session => { session.contains(OUTPUT_FILE_EXTENSION) && session(OUTPUT_FILE_EXTENSION).as[String].trim().length() > 0 })(
      exec(convertByFileURL))
    // Proceed only if a URL was returned (not a quota reached message)
    .doIf( session => { session.contains(OUTPUT_FILE_URL) && session(OUTPUT_FILE_URL).as[String].startsWith("http") })(exec(pollForDownload))

  setUp(
    scnClearTokenCache.inject(atOnceUsers(1),nothingFor(1 seconds)),
    scnConvert.inject(
        atOnceUsers(1),
        nothingFor(30 seconds),
        rampUsersPerSec(1) to(50) during(30 minutes)
    )).protocols(httpProtocol)
}