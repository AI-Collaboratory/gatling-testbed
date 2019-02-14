package ldp

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import umd.ciber.ciber_sampling.CiberQueryBuilder
import org.slf4j.LoggerFactory

class FolderIngest extends Simulation {
  val BASE_URL = System.getenv("LDP_URL")
  val headers_turtle = Map("Content-Type" -> "text/turtle")
  val httpProtocol = http.baseUrl(BASE_URL)

  // Data: Unlimited newly random slice as URLs, files less than 20GB
  val seed = new java.lang.Float(.19855721)
  val cqbiter = new CiberQueryBuilder().randomSeed(seed).limit(20000).minBytes(100).maxBytes(20e6.toInt).iterator()
  val feeder = Iterator.continually({
    val path = cqbiter.next
    val title = path.substring(path.lastIndexOf('/')+1)
    Map("INPUTSTREAM" -> new java.io.FileInputStream(path),
        "PATH" -> path,
        "FULLPATH" -> path,
        "TITLE" -> title)
    })
  
  val ingestFolders = scenario("ingest-folders")
    .feed(feeder)
    .doIf( session => { session("PATH").as[String].startsWith("/") } ) { 
      exec(session => {
        val path = session("PATH").as[String]
        session.set("PATH", path.substring(1))
        session.set("Location", BASE_URL+"/")
    })}
    .asLongAs(session => { session("PATH").as[String].contains("/") }, "depth")(
      exec( session => {
        val path = session("PATH").as[String]
        val segment = path.substring(0, path.indexOf("/"))
        val nextPath = path.substring(path.indexOf("/")+1)
        session.set("PATH", nextPath)
        session.set("segment", segment)
      })
      .exec( // create LDP-RS Container Object
        http("post LDP RDF Container")
        .post("${Location}")
        .headers(headers_turtle)
        .header("Slug", "${segment}")
        .header("Link", "<http://www.w3.org/ns/ldp#BasicContainer>; rel=\"type\"")
        .body(StringBody(
         """
            @prefix dcterms: <http://purl.org/dc/terms/> .
            <> dcterms:title "${segment}" ;
               dcterms:extent "${depth}" .
        """))
        .check(header("Location").saveAs("Location"))
      )
    )
    .exec(
        http("post LDP non-RDF binary")
          .post("${Location}")
          .header("Content-type", "application/octet-stream")
          .body(RawFileBody("${FULLPATH}"))
          .check()
    )
    
  setUp(
    ingestFolders.inject(
        nothingFor(10 seconds),
        rampUsers(200) during(200 seconds)
    )).protocols(httpProtocol)
}