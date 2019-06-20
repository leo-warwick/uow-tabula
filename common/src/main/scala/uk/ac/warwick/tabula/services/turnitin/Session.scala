package uk.ac.warwick.tabula.services.turnitin

import java.io.IOException

import org.apache.commons.codec.digest.DigestUtils
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.{HttpPost, HttpUriRequest}
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.xml.sax.SAXParseException
import uk.ac.warwick.tabula.helpers.Products._
import uk.ac.warwick.tabula.helpers.{ApacheHttpClientUtils, Logging}

/**
  * Acquired from a call to Turnitin.login(), this will call Turnitin methods as a particular
  * user.
  */
class Session(turnitin: Turnitin, val sessionId: String) extends TurnitinMethods with Logging {

  /**
    * The API calls are split out into TurnitinMethods - the body of this class mostly contains
    * the supporting methods for generating the valid signed Turnitin requests. The API methods
    * then call the doRequest() function with whatever parameters.
    */

  import TurnitinDates._

  // Some parameters are not included in the MD5 signature calculation.
  val excludeFromMd5 = Seq(
    "dtend", "create_session", "session-id", "src", "apilang",
    "exclude_biblio", "exclude_quoted", "exclude_type", "exclude_value"
  )

  // These are overriden within Turnitin.login().
  var userEmail = ""
  var userFirstName = ""
  var userLastName = ""
  var userId = ""

  private val httpClient = turnitin.httpClient

  private def diagnostic = turnitin.diagnostic

  def apiEndpoint: String = turnitin.apiEndpoint

  /**
    * All API requests call the same URL and require the same MD5
    * signature parameter.
    *
    * If you start getting an "MD5 NOT AUTHENTICATED" on an API method you've
    * changed, it's usually because it doesn't recognise one of the parameters.
    * We MD5 on all parameters but the server will only MD5 on the parameters
    * it recognises, hence the discrepency. There is no way to know which parameters
    * that Turnitin cares about. There is no list in the docs. What fun!
    */
  def doRequest(
    functionId: String, // API function ID (defined in TurnitinMethods object)
    params: (String, String)*): TurnitinResponse = {

    val request = getRequest(functionId, params: _*)

    val handler: ResponseHandler[TurnitinResponse] =
      ApacheHttpClientUtils.handler {
        case response if Option(response.getFirstHeader("location")).nonEmpty =>
          EntityUtils.consumeQuietly(response.getEntity)
          TurnitinResponse.redirect(response.getFirstHeader("location").getValue)

        case response if turnitin.diagnostic =>
          TurnitinResponse.fromDiagnostic(EntityUtils.toString(response.getEntity))

        case response =>
          // Call handleEntity to avoid AbstractResponseHandler throwing exceptions for >=300 status codes
          ApacheHttpClientUtils.xmlResponseHandler(TurnitinResponse.fromXml)
            .handleEntity(response.getEntity)
      }

    try {
      val response = httpClient.execute(request, handler)
      logger.debug("Response: " + response)
      response
    } catch {
      case e: IOException =>
        logger.error("Exception contacting Turnitin", e)
        new TurnitinResponse(code = 9000, diagnostic = Some(e.getMessage))
      case e: SAXParseException =>
        logger.error("Unexpected response from Turnitin", e)
        new TurnitinResponse(code = 9001, diagnostic = Some(e.getMessage))
    }
  }

  def getRequest(
    functionId: String, // API function ID (defined in TurnitinMethods object)
    params: (String, String)*): HttpUriRequest = {

    val fullParameters = calculateParameters(functionId, params: _*)
    val req =
      turnitin.request(HttpPost.METHOD_NAME)
        .addParameters(fullParameters.toSeq.map { case (k, v) => new BasicNameValuePair(k, v) }: _*)

    logger.debug("doRequest: " + fullParameters)
    req.build()
  }

  def calculateParameters(functionId: String, params: (String, String)*): Map[String, String] = {
    val parameters = (Map("fid" -> functionId) ++ commonParameters ++ params).filterNot(nullValue)
    parameters + md5hexparam(parameters)
  }

  /**
    * Makes a request as in doRequest, but leaves the response handling to you, via
    * the transform function.
    */
  def doRequestAdvanced(
    functionId: String, // API function ID
    pdata: Option[FileData], // optional file to put in "pdata" parameter
    params: (String, String)*) // POST parameters
    (transform: ResponseHandler[TurnitinResponse]): TurnitinResponse = {

    val parameters = Map("fid" -> functionId) ++ commonParameters ++ params
    val req =
      turnitin.request(HttpPost.METHOD_NAME)
        .addParameters((parameters + md5hexparam(parameters)).toSeq.map { case (k, v) => new BasicNameValuePair(k, v) }: _*)

    logger.debug("doRequest: " + parameters)

    try {
      httpClient.execute(req.build(), ApacheHttpClientUtils.handler {
        case response if turnitin.diagnostic =>
          TurnitinResponse.fromDiagnostic(EntityUtils.toString(response.getEntity))

        case response =>
          transform.handleResponse(response)
      })
    } catch {
      case e: IOException =>
        logger.error("Exception contacting Turnitin", e)
        new TurnitinResponse(code = 9000, diagnostic = Some(e.getMessage))
    }
  }

  /**
    * Parameters that we need in every request.
    */
  def commonParameters: Map[String, String] = Map(
    "diagnostic" -> (if (diagnostic) "1" else "0"),
    "gmtime" -> gmtTimestamp,
    "encrypt" -> "0",
    "aid" -> turnitin.aid,
    "fcmd" -> "2",
    "uid" -> userId,
    "uem" -> userEmail,
    "ufn" -> userFirstName,
    "uln" -> userLastName,
    "utp" -> "2",
    "dis" -> "1", // disable emails
    "src" -> turnitin.integrationId) ++ subAccountParameter ++ sessionIdParameter

  /** Optional sub-account ID */
  private def subAccountParameter: Map[String, String] =
    if (turnitin.said == null || turnitin.said.isEmpty)
      Map.empty
    else
      Map("said" -> turnitin.said)

  /** Optional session ID */
  private def sessionIdParameter: Map[String, String] = {
    if (sessionId == null)
      Map.empty
    else
      Map("session-id" -> sessionId)
  }

  /** The md5 signature to add to the request parameter map. */
  def md5hexparam(map: Map[String, String]): (String, String) = "md5" -> md5hex(map)

  /**
    * Sort parameters by key, concatenate all the values with
    * the shared key and MD5hex that.
    */
  def md5hex(params: Map[String, String]): String = {
    val filteredParams = params.filterKeys(!excludeFromMd5.contains(_)).toSeq
    val sortedParams = filteredParams.sortBy(toKey) // sort by key (left part of Pair)
    val sortedValues = sortedParams.map(toValue).mkString // map to value (right part of Pair)
    DigestUtils.md5Hex(sortedValues + turnitin.sharedSecretKey)
  }

}
