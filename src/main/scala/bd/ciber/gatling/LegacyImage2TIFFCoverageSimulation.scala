package bd.ciber.gatling

import scala.concurrent.duration._
import io.gatling.core.validation._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import io.gatling.core.validation.Validation
import io.gatling.core.validation.Validation
import io.gatling.core.validation.Validation

class LegacyImage2TIFFCoverageSimulation extends Simulation {
  final val LOG = org.slf4j.LoggerFactory.getLogger("legacyimage2tiffcoverage");
  val bdusername = System.getProperty("bdusername");
  val bdpassword = System.getProperty("bdpassword");
  val dapUrl = "http://dap-dev.ncsa.illinois.edu:8184/"
  val httpProtocol = http.baseURL(dapUrl).disableWarmUp.basicAuth(bdusername, bdpassword)
  LOG.info("Got Brown Dog AuthN username "+bdusername+", password "+bdpassword)
  val headers_text = Map("Accept" -> "text/plain")
  val randomSeed = Math.random.toFloat
  val ciberIndex = new bd.ciber.testbed.CiberIndex();
  ciberIndex.setMongoClient(new com.mongodb.MongoClient());
     
  val samples = ciberIndex.get(1100, randomSeed, 100, 20e6.toInt, "TARGA", "PICT", /*"WMF",*/ "BMP", /*"PSD",*/ "TGA", "PCT", "EPS", "MACPAINT", "MSP", "PCX")
  val dummy = samples.next
  val feeder = Iterator.continually(
      if(samples hasNext) { 
        Map("path" -> ( samples.next ))
      } else {
        Map("path" -> dummy)
      }
    )
  
  val includesMyExtension = (conversionInputs: Option[String], session: Session) => {
    val path = session("path").as[String]
    val extension = path.substring(path.lastIndexOf(".") + 1).toLowerCase()
    conversionInputs.get.split("\n") contains extension match {
      case true => Option[String]("").success
      case false => ("No TIFF conversion for " + extension + " at " + path).failure
    }
  }

  val hasExtension = (path: String) => path.substring(path.lastIndexOf("/")).contains(".")

  val assertTIFFConvertable = scenario("assertTIFFConvertable")
    .doIf(session => { hasExtension(session("path").as[String]) }) {
      exec(
        http("getInputsForTIFF")
          .get("convert/tiff/")
          .headers(headers_text)
          .check(bodyString.transformOption[String](includesMyExtension)))
    }

  val convertToTIFF = scenario("convertToTIFF")
    .exec(
      http("postFile")
        .post("convert/tiff/")
        .headers(headers_text)
        .formUpload("file", "/srv/xfer/${path}")
        .check(bodyString.exists.saveAs("conversionResult")))

  val scnFeedToBD = scenario("browndog")
    .feed(feeder)
    .exec(assertTIFFConvertable)
    .exec(convertToTIFF)

  setUp(scnFeedToBD.inject(atOnceUsers(20),constantUsersPerSec(1).during(980))).protocols(httpProtocol)
}