package ldp

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import umd.ciber.ciber_sampling.CiberQueryBuilder

import scala.concurrent.duration._

class StressTestIngest extends Simulation {

  val STRESS_DATA = "/tmp/stress-test/stress"

  val BASE_URL = System.getenv("LDP_URL")
  val headers_turtle = Map("Content-Type" -> "text/turtle")
  val httpProtocol = http.baseUrl(BASE_URL)

  // Data: Unlimited newly random slice as URLs, files less than 20GB
  val seed = new java.lang.Float(.19855721)
  val cqbiter = new CiberQueryBuilder().randomSeed(seed).limit(20000).minBytes(100).maxBytes(20e6.toInt).iterator()
  val feeder = Iterator.continually({
    val path = cqbiter.next
    val title = path.substring(path.lastIndexOf('/')+1, path.length())
    Map("INPUTSTREAM" -> new java.io.FileInputStream(path), "PATH" -> path, "TITLE" -> title)
  })


  // Ingest all files as LDPNR contained by an LDPR
  val scnIngest = scenario("ingest")
    .feed(feeder)
    .exec( // create LDP-RS Container Object
        http("post LDP RDF Container")
        .post(BASE_URL)
        .headers(headers_turtle)
        .header("Link", "<http://www.w3.org/ns/ldp#BasicContainer>; rel=\"type\"")
        .body(StringBody(
         """
            @prefix dcterms: <http://purl.org/dc/terms/> .
            <> dcterms:title "${TITLE}" ;
               dcterms:source "${PATH}" .
        """))
        .check(status.in(201, 200), header("Location").saveAs("Location"))
    ).exec( // create LDP-NR, removed Slug header and Link to ldp:Resource
          http("post LDP non-RDF binary")
          .post("${Location}")
          .header("Content-type", "application/octet-stream")
          .body(RawFileBody("${PATH}"))
          .check(status.in(201, 200))
    )

  setUp(
    scnIngest.inject(
      nothingFor(10 seconds),
      rampUsers(200) during(50 seconds)
    )).protocols(httpProtocol)
}
