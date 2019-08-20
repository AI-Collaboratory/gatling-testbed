package ldp

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import umd.ciber.ciber_sampling.CiberQueryBuilder

import scala.concurrent.duration._

class SolidStressTestIngest extends Simulation {


  val BASE_URL = System.getenv("LDP_URL")
  val SIM_USERS:  Int = Integer.valueOf(System.getenv().getOrDefault("SIM_USERS", "2000"))
  val SIM_DURATION: Int = Integer.valueOf(System.getenv().getOrDefault("SIM_DURATION", "200"))

  val headers_turtle = Map("Content-Type" -> "text/turtle")
  val httpProtocol = http.baseUrl(BASE_URL)

  // Data: Unlimited newly random slice as URLs, files less than 20GB
  val seed = new java.lang.Float(.19855721)
  val cqbiter = new CiberQueryBuilder().randomSeed(seed).limit(20000).minBytes(100).maxBytes(20e6.toInt).iterator()
  val feeder = Iterator.continually({
    val path = cqbiter.next
    val title = path.substring(path.lastIndexOf('/') + 1, path.length())
    Map("INPUTSTREAM" -> new java.io.FileInputStream(path), "PATH" -> path, "TITLE" -> title)
  })


  object Search {

    val searchContainer = exec(http("Get Container")
      .get("${Container}")
      .check(status.is(200)))

    val searchRdfResource = exec(http("Get RDF Resource")
      .get("${RDFSource}")
      .check(status.is(200)))

    val searchNonRdfResource = exec(http("Get Non RDF Resource")
      .get("${NonRDFSource}")
      .check(status.is(200)))
  }

  val CONTAINER_RDF =
    """
      @prefix dcterms: <http://purl.org/dc/terms/> .
      <> dcterms:title "${TITLE}" ;
         dcterms:source "${PATH}" .
    """

  val RESOURCE_RDF =
    """
      @prefix foaf: <http://xmlns.com/foaf/0.1/>.
      @prefix vcard: <http://www.w3.org/2006/vcard/ns#>.
      @prefix schem: <http://schema.org/>.
      @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.

      <>
          a schem:Person, vcard:Person;
          rdfs:label "Joe Bloggs"@en;
          foaf:name "Joe";
          foaf:givenName "Bloggs"@en;
          foaf:familyName "Bloggs"@en;
          foaf:birthday "01-01";
          foaf:age 43;
          foaf:gender "male"@en.
    """

  object Resource {

    val CONTAINER_KEY = "Container"
    val CONTAINER_VAR = "${Container}"
    val RDF_SOURCE_KEY = "RDFSource"
    val RDF_SOURCE_VAR = "${RDFSource}"
    val NON_RDF_SOURCE_KEY = "NonRDFSource"
    val NON_RDF_SOURCE_VAR = "${NonRDFSource}"

    val createContainer = exec(http("Create Container")
      .post(BASE_URL)
      .headers(headers_turtle)
      .header("Link", "<http://www.w3.org/ns/ldp#BasicContainer>; rel=\"type\"")
      .body(StringBody(CONTAINER_RDF))
      .check(status.in(201, 200), header("Location").saveAs(CONTAINER_KEY))
    )

    val createRdf = exec(http("Create RDF Resource")
      .post(CONTAINER_VAR)
      .headers(headers_turtle)
      .header("Link", "<http://www.w3.org/ns/ldp#RDFSource>; rel=\"type\"")
      .body(StringBody(RESOURCE_RDF))
      .check(status.in(201, 200), header("Location").saveAs(RDF_SOURCE_KEY)))
      .pause(2 seconds)

    val createNonRdfResource = exec(http("Create non-RDF binary")
      .post(CONTAINER_VAR)
      .header("Content-type", "application/octet-stream")
      .body(RawFileBody("${PATH}"))
      .check(status.in(201, 200), header("Location").saveAs(NON_RDF_SOURCE_KEY)))

    val updateRdfMultipleTimes = repeat(10, "n") {
      exec(http("Update RDF Resource")
        .put(RDF_SOURCE_VAR)
        .headers(headers_turtle)
        .header("Link", "<http://www.w3.org/ns/ldp#RDFSource>; rel=\"type\"")
        .body(StringBody(
          """
            @prefix foaf: <http://xmlns.com/foaf/0.1/>.
            @prefix vcard: <http://www.w3.org/2006/vcard/ns#>.
            @prefix schem: <http://schema.org/>.
            @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.

            <>
                a schem:Person, vcard:Person;
                rdfs:label "Joe Bloggs"@en;
                foaf:name "Joe";
                foaf:givenName "Bloggs"@en;
                foaf:familyName "Bloggs"@en;
                foaf:birthday "01-01";
                foaf:age 43;
                foaf:gender "male"@en;
                rdfs:update "${n}".
          """
        ))
        .check(status.is(204)))
        .pause(2 seconds)
    }

    val deleteContainer = exec(http("Delete Resource")
      .delete(CONTAINER_VAR)
      .check(status.is(204)))

    val deleteRdfResource = exec(http("Delete Resource")
      .delete(RDF_SOURCE_VAR)
      .check(status.is(204)))

    val deleteNonRdfResource = exec(http("Delete Resource")
      .delete(NON_RDF_SOURCE_VAR)
      .check(status.is(204)))

  }

  val createAndGetIngest = scenario("Create and Get RDF and Non RDF Resources")
    .feed(feeder)
    .exec(Resource.createContainer, Search.searchContainer,
      Resource.createRdf, Search.searchRdfResource,
      Resource.createNonRdfResource, Search.searchNonRdfResource)

  val createAndDeleteIngest = scenario("Create and Delete RDF and Non RDF Resources")
    .feed(feeder)
    .exec(Resource.createContainer,
      Resource.createRdf,
      Resource.createNonRdfResource,
      Resource.deleteNonRdfResource,
      Resource.deleteRdfResource,
      Resource.deleteContainer)

  val createAndUpdateIngest = scenario("Create and Update RDF and Non RDF Resources")
    .feed(feeder)
    .exec(Resource.createContainer,
      Resource.createRdf,
      Resource.createNonRdfResource,
      Resource.updateRdfMultipleTimes)

  private val USERS_60_PERCENT: Int = SIM_USERS - (SIM_USERS * 40) / 100
  private val USERS_20_PERCENT: Int = (SIM_USERS * 20) / 100

  setUp(
    createAndGetIngest.inject(
      nothingFor(2 seconds),
      rampUsers(USERS_60_PERCENT) during (SIM_DURATION seconds)),

    createAndUpdateIngest.inject(
      nothingFor(5 seconds),
      rampUsers(USERS_20_PERCENT) during (SIM_DURATION seconds)),

    createAndDeleteIngest.inject(
      nothingFor(10 seconds),
      rampUsers(USERS_20_PERCENT) during (SIM_DURATION seconds))
  ).protocols(httpProtocol)
}
