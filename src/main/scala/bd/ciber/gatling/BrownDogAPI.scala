package bd.ciber.gatling

import io.gatling.commons.validation._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.ExtraInfo
import java.net.URLEncoder
import io.gatling.core.action.builder.AsLongAsLoopType

object BrownDogAPI {
  
  // Session keys
  val BD_URL = "BD_URL"
  val BD_API_KEY = "BD_API_KEY"
  val USER_NAME = "USER_NAME"
  val USER_PASSWORD = "USER_PASSWORD"
  val TOKEN_VALUE = "TOKEN_VALUE"
  
  val FILE_PATH = "FILE_PATH"
  val FILE_URL = "FILE_URL"
  val FILE_URL_ENCODED = "FILE_URL_ENCODED"
  val FILE_ID = "FILE_ID"
  val FILE_STATUS = "FILE_STATUS"
  val FILE_EXTENSION = "FILE_EXTENSION"
  val OUTPUT_FILE_EXTENSION = "OUTPUT_FILE_EXTENSION"
  val INPUT_FILE_EXTENSION = "INPUT_FILE_EXTENSION"
  val OUTPUT_FILE_URL = "OUTPUT_FILE_URL"
  val CONVERSION_OUTPUTS = "CONVERSION_OUTPUTS"
  val CONVERSION_INPUTS = "CONVERSION_INPUTS"
  val BODY_STRING = "BODY_STRING"
  
  val bdUrl = System.getProperty("bdUrl");
  val bdAPIKey = System.getProperty("bdAPIKey");
  val bdUsername = System.getProperty("bdUsername");
  val bdPassword = System.getProperty("bdPassword");
  
  val httpProtocol = http.baseURL(bdUrl)
    .extraInfoExtractor { extraInfo => List(getExtraInfo(extraInfo)) }
  
  private def getExtraInfo(extraInfo: ExtraInfo): String = {
    var extras = List( extraInfo.request.getMethod, extraInfo.request.getUrl )
    if( extraInfo.request.getFile != null ) {
      extras = extras :+ extraInfo.request.getFile.getPath
    }
    if( extraInfo.session.contains(FILE_URL) ) {
      extras = extras :+ extraInfo.session.get(FILE_URL).as[String]
    }
    extras.mkString("\t")
  }
  
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
  
  val scnClearTokenCache = scenario("clearCache")
    .exec( session => {
      tokenCache.clear()
      session.set(BD_URL, bdUrl)
        .set(BD_API_KEY, bdAPIKey)
        .set(USER_NAME, bdUsername)
        .set(USER_PASSWORD, bdPassword)
    })
    
  def initActions = exec( session => {
      session.set(USER_NAME, bdUsername)
    })
    
  def scnLogin = scenario("login")
    .exec( session => {
      tokenCache.clear()
      session.set(BD_API_KEY, bdAPIKey)
        .set(USER_NAME, bdUsername)
        .set(USER_PASSWORD, bdPassword)
    }).exec(loginWithTokenCaching)

  def loginWithTokenCaching =
    doIf( session => tokenCache.lock( session( USER_NAME ).as[String], session.userId ) ) {
      // code that does the login and stores the token into TOKEN_VALUE
      exec(http("getKey")
          .post("/keys/")
          .basicAuth("${USER_NAME}", "${USER_PASSWORD}")
          .headers(headers_accept_json)
          .check(jsonPath("$.api-key").ofType[String].saveAs(BD_API_KEY))
      ).exitHereIfFailed
      exec(http("getToken")
          .post("/keys/${BD_API_KEY}/tokens")
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
    .exec( session => session.set( TOKEN_VALUE, tokenCache.get( session(USER_NAME).as[String] ) ) )
    
  def getConversionInputs = 
    exec(http("getConversionInputs")
        .get("/conversions/inputs/${OUTPUT_FILE_EXTENSION}")
        .headers(headers_accept_text)
        .header("Authorization", { session => tokenCache.get(session(USER_NAME).as[String]) } )
        .check(bodyString.transform( string => string.trim().split('\n') ).saveAs(CONVERSION_INPUTS))
    ).exitHereIfFailed
    
  def getConversionOutputs = 
    exec(http("getConversionOutputs")
        .get("/conversions/outputs/${INPUT_FILE_EXTENSION}")
        .headers(headers_accept_text)
        .header("Authorization", { session => tokenCache.get(session(USER_NAME).as[String]) } )
        .check(bodyString.transform( string => string.trim().split('\n') ).saveAs(CONVERSION_OUTPUTS))
    ).exitHereIfFailed
    
  def assertConvertable = scenario("assertConvertable")
    .exec(
      http("getInputsForConversion")
        .get("/conversions/inputs/${OUTPUT_FILE_EXTENSION}/")
        .headers(headers_accept_text)
        .header("Authorization", { session => tokenCache.get(session(USER_NAME).as[String]) } )
        .check(substring("${INPUT_FILE_EXTENSION}").count.not(0)))

  def convertByFileURL = scenario("convertByReferenceURL")
    .exec( session => {
      val file_url = session(FILE_URL).as[String]
      val file_url_encoded = URLEncoder.encode(file_url, "utf-8")
      session.set( FILE_URL_ENCODED, file_url_encoded )
    })
    .exec(
      http("getConvertByUrl")
        .get("/conversions/${OUTPUT_FILE_EXTENSION}/${FILE_URL_ENCODED}")
        .headers(headers_accept_text)
        .header("Authorization", { session => tokenCache.get(session(USER_NAME).as[String]) } )
        .check(bodyString.transform( string => string.trim() ).saveAs(OUTPUT_FILE_URL)))
  
  def convertByFilePath = scenario("convertByFilePath")
    .exec(
      http("convertByFilePath")
        .post("/conversions/${OUTPUT_FILE_EXTENSION}")
        .headers(headers_accept_text)
        .header("Authorization", { session => tokenCache.get(session(USER_NAME).as[String]) } )
        .formUpload("file", "${FILE_PATH}")
        .check(bodyString.transform( string => string.trim() ).saveAs(OUTPUT_FILE_URL)))
        
  def pollForDownload = 
    tryMax(10) {
      pause(30)
      .exec(
        http("download")
          .get("${OUTPUT_FILE_URL}")
          .header("Authorization", { session => tokenCache.get(session(USER_NAME).as[String]) } )
          .check(status.is(200))
      )
    }
        
  def extractByFileURL = 
    exec(http("postUrl")
        .post("/extractions/url")
        .headers(headers_accept_json)
        .header("Authorization", { session => tokenCache.get(session(USER_NAME).as[String]) } )
        .body(StringBody("{ \"fileurl\": \"${FILE_URL}\" }"))
        .check(status.is(200))
        .check(jsonPath("$.id").ofType[String].saveAs(FILE_ID))
    )
    .exitHereIfFailed
    .exec( session => {
      val resp = session(FILE_ID).as[String]
      session
    })
    .exec(session => { session.set(FILE_STATUS, "") })
    .asLongAs( session => { session(FILE_STATUS).as[String].toLowerCase() != "done" }) (
      pause(30)
      .exec(http("pollUrl")
        .get("/extractions/${FILE_ID}/status")
        .headers(headers_accept_json)
        .header("Authorization", { session => tokenCache.get(session(USER_NAME).as[String]) } )
        .check(jsonPath("$.Status").ofType[String].transformOption(statusTransformOption)
            .saveAs(FILE_STATUS))
      ).exitHereIfFailed
    )
    
}