package uk.ac.warwick.tabula.services.turnitin

import uk.ac.warwick.tabula.services.turnitin.TurnitinMethods._
import uk.ac.warwick.util.web.{Uri, UriBuilder}

abstract class Response(val message: String) {
	def successful: Boolean
}
abstract class SuccessResponse(message: String) extends Response(message) { def successful = true}
abstract class FailureResponse(message: String) extends Response(message) { def successful = false }
case class Created(id: String) extends SuccessResponse("Created")
case class Deleted() extends SuccessResponse("Deleted")
case class Done() extends SuccessResponse("Done")
case class GotSubmissions(list: Seq[TurnitinSubmissionInfo]) extends SuccessResponse("Got submissions")
case class AlreadyExists() extends FailureResponse("Object already exists")
case class DuplicateClass() extends FailureResponse("Class already exists")
case class ClassNotFound() extends FailureResponse("Class not found")
case class ClassDeleted() extends FailureResponse("Class deleted")
case class AssignmentNotFound() extends FailureResponse("Assignment not found")
case class SubmissionNotFound() extends FailureResponse("Submission not found")
case class Failed(code: Int, reason: String) extends FailureResponse(reason)

/**
 * Contains the main API methods that can be called from Session.
 *
 * About IDs
 * ---------
 *
 * In Turnitin, classes and assignments can have two kinds of ID. They always have an internally
 * generated ID which is returned in a response, but it's also possible to pass your own external ID
 * when creating the object. The API will never expose this back to you, but it is stored with the object
 * so you can later refer to it by that ID.
 *
 * I think when we use a ClassId or AssignmentId, we should use our own values.
 *
 * About creating objects
 * ----------------------
 *
 * For many of the APIs, if you make a request as a user who doesn't already exist, they will be created
 * (though the login step will do this anyway). If you create an assignment referring to a class ID that
 * doesn't exist, the class will be created implicitly. the same goes for the paper submission, which can
 * implicitly create both a class and an assignment for you. So you can avoid a few API calls by skipping
 * the step of making those things explicitly.
 *
 */
trait TurnitinMethods { self: Session =>

	// Create a session in turnitin and return a session ID.
	// This is only called by Turnitin service, and once you have a Session
	// you are already logged in and can just use the other methods.
	def login(): Response = {
		if (sessionId != null) {
			throw new UnsupportedOperationException("This should only be called by Turnitin service on an anonymous Session without an ID")
		}

		val response = doRequestAdvanced(LoginFunction, None,
			"fcmd" -> "2",
			"utp" -> "2",
			"uem" -> userEmail,
			"ufn" -> userFirstName,
			"uln" -> userLastName,
			"create_session" -> "1") { request =>
				request >:+ { (headers, request) =>
					logger.debug("Login request")
					logger.debug(headers.toString())
					request <> { (node) => TurnitinResponse.fromXml(node) }
				}
			}
		if (logger.isDebugEnabled) {
			logger.debug("Login %s : %s" format (userEmail, response))
		}
		if (response.success) Created(response.sessionId.getOrElse(""))
		else Failed(response.code, response.message)
	}

	/**
		* Ensures that the session has a userid for the user who is logged in.
		*/
	def acquireUserId() {
		if (userId == "") {
			userId = getUserId().getOrElse(throw new IllegalStateException("Failed to get userid"))
		}
	}
	/**
		* Gets/creates this user and returns the userid
		* if successful.
		*/
	def getUserId(): Option[String] = {
		doRequest(LoginFunction,
			"fcmd" -> "2",
			"utp" -> "2",
			"uem" -> userEmail,
			"ufn" -> userFirstName,
			"uln" -> userLastName)
			.userId
	}

	/**
		* List the submissions made to this assignment in this class.
		*
		* This command <em>may</em> throw ClassNotFound or AssignmentNotFound in some circumstances,
		* but the intended behaviour is that it will create the class or assignment implicitly.
		*/
	def listSubmissions(classId: ClassId, className: ClassName, assignmentId: AssignmentId, assignmentName: AssignmentName): Response = {
		val response = doRequest(ListSubmissionsFunction,
			"cid" -> classId.value,
			"ctl" -> className.value,
			"assignid" -> assignmentId.value,
			"assign" -> assignmentName.value,
			"tem" -> userEmail,
			"fcmd" -> "2")
		if (response.success) GotSubmissions(response.submissionsList)
		else resolveError(response)
	}

	/**
		* There is an API call fid=6 to get a report link, but at the moment it doesn't
		* work, so this generates a link to the originality report viewer for a document.
		*/
	def getDocumentViewerLink(document: DocumentId): Uri = {
		UriBuilder.parse(apiEndpoint)
			.setPath("/dv")
			.setQuery("")
			.addQueryParameter("o", document.value)
			.addQueryParameter("session-id", sessionId)
			.addQueryParameter("s", "1") // s=1 -> Originality report
			.toUri
	}

	// scalastyle:off magic.number
	def resolveError(response: TurnitinResponse): Response = response.code match {
		case 419 => AlreadyExists()
		case 204 => ClassNotFound()
		case 206 => AssignmentNotFound()
		case 248 => ClassDeleted()
		case _ => Failed(response.code, response.message)
	}
	// scalastyle:on magic.number

}

object TurnitinMethods {
	// Values for the "fid" parameter of an API call
	private val LoginFunction = "1"
	private val CreateClassFunction = "2"
	private val CreateAssignmentFunction = "4"
	private val SubmitPaperFunction = "5"
	private val GenerateReportFunction = "6"
	private val DeletePaperFunction = "8"
	private val ListSubmissionsFunction = "10"
	private val LogoutFunction = "18"
	private val ListEnrollmentFunction = "19"
}