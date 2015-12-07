package bd.ciber.gatling

import scala.concurrent.duration._
import io.gatling.core.validation._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import io.gatling.core.validation.Validation
import io.gatling.core.validation.Validation
import io.gatling.core.validation.Validation

class Documents2PDFSimulation extends Simulation {
  final val LOG = org.slf4j.LoggerFactory.getLogger("documents2pdf");
  val dapUsername = System.getProperty("dapUsername");
  val dapPassword = System.getProperty("dapPassword");
  val dapUrl = System.getProperty("dapUrl");
  
  val httpProtocol = http.baseURL(dapUrl).disableWarmUp.basicAuth(dapUsername, dapPassword)
  LOG.info("Got Brown Dog AuthN username "+dapUsername+", password "+dapPassword)
  val headers_text = Map("Accept" -> "text/plain")
  
  val randomSeed = new java.lang.Float(.39855721)
  val ciberIndex = new bd.ciber.testbed.CiberIndex();
  ciberIndex.setMongoClient(new com.mongodb.MongoClient());
  val samples = ciberIndex.get(1000, randomSeed, 100, 20e6.toInt, "DOC", "DOCX", "ODF", "RTF", "WPD", "WP", "LWP", "WSD");
  val feeder = Iterator.continually(Map("path" -> (samples.next)))
        
      
  val includesMyExtension = (conversionInputs: Option[String], session: Session) => {
    val path = session("path").as[String]
    val extension = path.substring(path.lastIndexOf(".") + 1).toLowerCase()
    conversionInputs.get.split("\n") contains extension match {
      case true => Option[String]("").success
      case false => ("No PDF conversion for " + extension + " at " + path).failure
    }
  }

  val hasExtension = (path: String) => path.substring(path.lastIndexOf("/")).contains(".")

  val assertPDFConvertable = scenario("assertPDFConvertable")
    .doIf(session => { hasExtension(session("path").as[String]) }) {
      exec(
        http("getInputsForPDF")
          .get("convert/pdf/")
          .headers(headers_text)
          .check(bodyString.transformOption[String](includesMyExtension)))
    }

  val convertToPDF = scenario("convertToPDF")
    .exec(
      http("postFile")
        .post("convert/pdf/")
        .headers(headers_text)
        .formUpload("file", "/srv/xfer/${path}")
        .check(bodyString.exists.saveAs("conversionResult")))

  val scnFeedToBD = scenario("browndog")
    .feed(feeder).exec()
    .exec(assertPDFConvertable)
    .exec(convertToPDF)

  setUp(scnFeedToBD.inject(atOnceUsers(20),constantUsersPerSec(1).during(80))).protocols(httpProtocol)
}