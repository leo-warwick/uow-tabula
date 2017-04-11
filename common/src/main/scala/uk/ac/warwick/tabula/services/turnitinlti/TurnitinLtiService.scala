package uk.ac.warwick.tabula.services.turnitinlti

import java.io.IOException
import javax.crypto.spec.SecretKeySpec
import javax.crypto.{Mac, SecretKey}

import dispatch.classic.Request.toRequestVerbs
import dispatch.classic._
import dispatch.classic.thread.ThreadSafeHttpClient
import net.oauth.OAuthMessage
import net.oauth.signature.OAuthSignatureMethod
import org.apache.commons.io.FilenameUtils._
import org.apache.http.client.params.{ClientPNames, CookiePolicy}
import org.apache.http.impl.client.DefaultRedirectStrategy
import org.apache.http.params.HttpConnectionParams
import org.apache.http.protocol.HttpContext
import org.apache.http.{HttpRequest, HttpResponse, HttpStatus}
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.{DisposableBean, InitializingBean}
import org.springframework.stereotype.Service
import org.xml.sax.SAXParseException
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.api.web.Routes
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.AutowiringOriginalityReportServiceComponent
import uk.ac.warwick.tabula.{CurrentUser, DateFormats}
import uk.ac.warwick.util.core.StringUtils
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._

object TurnitinLtiService {

	val AssignmentPrefix = "Assignment-"

	val turnitinAssignmentNameMaxCharacters = 99
	val turnitinClassTitleMaxCharacters = 99

	val SubmitAssignmentWaitInSeconds = 20
	val SubmitAssignmentMaxRetries = 50
	val SubmitAttachmentWaitInSeconds = 2
	val SubmitAttachmentMaxRetries = 20
	val ReportRequestWaitInSeconds = 20
	val ReportRequestMaxRetries = 50
	val LongAwaitedReportWaitInSeconds: Int = 60 * 60

	/**
	 * Quoted supported types are...
	 * "MS Word, Acrobat PDF, Postscript, Text, HTML, WordPerfect (WPD) and Rich Text Format".
	 *
	 * https://guides.turnitin.com/01_Manuals_and_Guides/Student/Student_User_Manual/09_Submitting_a_Paper#File_Types_and_Size
	 */
	val validExtensions = Seq("doc", "docx", "odt", "wpd", "ps", "eps", "htm", "html", "hwp", "rtf", "txt", "pdf", "pptx", "ppt", "ppsx", "pps", "xls", "xlsx")
	val maxFileSizeInMegabytes = 20
	val maxFileSize: Int = maxFileSizeInMegabytes * 1024 * 1024  // 20M

	def validFileType(file: FileAttachment): Boolean =
		validExtensions contains getExtension(file.name).toLowerCase

	def validFileSize(file: FileAttachment): Boolean =
		file.actualDataLength < maxFileSize

	def validFile(file: FileAttachment): Boolean = validFileType(file) && validFileSize(file)

	/**
	 * ID that we should store classes under. They are per-module so we base it on the module code.
	 * This ID is stored within TurnitinLti and requests for the same ID should return the same class.
	 */
	def classIdFor(assignment: Assignment, prefix: String): ClassId = if (assignment.turnitinLtiClassWithAcademicYear) {
		ClassId(s"$prefix-${assignment.module.code}-${assignment.academicYear.startYear.toString}")
	} else {
		ClassId(s"$prefix-${assignment.module.code}")
	}

	def assignmentIdFor(assignment: Assignment) = AssignmentId(s"$AssignmentPrefix${assignment.id}")

	def classNameFor(assignment: Assignment): ClassName = {
		val module = assignment.module
		if (assignment.turnitinLtiClassWithAcademicYear) {
			ClassName(StringUtils.safeSubstring(s"${module.code.toUpperCase} (${assignment.academicYear.toString}) - ${module.name}", 0, turnitinClassTitleMaxCharacters))
		} else {
			ClassName(StringUtils.safeSubstring(s"${module.code.toUpperCase} - ${module.name}", 0, turnitinClassTitleMaxCharacters))
		}
	}

