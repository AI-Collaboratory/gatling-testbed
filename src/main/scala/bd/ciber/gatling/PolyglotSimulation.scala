package bd.ciber.gatling

import scala.concurrent.duration._

import io.gatling.core.validation._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class PolyglotSimulation extends Simulation {
  val alloy: AlloySimulation = new AlloySimulation()
  val dapUrl = "http://dap1.ncsa.illinois.edu:8184/"
  val httpProtocol = http.baseURL(dapUrl).disableWarmUp
  val headers_any = Map("Accept" -> "*/*")
  val parseLines = (optString: String) => optString.split("\n")
  val hasExtension = (path: String) => path.substring(path.lastIndexOf("/")).contains(".")

  // TODO add http response check for match against txt and pdf
  val scnList = scenario("listConversions")
    .doIf(session => { hasExtension(session("path").as[String]) }) {
      exec(session => {
        val path = session("path").as[String]
        session.set("extension", path.substring(path.lastIndexOf(".")+1))
      })
        .exec(
          http("request_conversions")
            .get("convert/${extension}")
            .headers(headers_any)
            .check(bodyString.transform(parseLines)/* TODO here */.saveAs("conversions")))
        .exec(session => {
          println(session("conversions"))
          session
        })
    }

  val scnAlloyToBD = scenario("browndog")
    .exec(alloy.scnLogin)
    .exec(alloy.scnCrawlToData)
    .exec(scnList)
    
  val scnFeedToBD = scenario("browndog")
    // TODO feed in testfiles.csv
    .exec(scnList)

  setUp(scnAlloyToBD.inject(atOnceUsers(20))).protocols(httpProtocol)
}