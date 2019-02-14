package bd

import scala.concurrent.duration._

import io.gatling.commons.validation._
import io.gatling.commons.validation.Validation
import scala.collection.mutable.Queue

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

//import io.gatling.core.validation.Validation
//import io.gatling.http.check.HttpCheck

import org.slf4j.LoggerFactory

class ExtractCollectionSimulation extends Simulation {
  val LOG = LoggerFactory.getLogger("bd.ciber.gatling.IndigoSimulation");
  val cdmiProxyUrl = System.getProperty("cdmiProxyUrl");
  val dtsUrl = System.getProperty("dtsUrl");
  val commkey = System.getProperty("dtsCommKey");
  val startPath = System.getProperty("dtsTestPath1");
  LOG.info("Using DTS at URL: "+dtsUrl)

  val httpProtocol = http.baseUrl(cdmiProxyUrl).disableWarmUp
  val headers_accept_json = Map("Accept" -> "application/json", "Content-type" -> "application/json")
  val headers_any = Map(
      "X-CDMI-Specification-Version" -> "1.1",
      "Accept" -> "*/*")
  val headers_container = Map(
      "X-CDMI-Specification-Version" -> "1.1",
      "Accept" -> "application/cdmi-container")

  val filePaths = Queue[String]()
  val feeder = Iterator.continually( Map("path" -> ( filePaths.dequeue )) )

  val scnList = scenario("indigo-list")
    .exec( session => {
      LOG.info( "Getting list for: " + session("path").as[String] )
      session.set("children", Seq())
    })
    .exec(http("request_container")
      .get("${path}")
      .headers(headers_container)
      .check(jsonPath("$.children[*]").findAll.exists.saveAs("children"))
    ) // removed .exitHereIfFailed b/c scenario must continue past empty dir
    .exec( session => {
        val path = session("path").as[String]
        val children = session("children").as[Seq[String]].map( x => {
            val enc = java.net.URLEncoder.encode(x.replaceAll("/", ""), "UTF-8")
                  .replaceAll("\\+", "%20")
            if (x.endsWith("/")) {
                path + enc + "/"
            } else {
                path + enc
            }
          } )
        session.set("children", children)
      }
    )

  val statusTransformOption = (input: Option[String], session: Session) => {
    val path = session("path").as[String]
    val extension = path.substring(path.lastIndexOf(".") + 1).toLowerCase()
    input.get match {
      case "Done" => Success(Option("Done"))
      case "Processing" => Success(Option("Processing"))
      case "Required Extractor is either busy or is not currently running. Try after some time." =>
        Failure("An extractor was busy or not running for " + extension + " at " + path)
      case x => Failure("The DTS failed to extract for "+ extension + " at " + path + " with status: "+x)
    }
  }

  val scnPostFileToExtract = scenario("post-file-to-extract")
    .feed(feeder)
    .exec(http("postUrl")
        .post(dtsUrl + "/api/extractions/upload_url?key="+commkey)
        .headers(headers_accept_json)
        .body(StringBody("{ \"fileurl\": \"${path}\" }"))
        .check(jsonPath("$.id").ofType[String].saveAs("id"))
    ).exitHereIfFailed
    .exec( session => {
      val resp = session("id").as[String]
      LOG.info(resp+" has path: "+ session("path").as[String])
      session
    })
    .exec(session => { session.set("status", "") })
    .asLongAs( session => { session("status").as[String].toLowerCase() != "done" }) (
      exec(http("pollUrl")
        .get(dtsUrl + "/api/extractions/${id}/status?key="+commkey)
        .headers(headers_accept_json)
        .check(jsonPath("$.Status").ofType[String].transformOption(statusTransformOption)
            .saveAs("status"))
      ).exitHereIfFailed
      .exec(session => {
          LOG.info(session("status").as[String])
          session
        }
      ).pause(2)
    )


  val scnLevelFirstCrawl = scenario("level-first-crawl")
    .exec(session => {
        LOG.info("Starting crawl at: " + cdmiProxyUrl + startPath)
        session.set("pathQueue", Queue[String]()).set("path", cdmiProxyUrl + startPath)
      }
    )
    .asLongAs(session => { session("path").as[String] != "" }
    )(
      doIfOrElse(session => {
        !session("path").as[String].endsWith("/")
      })
      (exec(session => {
        filePaths.enqueue( session("path").as[String] )
        session
        }))
      (
        exec(scnList)
        .exec( session => {
            val children = session("children").as[Seq[String]]
            val pathQueue = session("pathQueue").as[Queue[String]]
            children foreach { x =>
              pathQueue enqueue x
            }
            session
          }
        )
      )
      .exec( session => {
          val pathQueue = session("pathQueue").as[Queue[String]]
          var next = ""
          try {
            next = { pathQueue dequeue }
          } catch {
            case e: java.util.NoSuchElementException => {}
          }
          session.set("path", next)
        }
      )
    )
    .exec( session => {
      LOG.info("Gathered this many file paths: "+filePaths.size)
      session
    })

  setUp(
      scnLevelFirstCrawl.inject( atOnceUsers(1) ),
      scnPostFileToExtract.inject( nothingFor(500), rampUsers(200) during(300))
  ).protocols(httpProtocol)
}