	def assignmentNameFor(assignment: Assignment): AssignmentName = {
		AssignmentName(s"${assignment.id} (${assignment.academicYear.toString}) ${assignment.name}")
	}

	def assignmentEndDate(assignment: Assignment): DateTime = {
		Seq(
			Seq(assignment.closeDate),
			assignment.extensions.asScala.flatMap(_.expiryDate),
			Seq(DateTime.now)
		).flatten.max.plusMonths(1)
	}
}

/**
 * Service for accessing the Turnitin LTI plagiarism API.
 */
@Service("turnitinLTIService")
class TurnitinLtiService extends Logging with DisposableBean with InitializingBean
	with AutowiringOriginalityReportServiceComponent {

	/** Turnitin LTI account id */
	@Value("${TurnitinLti.aid}") var turnitinAccountId: String = _
	/** Shared key as set up on the University of Warwick account's LTI settings */
	@Value("${TurnitinLti.key}") var sharedSecretKey: String = _

	@Value("${TurnitinLti.submitassignment.url}") var apiSubmitAssignment: String = _
	@Value("${TurnitinLti.submitpaper.url}") var apiSubmitPaperEndpoint: String = _
	@Value("${TurnitinLti.listendpoints.url}") var apiListEndpoints: String = _
	@Value("${TurnitinLti.submissiondetails.url}") var apiSubmissionDetails: String = _
	@Value("${TurnitinLti.reportlaunch.url}") var apiReportLaunch: String = _

	@Value("${turnitin.class.prefix}") var classPrefix: String =_

	@Value("${toplevel.url}") var topLevelUrl: String = _

	val userAgent = "Tabula, Coursework submission app, University of Warwick, coursework@warwick.ac.uk"

	val isoFormatter = DateFormats.IsoDateTime

	val http: Http = new Http with thread.Safety {
		override def make_client = new ThreadSafeHttpClient(new Http.CurrentCredentials(None), maxConnections, maxConnectionsPerRoute) {
			HttpConnectionParams.setConnectionTimeout(getParams, 120000)
			HttpConnectionParams.setSoTimeout(getParams, 120000)
			setRedirectStrategy(new DefaultRedirectStrategy {
				override def isRedirected(req: HttpRequest, res: HttpResponse, ctx: HttpContext) = false
			})
			getParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
		}
	}

	override def destroy() {
		http.shutdown()
	}

	override def afterPropertiesSet() {}

	def submitAssignment(assignment: Assignment, user: CurrentUser): TurnitinLtiResponse = {
		// 
		// Don't allow an end date more than 1 year away, regardless of assignment properties
		val customDueDate = Seq(TurnitinLtiService.assignmentEndDate(assignment), DateTime.now.plusYears(1)).min
		doRequest(
			apiSubmitAssignment,
			Map(
				"roles" -> "Instructor",
				"resource_link_id" -> TurnitinLtiService.assignmentIdFor(assignment).value,
				"resource_link_title" -> StringUtils.safeSubstring(TurnitinLtiService.assignmentNameFor(assignment).value, 0, TurnitinLtiService.turnitinAssignmentNameMaxCharacters),
				"resource_link_description" -> TurnitinLtiService.assignmentNameFor(assignment).value,
				"context_id" -> TurnitinLtiService.classIdFor(assignment, classPrefix).value,
				"context_title" -> TurnitinLtiService.classNameFor(assignment).value,
				"custom_duedate" -> isoFormatter.print(customDueDate),
				"custom_late_accept_flag" -> "1",
				"ext_resource_tool_placement_url" -> s"$topLevelUrl${Routes.turnitin.submitAssignmentCallback(assignment)}"
			) ++ userParams(user.userId, user.email, user.firstName, user.lastName),
			Some(HttpStatus.SC_OK)
		) { request => request >- { html => TurnitinLtiResponse.fromHtml(success = true, html = html) }}
	}

	/**
	 * Expected response xml for a successful submission
			<response>
				<status>fullsuccess</status>
				<submission_data_extract>
						Some text
			</submission_data_extract>
				<lis_result_sourcedid>[Turnitin Submission ID]</lis_result_sourcedid>
				<message>Your file has been saved successfully.</message>
			</response>
	 */
	def submitPaper(
		assignment: Assignment,	paperUrl: String, userId: String, userEmail: String, attachment: FileAttachment, userFirstName: String, userLastName: String
	 ): TurnitinLtiResponse = doRequest(

		s"$apiSubmitPaperEndpoint/${assignment.turnitinId}",
		Map(
			"resource_link_id" -> TurnitinLtiService.assignmentIdFor(assignment).value,
			"context_id" -> TurnitinLtiService.classIdFor(assignment, classPrefix).value,
			"context_title" -> TurnitinLtiService.classNameFor(assignment).value,
			"custom_xmlresponse" -> "1",
			// or Instructor, but must supply an author user id, whatever the parameter for that is!!!
			"roles" -> "Learner",
			// I hoped this would be the callback Turnitin uses when a paper has been processed - apparently not
			// "ext_outcomes_tool_placement_url" ->  s"$topLevelUrl/api/tunitin-outcomes",
			"custom_submission_url" -> paperUrl,
			"custom_submission_title" -> attachment.id,
			"custom_submission_filename" -> attachment.name

		)
			++ userParams(userId, userEmail, userFirstName, userLastName) ) {
		request =>
			request >:+ {
				(headers, request) =>
					request <>  {
						(node) => {
							val response = TurnitinLtiResponse.fromXml(node)

							response
						}
					}
			}

	}

	def getSubmissionDetails(turnitinSubmissionId: String, user: CurrentUser): TurnitinLtiResponse = doRequest(
		s"$apiSubmissionDetails/$turnitinSubmissionId", Map()) {
		request =>
			request >:+ {
				(headers, request) =>
					request >- {
						(json) => {
							TurnitinLtiResponse.fromJson(json)
						}
					}
			}
	}

	def getOriginalityReportEndpoint(attachment: FileAttachment) = s"$apiReportLaunch/${attachment.originalityReport.turnitinId}"

	def getOriginalityReportParams(endpoint: String, assignment: Assignment, attachment: FileAttachment, userId: String, email: String, firstName: String, lastName: String):Map[String, String] = {
		getSignedParams(
			Map(
			"resource_link_id" -> TurnitinLtiService.assignmentIdFor(assignment).value,
			"roles" -> "Instructor",
			"context_id" -> TurnitinLtiService.classIdFor(assignment, classPrefix).value,
			"context_title" -> TurnitinLtiService.classNameFor(assignment).value
		) ++ userParams(userId, email, firstName, lastName), getOriginalityReportEndpoint(attachment))
	}

	def listEndpoints(turnitinAssignmentId: String, user: CurrentUser): TurnitinLtiResponse = doRequest(
		s"$apiListEndpoints/$turnitinAssignmentId", Map()) {
		request =>
			request >:+ {
				(headers, request) =>
					request >- {
						(json) => {
							TurnitinLtiResponse.fromJson(json)
						}
					}
			}
	}

	def ltiConformanceTestParams(
		endpoint: String,
		secret: String,
		key: String,
		givenName: String,
		familyName: String,
		email: String,
		role: String,
		mentee: String,
		customParams: String,
		tool_consumer_info_version: String,
		assignment: Assignment,
		user: CurrentUser ): Map[String, String] = {

		val signedParams = getSignedParams(
			Map(
				"lis_result_sourcedid" -> assignment.id,
				"lis_outcome_service_url" -> s"$topLevelUrl/api/v1/turnitin/turnitin-lti-outcomes/assignment/${assignment.id}",
				"resource_link_id" -> assignment.id,
				"roles" -> role,
				"role_scope_mentor" -> mentee,
				"context_title" -> assignment.module.name,
				"context_id" -> assignment.module.code,
				"context_label" -> "A label for this context",
				"resource_link_title" -> assignment.name,
				"resource_link_description" -> "A description for this resource link",
				"tool_consumer_info_version" -> tool_consumer_info_version,

				// custom - custom params should be prefixed with _custom
				"custom_debug" -> "true"

			)  ++ userParams(null, email, givenName, familyName) ++ customParamsForTesting(customParams, email) filter(p => p._2.nonEmpty),
			endpoint, Some(secret), Some(key)
		)

		logger.debug("doRequest: " + signedParams)

		signedParams

	}

	def customParamsForTesting(customParams: String, email: String): Map[String, String] = {
		/**
		 * The LTI 1 specification forces all characters in key names to lower case and
		 * maps anything that is not a number or letter to an underscore.
		 * In LTI 2 it is best practice to send the parameter under both names, if different.
		 */

		val paramsList = customParams.split("\n").toList

		val map =	paramsList.map (
				p => (s"custom_${p.substring(0, p.indexOf("=")).replaceAll("[^a-zA-Z0-9]", "_").toLowerCase}"
					-> p.substring(p.indexOf("=")+1).replace("$User.id", email).replace("$User.username", email).replace("$ToolConsumerProfile.url", topLevelUrl)
					.filter(_ >= ' '))) // strip out carriage returns etc.
				.toMap
		map
}

	def userParams(userId: String, email: String, firstName: String, lastName: String): Map[String, String] = {
		Map(
			// according to the spec, need lis_person_name_full or both lis_person_name_family and lis_person_name_given.
			"user_id" -> email.maybeText.getOrElse(s"$userId@tabula.warwick.ac.uk"),
			"lis_person_contact_email_primary" -> email.maybeText.getOrElse("tabula@warwick.ac.uk"),
			"lis_person_name_given" -> firstName,
			"lis_person_name_family" -> lastName)
	}

	def commonParameters = Map(
		"lti_message_type" -> "basic-lti-launch-request",
		"lti_version" -> "LTI-1p0",
		"launch_presentation_locale" -> "en-GB",
		"tool_consumer_info_product_family_code" -> "Tabula"
	)

	def doRequestForLtiTesting(
		endpoint: String, secret: String, key: String, params: Map[String, String], expectedStatusCode: Option[Int] = None
	) (transform: Request => Handler[TurnitinLtiResponse]): TurnitinLtiResponse = {

		val signedParams = getSignedParams(params, endpoint, Some(secret), Some(key))

		logger.debug("doRequest: " + signedParams)

		val req = (url(endpoint) <:< Map()).POST << signedParams

		try {
			if (expectedStatusCode.isDefined){
				Try(http.when(_==expectedStatusCode.get)(transform(req))) match {
					case Success(response) => response
					case Failure(StatusCode(code, contents)) =>
						logger.warn(s"Not expected http status code - $code")
						new TurnitinLtiResponse(false, statusMessage = Some("Unexpected HTTP status code"), responseCode = Some(code))
					case _ =>
						new TurnitinLtiResponse(false, statusMessage = Some("Unexpected HTTP status code"))
				}
			} else http.x(transform(req))
		} catch {
				case e: IOException =>
					logger.error("Exception contacting provider", e)
					new TurnitinLtiResponse(false, statusMessage = Some(e.getMessage))
				case e: SAXParseException =>
					logger.error("Unexpected response from provider", e)
					new TurnitinLtiResponse(false, statusMessage = Some(e.getMessage))
		}
	}

	def doRequest(
		endpoint: String, params: Map[String, String], expectedStatusCode: Option[Int] = None
	) (transform: Request => Handler[TurnitinLtiResponse]): TurnitinLtiResponse = {

		val signedParams = getSignedParams(params, endpoint)

		val req = (url(endpoint) <:< Map()).POST << signedParams

		logger.debug("doRequest: " + signedParams)
		try {
			if (expectedStatusCode.isDefined){
				Try(http.when(_==expectedStatusCode.get)(transform(req))) match {
					case Success(response) => response
					case Failure(StatusCode(code, contents)) =>
						logger.warn(s"Not expected http status code - $code")
						new TurnitinLtiResponse(false, statusMessage = Some("Unexpected HTTP status code"), responseCode = Some(code))
					case _ =>
						new TurnitinLtiResponse(false, statusMessage = Some("Unexpected HTTP status code"))
				}
			} else http.x(transform(req))
		} catch {
				case e: IOException =>
					logger.error("Exception contacting Turnitin", e)
					new TurnitinLtiResponse(false, statusMessage = Some(e.getMessage))
				case e: java.lang.Exception =>
					logger.error("Some other exception", e)
					new TurnitinLtiResponse(false, statusMessage = Some(e.getMessage))
		}
	}

	def getSignedParams(params: Map[String, String], endpoint: String, secret: Option[String] = None, key: Option[String] = None): Map[String, String] = {
		val clientSharedSecret = secret.orElse(Some(sharedSecretKey))

		val oauthParams = Map(
			"oauth_consumer_key" -> key.getOrElse(turnitinAccountId),
			"oauth_nonce" -> System.nanoTime().toString,
			"oauth_timestamp" -> (System.currentTimeMillis() / 1000).toString,
			"oauth_signature_method" -> "HMAC-SHA1",
			"oauth_callback" -> "about:blank",
			"oauth_version" -> "1.0"
		)

		val allParams = commonParameters ++ params ++ oauthParams

		val signatureBaseString = TurnitinLtiOAuthSupport.getSignatureBaseString(endpoint, "POST", allParams)
		val signature = TurnitinLtiOAuthSupport.sign(signatureBaseString, clientSharedSecret)

		allParams + ("oauth_signature" -> signature)
	}
}

