package bd.ciber.gatling

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

import org.slf4j.LoggerFactory

class IndigoSimulation extends Simulation {
  val LOG = LoggerFactory.getLogger("bd.ciber.gatling.IndigoSimulation");
  val indigoUrl = "http://ciber.umd.edu/api/cdmi"
  val startPath = "/Archive/ciber/";
  val indigousername = System.getProperty("indigousername");
  val indigopassword = System.getProperty("indigopassword");
  val httpIndigoProtocol = http.baseURL(indigoUrl).disableWarmUp.basicAuth(indigousername, indigopassword)
  val headers_any = Map("Accept" -> "*/*")
  val headers_container = Map(
      "Accept" -> "application/cdmi-container",
      "" -> "")

  val scnLogin = scenario("indigo-login").exec(
    http("request_login")
      .post(indigoUrl + "login")
      .headers(headers_any)
      .formParam("username", indigousername)
      .formParam("password", indigopassword))

  val shuffle = (list: Seq[String]) => util.Random.shuffle(list)

  val scnList = scenario("indigo-list")
    .exec(session => {
      val list = List()
      session.set("children", list)
    })
    .exec(http("request_container")
      .get(indigoUrl + "${path}")
      .headers(headers_container)
      .check(jsonPath("$.children[*]").findAll.transform(shuffle).exists.saveAs("children")))
      .exitHereIfFailed

  val isfolder = (list: Seq[String]) => list.head.endsWith("/")

  val scnCrawlToData = scenario("indigo-crawl")
    .exec(session => { session.set("path", startPath) })
    .asLongAs(session => { session.get("path").as[String].endsWith("/") })(
      exec(scnList)
        .doIfOrElse(session => {
          session.contains("children") && session.get("children").as[Seq[String]].length > 0
        }) {
          exec(
            session => {
              val path = session.get("path").as[String]
              val raw = session.get("children").as[Seq[String]].head
              val trim = raw.replaceAll("/", "");
              val enc = java.net.URLEncoder.encode(trim, "UTF-8")
                .replaceAll("\\+", "%20")
              if (raw.endsWith("/")) {
                session.set("path", path + enc + "/")
              } else {
                session.set("path", path + enc)
              }
            })
        } { exec(session => { session.set("path", "") }) })

  val scnStart = scenario("testCDMICrawlToData")
    //.exec(scnLogin)
    .exec(scnCrawlToData)
    .exec(session => {
      println("Got Data Path: " + session("path").as[String])
      session
    })

  setUp(scnStart.inject(atOnceUsers(100))).protocols(httpIndigoProtocol)
}
