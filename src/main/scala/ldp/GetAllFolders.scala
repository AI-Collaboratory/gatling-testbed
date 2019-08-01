package ldp

import scala.concurrent.duration.DurationInt

import io.gatling.core.Predef.RawFileBody
import io.gatling.core.Predef.Simulation
import io.gatling.core.Predef.StringBody
import io.gatling.core.Predef.configuration
import io.gatling.core.Predef.exec
import io.gatling.core.Predef.feeder2FeederBuilder
import io.gatling.core.Predef.findCheckBuilder2CheckBuilder
import io.gatling.core.Predef.findCheckBuilder2ValidatorCheckBuilder
import io.gatling.core.Predef.nothingFor
import io.gatling.core.Predef.openInjectionProfileFactory
import io.gatling.core.Predef.rampUsersPerSec
import io.gatling.core.Predef.rawFileBodies
import io.gatling.core.Predef.scenario
import io.gatling.core.Predef.stringToExpression
import io.gatling.core.Predef.value2Success
import io.gatling.http.Predef.checkBuilder2HttpCheck
import io.gatling.http.Predef.header
import io.gatling.http.Predef.http
import io.gatling.http.Predef.httpHeaderCheckMaterializer
import io.gatling.http.Predef.httpStatusCheckMaterializer
import io.gatling.http.Predef.status
import umd.ciber.ciber_sampling.CiberQueryBuilder
import ldp.folderSeed._

class GetAllFolders extends Simulation {
  val BASE_URL = System.getenv("LDP_URL")
  val headers_turtle = Map("Content-Type" -> "text/turtle")
  val httpProtocol = http.baseUrl(BASE_URL)

  // Data: Unlimited newly random slice as URLs, files less than 20GB
  val getQueryIterator = new CiberQueryBuilder().randomSeed(folderSeed.value).limit(20000).minBytes(100).maxBytes(20e6.toInt).iterator()
  val getFeeder = Iterator.continually({
  val path = getQueryIterator.next
  val title = path.substring(path.lastIndexOf('/')+1)
  Map("INPUTSTREAM" -> new java.io.FileInputStream(path),
      "PATH" -> path,
      "FULLPATH" -> path,
      "TITLE" -> title)
  })

  val getAllFolders = scenario("ingest-folders")
    .feed(getFeeder)
    .doIf( session => { session("PATH").as[String].startsWith("/") } ) { 
      exec(session => {
        val path = session("PATH").as[String]
        session.set("PATH", path.substring(1))
          .set("Location", BASE_URL)
    })}
    .asLongAs(session => { session("PATH").as[String].contains("/") }, "depth")(
      exec( session => {
        val path = session("PATH").as[String]
        val segment = java.net.URLEncoder.encode(path.substring(0, path.indexOf("/")), "utf-8")
        val nextPath = path.substring(path.indexOf("/")+1)
        session.set("PATH", nextPath)
          .set("segment", segment)
      })
      .exec(
          http("Get container")
          .get("${Location}/${segment}")
          .check(status.in(200))
      )
      .exec( // make next Location for loop
          session => {
            val loc = session("Location").as[String]+"/"+session("segment").as[String]
            session.set("Location", loc)
          }
      )
    )
    .exec(
        http("Get binary")
          .get("${Location}")
          .header("Content-type", "application/octet-stream")
          .body(RawFileBody("${FULLPATH}"))
          .check(status.in(200))
    )
    
  setUp(
    getAllFolders.inject(
        nothingFor(5 minutes),
        rampUsersPerSec(10) to(30) during(10 minutes)
    )).protocols(httpProtocol)
}