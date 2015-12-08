package bd.ciber.gatling

import scala.concurrent.duration._
import scala.collection.mutable.Queue

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

import org.slf4j.LoggerFactory

class ExtractCollectionSimulation extends Simulation {
  val LOG = LoggerFactory.getLogger("bd.ciber.gatling.IndigoSimulation");
  val cdmiProxyUrl = System.getProperty("cdmiProxyUrl");
  val dtsUrl = System.getProperty("dtsUrl");
  val commkey = System.getProperty("dtsCommKey");
  val startPath = System.getProperty("dtsTestPath1");
  print(dtsUrl)
  print(commkey)
  
  val httpProtocol = http.baseURL(cdmiProxyUrl).disableWarmUp
  val headers_accept_json = Map("Accept" -> "application/json", "Content-type" -> "application/json")
  val headers_any = Map(
      "X-CDMI-Specification-Version" -> "1.1",
      "Accept" -> "*/*")
  val headers_container = Map(
      "X-CDMI-Specification-Version" -> "1.1",
      "Accept" -> "application/cdmi-container")
      
  val filePaths = Queue[String]()
  val feeder = Iterator.continually( Map("path" -> ( filePaths.dequeue() )) ) 

  val scnList = scenario("indigo-list")
    .exec( session => {
      session.set("children", Seq())
    })
    .exec(http("request_container")
      .get("${path}")
      .headers(headers_container)
      .check(jsonPath("$.children[*]").findAll.exists.saveAs("children"))
    ).exitHereIfFailed
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
      println(resp+" has path: "+ session("path").as[String])
      session
    })
    .exec(session => { session.set("body", "") })
    .asLongAs( session => { session("body").as[String] == "" }) (
      exec(http("pollUrl")
        .get(dtsUrl + "/api/extractions/${id}/status?key="+commkey)
        .headers(headers_accept_json)
        .check(bodyString.exists.saveAs("body"))
      ).exec(session => {
          println(session("body").as[String])
          session
        }
      )
    )
    
        
  val scnLevelFirstCrawl = scenario("level-first-crawl")
    .exec(session => { 
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

  setUp(
      scnLevelFirstCrawl.inject(atOnceUsers(1)),
      scnPostFileToExtract.inject(nothingFor(30 seconds),atOnceUsers(10))
  ).protocols(httpProtocol)
}