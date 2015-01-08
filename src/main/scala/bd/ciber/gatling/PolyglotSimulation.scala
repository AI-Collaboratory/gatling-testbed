package bd.ciber.gatling

import scala.concurrent.duration._
import io.gatling.core.validation._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import io.gatling.core.validation.Validation
import io.gatling.core.validation.Validation
import io.gatling.core.validation.Validation

class PolyglotSimulation extends Simulation {
  val alloy: AlloySimulation = new AlloySimulation()
  val dapUrl = "http://dap1.ncsa.illinois.edu:8184/"
  val httpProtocol = http.baseURL(dapUrl).disableWarmUp
  val headers_text = Map("Accept" -> "text/plain")

  val includesMyExtension = (conversionInputs: Option[String], session: Session) => {
    val path = session("path").as[String]
    val extension = path.substring(path.lastIndexOf(".") + 1)
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
        .formUpload("file", "${path}")
        .check(bodyString.exists.saveAs("conversionResult")))

  val sampleFiles = new java.io.File("src/test/resources/sample-files").listFiles().toIterator
  val feeder = Iterator.continually(Map("path" -> (sampleFiles.filter(_.isFile).next.getAbsolutePath)))

  val scnAlloyToBD = scenario("browndog")
    .exec(alloy.scnLogin)
    .exec(alloy.scnCrawlToData)
    .exec(assertPDFConvertable)

  val scnFeedToBD = scenario("browndog")
    .feed(feeder)
    .exec(assertPDFConvertable)
    .exec(convertToPDF)

  setUp(scnFeedToBD.inject(atOnceUsers(5))).protocols(httpProtocol)
}