trait TurnitinLtiServiceComponent {
	def turnitinLtiService: TurnitinLtiService
}

trait AutowiringTurnitinLtiServiceComponent extends TurnitinLtiServiceComponent {
	var turnitinLtiService: TurnitinLtiService = Wire[TurnitinLtiService]
}

case class ClassName(value: String)

case class ClassId(value: String)

case class AssignmentName(value: String)

case class AssignmentId(value: String)

object TurnitinLtiOAuthSupport {

	def sign(baseString: String, clientSharedSecret: Option[String] = None, tokenSharedSecret: Option[String] = None): String = {
		def getBytesUtf8 = org.apache.commons.codec.binary.StringUtils.getBytesUtf8 _
		def encodeBase64String = org.apache.commons.codec.binary.Base64.encodeBase64String _

		// compute key
		val keyBuf = new StringBuilder

		clientSharedSecret.foreach(keyBuf.append)
		keyBuf.append('&')
		tokenSharedSecret.foreach(keyBuf.append)

		val key = keyBuf.toString

		// sign
		val secretKey: SecretKey = new SecretKeySpec(getBytesUtf8(key), "HmacSHA1")
		val mac: Mac = Mac.getInstance("HmacSHA1")
		mac.init(secretKey)

		encodeBase64String(mac.doFinal(getBytesUtf8(baseString)))
	}

	def getSignatureBaseString(requestUrl: String, httpMethod: String, baseParameters: Map[String, String]): String = {
		val message = new OAuthMessage(httpMethod, requestUrl, JMap(baseParameters.toSeq: _*).entrySet())

		OAuthSignatureMethod.getBaseString(message)
	}

}