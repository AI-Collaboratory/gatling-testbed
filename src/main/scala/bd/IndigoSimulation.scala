package bd

import scala.concurrent.duration._
import scala.collection.mutable.Queue

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

import org.slf4j.LoggerFactory

class IndigoSimulation extends Simulation {
  val LOG = LoggerFactory.getLogger("bd.ciber.gatling.IndigoSimulation");
  val cdmiProxyUrl = System.getProperty("cdmiProxyUrl");
  val startPath = System.getProperty("dtsTestPath1");

  val httpProtocol = http.baseUrl(cdmiProxyUrl).disableWarmUp
  val headers_any = Map(
      "X-CDMI-Specification-Version" -> "1.1",
      "Accept" -> "*/*")
  val headers_container = Map(
      "X-CDMI-Specification-Version" -> "1.1",
      "Accept" -> "application/cdmi-container",
      "" -> "")

  val shuffle = (list: Seq[String]) => util.Random.shuffle(list)

  val scnList = scenario("indigo-list")
    .exec( session => {
      // println("Listing Collection: " + session("path").as[String])
      session.set("children", Seq())
    })
    .exec(http("request_container")
      .get("${path}")
      .headers(headers_container)
      .check(jsonPath("$.children[*]").findAll.exists.saveAs("children"))
    ).exitHereIfFailed
    .exec( session => {
        val path = session("path").as[String]
        val children = session("children").as[Seq[String]].map( x => { path + x } )
        session.set("children", children)
      }
    )

  val scnWalkToFile = scenario("indigo-crawl")
    .exec(session => { session.set("path", cdmiProxyUrl + startPath) })
    .asLongAs(session => { session("path").as[String].endsWith("/") })(
      exec(scnList)
        .doIfOrElse(session => {
          session.contains("children") && session("children").as[Seq[String]].length > 0
        }) {
          exec(
            session => {
              val children = session("children").as[Seq[String]]
              // FIXME: shuffle the child order or pick at random
              val path = session("path").as[String]
              val raw = children.head
              val trim = raw.replaceAll("/", "");
              val enc = java.net.URLEncoder.encode(trim, "UTF-8")
                .replaceAll("\\+", "%20")
              session.set("parent", path)
              if (raw.endsWith("/")) {
                session.set("path", path + enc + "/")
              } else {
                session.set("path", path + enc)
              }
            })
        } { exec(session => {
                println("Empty collection, dead end: " + session("path").as[String])
                session
              })
        })

  val scnCrawlAllFiles = scenario("indigo-crawl-all-files")
    .exec(session => {
        session.set("pathQueue", Queue[String]()).set("path", cdmiProxyUrl + startPath)
      }
    )
    .asLongAs(session => { session("path").as[String] != "" }
    )(
      exec(session => {
        println( session("path").as[String] )
        session
      })
      .doIf(session => {
        session("path").as[String].endsWith("/")
      }) (
        exec(scnList)
        .exec( session => {
            val children = session("children").as[Seq[String]]
            val pathQueue = session("pathQueue").as[Queue[String]]
            children foreach { x => pathQueue enqueue x }
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



  val scnStart = scenario("testCDMIWalkToFile")
    .exec(scnWalkToFile)
    .exec(session => {
      println("Got Data Path: " + session("path").as[String])
      session
    })

  setUp(scnCrawlAllFiles.inject(atOnceUsers(1))).protocols(httpProtocol)
}
