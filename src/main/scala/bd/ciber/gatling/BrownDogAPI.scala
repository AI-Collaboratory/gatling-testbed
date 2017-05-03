package bd.ciber.gatling

import io.gatling.core.validation._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import java.net.URLEncoder
import io.gatling.core.action.builder.AsLongAsLoopType

object BrownDogAPI {
  final val LOG = org.slf4j.LoggerFactory.getLogger("bd.ciber.gatling.BrownDogAPI");
  
  // Session keys
  val BD_URL = "BD_URL"
  val USER_NAME = "USER_NAME"
  val USER_PASSWORD = "USER_PASSWORD"
  
  val TOKEN_VALUE = "TOKEN_VALUE"
  
  val FILE_URL = "FILE_URL"
  val FILE_URL_ENCODED = "FILE_URL_ENCODED"
  val FILE_ID = "FILE_ID"
  val FILE_STATUS = "FILE_STATUS"
  val FILE_EXTENSION = "FILE_EXTENSION"
  val OUTPUT_FILE_EXTENSION = "OUTPUT_FILE_EXTENSION"
  val OUTPUT_FILE_URL = "OUTPUT_FILE_URL"
  val DAP_OUTPUTS = "DAP_OUTPUTS"
  val BODY_STRING = "BODY_STRING"
  
  val headers_accept_json = Map("Accept" -> "application/json", "Content-type" -> "application/json")  
  val headers_accept_text = Map("Accept" -> "text/plain", "Content-type" -> "text/plain")  
  
  private val tokenCache = new LookupCache()
  
  val statusTransformOption = (input: Option[String], session: Session) => {
    val path = session(FILE_URL).as[String]
    val extension = path.substring(path.lastIndexOf(".") + 1).toLowerCase()
    input.get match {
      case "Done" => Success(Option("Done"))
      case "Processing" => Success(Option("Processing"))
      case "Required Extractor is either busy or is not currently running. Try after some time." => 
        Failure("An extractor was busy or not running for " + extension + " at " + path)
      case x => Failure("The DTS failed to extract for "+ extension + " at " + path + " with status: "+x)
    }
  }

  def loginWithTokenCaching =
    doIf( session => tokenCache.lock( session( USER_NAME ).as[String], session.userId ) ) {
      // code that does the login and stores the token into TOKEN_VALUE
      exec(http("getKey")
          .post("${BD_URL}/keys/")
          .basicAuth("${USER_NAME}", "${USER_PASSWORD}")
          .headers(headers_accept_json)
          .check(jsonPath("$.api-key").ofType[String].saveAs("API_KEY"))
      ).exitHereIfFailed
      .exec(http("getToken")
          .post("${BD_URL}/keys/${API_KEY}/tokens")
          .basicAuth("${USER_NAME}", "${USER_PASSWORD}")
          .headers(headers_accept_json)
          .check(jsonPath("$.token").ofType[String].saveAs("TOKEN_VALUE"))
      ).exitHereIfFailed      
      // save the token into the cache for next time
      .exec( session => {
        tokenCache.put(
          session( USER_NAME ).as[String],
          session( TOKEN_VALUE ).as[String],
          session.userId
        )
        session
      })
    }
    // pull the token out of the cache for use in subsequent tests
    .exec( session => session.set( TOKEN_VALUE, tokenCache.get( session( USER_NAME ).as[String] ) ) )
    
  def getConvertOutputs = 
    exec( session => {
      val path = session( FILE_URL ).as[String]
      val ext = path.substring(path.lastIndexOf('.')+1)
      session.set( FILE_EXTENSION, ext)
    })
    .exec(http("getDAPOutputs")
        .get("${BD_URL}/dap/inputs/${FILE_EXTENSION}")
        .headers(headers_accept_text)
        .header("Authorization", { session => tokenCache.get(session(USER_NAME).as[String]) } )
        .check(bodyString.transform( string => string.trim().split('\n') ).saveAs(DAP_OUTPUTS))
    ).exitHereIfFailed
    
  def convertByFileURL = scenario("convert")
    .exec( session => {
      val file_url = session(FILE_URL).as[String]
      val file_url_encoded = URLEncoder.encode(file_url, "utf-8")
      session.set( FILE_URL_ENCODED, file_url_encoded )
    })
    .exec(
      http("getConvertByUrl")
        .get("${BD_URL}/dap/convert/${OUTPUT_FILE_EXTENSION}/${FILE_URL_ENCODED}")
        .headers(headers_accept_text)
        .header("Authorization", { session => tokenCache.get(session(USER_NAME).as[String]) } )
        .check(bodyString.transform( string => string.trim() ).saveAs(OUTPUT_FILE_URL)))
  
  def pollForDownload = 
    asLongAs( session => { 
        session("counter").as[Int] < 10 && 
        (!session.contains("status") || session("status").as[String] != "done") },
        "counter", false, AsLongAsLoopType)
          {
            exitBlockOnFail(
              pause(10)
              .exec(
                http("download")
                  .get("${OUTPUT_FILE_URL}")
                  .header("Authorization", { session => tokenCache.get(session(USER_NAME).as[String]) } )
                  .check(status.is(200)))
              .exec( session => {
                session.set("status", "done")
              })
            )
          }
        
  def extractByFileURL = 
    exec(http("postUrl")
        .post("${BD_URL}/dts/api/extractions/upload_url")
        .headers(headers_accept_json)
        .header("Authorization", { session => tokenCache.get(session(USER_NAME).as[String]) } )
        .body(StringBody("{ \"fileurl\": \"${FILE_URL}\" }"))
        .check(status.is(200))
        .check(jsonPath("$.id").ofType[String].saveAs(FILE_ID))
    )
    .exitHereIfFailed
    .exec( session => {
      val resp = session(FILE_ID).as[String]
      LOG.info(resp+" has path: "+ session(FILE_URL).as[String])
      session
    })
    .exec(session => { session.set(FILE_STATUS, "") })
    .asLongAs( session => { session(FILE_STATUS).as[String].toLowerCase() != "done" }) (
      exec(http("pollUrl")
        .get("${BD_URL}/dts/api/extractions/${FILE_ID}/status")
        .headers(headers_accept_json)
        .header("Authorization", { session => tokenCache.get(session(USER_NAME).as[String]) } )
        .check(jsonPath("$.Status").ofType[String].transformOption(statusTransformOption)
            .saveAs(FILE_STATUS))
      ).exitHereIfFailed
      .exec(session => {
          LOG.info(session(FILE_STATUS).as[String])
          session
        }
      ).pause(2)
    )
    
}