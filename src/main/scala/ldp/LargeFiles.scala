package ldp

import scala.concurrent.duration._
import io.gatling.commons.validation._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import io.gatling.http.request.ExtraInfo
import scala.util.Random
import java.net.URLEncoder
import java.io.InputStream
import ciber.CiberQueryBuilder

class LargeFiles extends Simulation {

  val BASE_URL = System.getenv("LDP_URL")
  val headers_turtle = Map("Accept" -> "text/turtle", "Content-Type" -> "text/turtle")

  val httpProtocol = http.baseURL(BASE_URL)
    .extraInfoExtractor { extraInfo => List(getExtraInfo(extraInfo)) }

  private def getExtraInfo(extraInfo: ExtraInfo): String = {
    var extras = List( extraInfo.request.getMethod, extraInfo.request.getUrl )
    if( extraInfo.session.contains("PATH") ) {
      extras = extras :+ extraInfo.session.get("PATH").as[String]
    } else {
      extras = extras :+ ""
    }
    extras :+  extraInfo.response.statusCode
    extras.mkString("|")
  }

  // Data: Unlimited consistent random slice as URLs, files at least 1GB
  val seed = new java.lang.Float(.19855721)
  val cqbiter = new CiberQueryBuilder().randomSeed(seed).limit(2000).minBytes(1e9.toInt).iterator()
  val feeder = Iterator.continually({
    val path = cqbiter.next
    val title = path.substring(path.lastIndexOf('/')+1, path.length())
    Map("INPUTSTREAM" -> new java.io.FileInputStream(path), "PATH" -> path, "TITLE" -> title)
    })

  val CONTAINER_NAME = "largefiles"


  // Create a Collection container
  val scnCollection = scenario("collection")
    .exec(http("post LDP RDF Container /"+CONTAINER_NAME)
        .post(BASE_URL)
        .headers(headers_turtle)
        .header("Slug", CONTAINER_NAME)
        .header("Link", "<http://www.w3.org/ns/ldp#BasicContainer>; rel=\"type\"")
        .body(StringBody(
        """
            @prefix ldp: <http://www.w3.org/ns/ldp#> .
            @prefix dcterms: <http://purl.org/dc/terms/> .
            <> a ldp:Container, ldp:BasicContainer ;
               dcterms:title "Stress Test Collection" ;
               dcterms:description "Site of collection peformance testing" .
        """))
        .check(status.in(201, 409)))

  // Ingest all files as LDPNR contained by an LDPR
  val scnIngest = scenario("ingest")
    .feed(feeder)
    .exec( // create LDP-RS Container Object
        http("post LDP RDF Container")
        .post(BASE_URL+"/"+CONTAINER_NAME)
        .headers(headers_turtle)
        .header("Link", "<http://www.w3.org/ns/ldp#BasicContainer>; rel=\"type\"")
        .body(StringBody(
         """
            @prefix ldp: <http://www.w3.org/ns/ldp#> .
            @prefix dcterms: <http://purl.org/dc/terms/> .
            <> a ldp:Container, ldp:BasicContainer ;
               dcterms:title "${TITLE}" ;
               dcterms:source "${PATH}" .
        """))
        .check(status.in(201, 200), header("Location").saveAs("Location"))
    ).exec( // create LDP-NR
          http("put LDP non-RDF")
          .post("${Location}")
          .header("Content-Type", "application/octet-stream")
          .header("Slug", "BINARY")
          .header("Link", "<http://www.w3.org/ns/ldp#Resource>; rel=\"type\"")
          .body(RawFileBody("${PATH}"))
          .check(status.in(201, 200))
    )

  setUp(
    scnCollection.inject(
        atOnceUsers(1)),
    scnIngest.inject(
        nothingFor(10 seconds),
        rampUsers(2000) over(200 seconds)
    )).protocols(httpProtocol)
}
