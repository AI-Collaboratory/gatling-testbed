package ldp

import scala.concurrent.duration.DurationInt
import scala.util.Random

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class BasicContainmentMementosGET extends Simulation {
  val BASE_URL = System.getenv("LDP_URL")
  val headers_turtle = Map("Content-Type" -> "text/turtle")
  val httpProtocol = http.baseUrl(BASE_URL)

  val feeder = Iterator.continually(Map("name" -> Random.alphanumeric.take(20).mkString))

  val ingestThenGetFolders = scenario("BasicContainer distinct children")
    .feed(feeder)
    .exec( // Creates a unique top-level container per user
          http("POST top-level LDP BasicContainer")
          .post("")
          .headers(headers_turtle)
          .header("Slug", "${name}")
          .header("Link", "<http://www.w3.org/ns/ldp#BasicContainer>; rel=\"type\"")
          .check()
    )
    .repeat(10, "n") ( // Adds N child containers
      exec(
        http("POST child LDP BasicContainer")
          .post("/${name}")
          .headers(headers_turtle)
          .header("Slug", "child-${n}")
          .header("Link", "<http://www.w3.org/ns/ldp#BasicContainer>; rel=\"type\"")
          .check(status.in(201, 200))
      )
      .repeat(10, "na") ( // Adds N more version/mementos of each child container
        exec(
            http("post LDP non-RDF binary")
            .put("/${name}/child-${n}")
            .headers(headers_turtle)
            .header("Link", "<http://www.w3.org/ns/ldp#BasicContainer>; rel=\"type\"")
            .check()
        )
      )
    )
    .repeat(1000, "n") (
      exec(
          http("Get top-level container, making it list DISTINCT children")
          .get("/${name}")
          .header("Accept", "text/turtle")
          .header("Prefer", "return=representation; include=\"http://www.w3.org/ns/ldp#PreferMembership http://www.w3.org/ns/ldp#PreferContainment\"")
          .check()
          )
      .pause(1)
    )

  setUp(
    ingestThenGetFolders.inject(
        atOnceUsers(10)
    )).protocols(httpProtocol)
}
