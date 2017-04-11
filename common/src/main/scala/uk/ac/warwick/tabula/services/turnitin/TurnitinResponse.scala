package uk.ac.warwick.tabula.services.turnitin

import scala.collection.immutable.Seq
import scala.util.matching.Regex
import scala.xml.Elem

/**
 * Response from the API.
 */
case class TurnitinResponse(
	val code: Int,
	val diagnostic: Option[String] = None,
	val redirectUrl: Option[String] = None,
	val xml: Option[Elem] = None) {
	def success: Boolean = code <= 100
	def error: Boolean = !success

	lazy val assignmentId: Option[String] = xml map { elem => (elem \\ "assignmentid").text } filterNot emptyString
	lazy val classId: Option[String] = xml map { elem => (elem \\ "classid").text } filterNot emptyString
	lazy val userId: Option[String] = xml map { elem => (elem \\ "userid").text } filterNot emptyString
	lazy val objectId: Option[String] = xml map { elem => (elem \\ "objectID").text } filterNot emptyString
	lazy val sessionId: Option[String] = xml map { elem => (elem \\ "sessionid").text } filterNot emptyString

	/**
	 * Convert an <object> list into a list of TurnitinSubmissionInfo items.
	 *
	 * It is assumed that the student's Uni ID was used as the first name, so
	 * it is used as the firstname element.
	 */
	lazy val submissionsList: Seq[TurnitinSubmissionInfo] = {
		xml map {
			_ \\ "object" map { obj =>
				TurnitinSubmissionInfo(
					objectId = DocumentId((obj \\ "objectID").text),
					title = (obj \\ "title").text,
					universityId = (obj \\ "firstname").text,
					similarityScore = (obj \\ "similarityScore").text.toInt,
					overlap = parseInt((obj \\ "overlap").text),
					webOverlap = parseInt((obj \\ "web_overlap").text),
					publicationOverlap = parseInt((obj \\ "publication_overlap").text),
					studentPaperOverlap = parseInt((obj \\ "student_paper_overlap").text))
			}
		} getOrElse Nil
	}

	def message: String = TurnitinResponse.responseCodes.getOrElse(code, unknownMessage)
	private def unknownMessage = if (success) "Unknown code" else "Unknown error"
	private def emptyString(s: String) = s.trim.isEmpty
	private def parseInt(text: String) = if (text.isEmpty) None else Some(text.toInt)
}

object TurnitinResponse {

	val StatusRegex: Regex = "(?s).+rcode&gt;(\\d+)&lt;.+".r

	/**
	 * Response in diagnostic mode is all HTML but we can
	 * regex out the status code at least.
	 */
	def fromDiagnostic(string: String): TurnitinResponse = {
		val code = string match {
			case StatusRegex(status) => status.toInt
			case _ => 1
		}
		new TurnitinResponse(code, diagnostic = Some(string))
	}

	def redirect(location: String): TurnitinResponse = {
		new TurnitinResponse(0, redirectUrl = Some(location))
	}

	def fromXml(xml: Elem): TurnitinResponse = {
		new TurnitinResponse((xml \\ "rcode").text.toInt, xml = Some(xml))
	}

	/** Acquired from http://docs.moodle.org/dev/Turnitin_errors */
	// scalastyle:off
	val responseCodes = Map(
		// Success States 1-99

		1 -> "General Success State, no errors",
		10 -> "FID 1, FCMD 1 – successful, send to login",
		11 -> "FID 1, FCMD 2 – successful, do not send to login",
		20 -> "FID 2, FCMD 1 – successful, send to login",
		21 -> "FID 2, FCMD 2 – successful, do not sent to login",
		30 -> "FID 3, FCMD 1 – successful, send to login",
		31 -> "FID 3, FCMD 2 – successful, do not send to login",
		40 -> "FID 4, FCMD 1,5 – successful, redirect user to assignment creation/modification page",
		41 -> "FID 4, FCMD 2 – successful",
		42 -> "FID 4, FCMD 3 – successful",
		43 -> "FID 4, FCMD 4 – successful",
		50 -> "FID 5, FCMD 1 – successful, redirect user to submission page",
		51 -> "FID 5, FCMD 2 - successful",
		60 -> "FID 6, FCMD 1 - successful",
		61 -> "FID 6, FCMD 2 - successful",
		70 -> "FID 7, FCMD 1,2 – successful",
		70 -> "FID 12, FCMD 1 – successful, redirect to administrator statistics page",
		71 -> "FID 8, FCMD 2 - successful",
		72 -> "FID 10, FCMD 2 - successful",
		73 -> "FID 11, FCMD 2 – successful",
		74 -> "FID 0 – successful",
		75 -> "FID 9, FCMD 2 - successful",

		// Data Errors 100-199

		100 -> "Primary account ID missing from URL",
		101 -> "No HTTPS - the URL was not transmitted via HTTPS",
		102 -> "GMT missing from URL",
		103 -> "GMT malformed - the date/time in the URL is bad",
		104 -> "Email missing from URL",
		105 -> "Email address is not between 5-75 characters",
		106 -> "Email address contains whitespace",
		107 -> "Email address in URL is malformed",
		108 -> "Password in URL contained whitespace",
		109 -> "Diagnostic value in URL is bad",
		110 -> "MD5 missing from URL",
		111 -> "fcmd missing from URL",
		112 -> "User first name missing from URL or incorrect length",
		113 -> "User last name missing from URL or incorrect length",
		114 -> "Class title missing from URL, or not between 5-50 characters",
		115 -> "Password is not between 6-12 characters",
		116 -> "fid missing from URL, or does not reference existing function",
		117 -> "User type missing from URL or is not valid",
		118 -> "encrypt value in URL is bad or missing",
		119 -> "Paper author’s first name missing or incorrect length",
		120 -> "Paper author’s last name missing or incorrect length",
		121 -> "Paper title missing – Please make sure to include a paper title before submitting your paper",
		122 -> "Paper type missing or invalid",
		123 -> "Assignment title missing or incorrect length",
		124 -> "ObjectID missing",
		125 -> "Password is required for function",
		126 -> "Date start and date due is required for function",
		127 -> "If one unique ID is used, they must all be used",
		128 -> "The class end date parameter is not in the right format. Please make sure that the format is in YYYMMDD.”",
		129 -> "The class end date parameter is not set. Please make sure that the class enddate is set if you wish to update the class.",
		130 -> "The web services url is not valid. Please make sure that the web services url is a well-formed url.",
		131 -> "The grade for the object is missing. Please make sure to include a grade for this",
		132 -> "The grade for the object is invalid. Please make sure the grade is between 0and max points for the assignment.",
		133 -> "AssignID not allowed if creating an assignment with IDSync on. Please eitherturn IDSync off or remove the assignID.",
		134 -> "submit_papers_to parameter needs to be between 0 and 2 inclusive.",
		140 -> "Undocumented, but appears to occur when assignment title is too long. Should probably return either 2302 or 123 in this situation.",
		141 -> "The value for the a date parameter is invalid",

		// Turnitin Database Errors 200-299

		200 -> "Primary account ID for Turnitin not activated to use the API",
		201 -> "IP address validation range for Turnitin not configured",
		202 -> "Primary account entry not active in Turnitin database",
		203 -> "MD5 key missing from Turnitin database",
		204 -> "Class does not exist in Turnitin database. Please contact you instructor for further details.",
		205 -> "Database error verifying class",
		206 -> "Assignment does not exist in Turnitin database for this class. The assignment may have been deleted.",
		207 -> "Database error verifying assignment",
		208 -> "User is not enrolled in class",
		209 -> "Database error verifying user’s enrollment in class",
		210 -> "User password does not match user email",
		211 -> "Database error verifying objectID",
		212 -> "objectID does not exist for this user",
		213 -> "objectID does not belong to this user",
		214 -> "Filename does not exist for this paper",
		215 -> "This primary account ID is not authorized to use this product",
		216 -> "Student limit for this account has been reached. Cannot join student to this class.",
		217 -> "The product for this account has expired. Please contact your sales agent to renew the product",
		218 -> "Database Error inserting unique ID into the database",
		219 -> "Unique user id in the database does not match submitted uid",
		220 -> "More than one class exists with this title and unique ids must be used in this case",
		221 -> "More than one assignment exists with this title and unique ids must be used in this case",
		222 -> ("User is associated with a different external uid. " +
				"If you have another user account with the same email address as the user account you are currently using, " +
				"that could be the cause of the problem. Please try modifying the email address for your current user account " +
				"to a unique email address and try again."),
		223 -> "Cannot verify Blackboard user’s identity within Turnitin. Missing Blackboard user id",
		224 -> "Could not verify user as primary instructor for this course",
		225 -> "Database error checking if student can view reports",
		226 -> "The class you are trying to update could not be found. Please check to make sure that the class exists.",
		227 -> ("The class you are trying to update has an assignment which ends after the class end date you have specified. " +
				"Please change you class end date or modify the assignment."),
		228 -> ("The assignment with the assignment id that you entered does not belong to the class with the class id you entered. " +
				"Please check to make sure that you are not using a duplicate assignment id."),
		229 -> "There was an error creating the rollover assignment.",
		230 -> ("You have been dropped from this class on the Turnitin end by your instructor. " +
				"Please contact your instructor if you think you are receiving this message in error."),
		231 -> ("There was an error processing your request: please try your action again. " +
				"Please contact the Turnitin helpdesk if the problem persists."),
		232 -> "Grademark is currently inactive for this account.",
		233 -> ("There was an error accessing this Turnitin Assignment because it was created via Course Copy, " +
				"Snapshot, or some other process, and the original Turnitin Assignment could not be found."),
		234 -> "Could not find class with the given unique ID or title.",
		235 -> "Could not find assignment with the given assignment ID or title.",
		236 -> "Could not retrieve assignment info with the given information.",
		237 -> "Could not retrieve grade for the given object.",
		238 -> "Could not find user with the given userid or email.",
		239 -> "There was an error disabling Anonymous Marking for this submission.",
		240 -> "Migrating of this product is not supported.",
		241 -> "The user does not have the specified role in the class.",
		242 -> "There was an error with the attached file, please make sure it follows the proper format.",
		243 -> "The attached file is not a CSV file.",
		244 -> "The number of files you have selected exceeds the 500 file maximum for bulk download. Please make sure that the number of files you select for bulk download does not exceed 500 files.",
		245 -> "The objectIDs that were provided do not all belong to the same assignment.",
		246 -> "The compression process cannot begin because the students you selected do not have GradeMark files.",
		247 -> "We are compressing a file you previously requested for download. When this compression is complete, you may request another file for download.",
		248 -> "The class you have selected has been deleted.",

		// Inconsistency Errors 300-399

		300 -> "Your IP address does not fall within the range of accepted IP addresses as specified by your Turnitin account administrator. Please check with your Turnitin account administrator if your IP address needs to be added as an accepted IP address.",
		301 -> "Date/time expired – GMT timestamp used in MD5 calculation is off. API calls must have a GMT within 60 minutes of the current GMT",
		302 -> "MD5 not authenticated - the MD5 in the URL does not match the MD5 calculated",

		// Function Errors 400-499

		400 -> "Creating new user failed",
		401 -> "Unauthorized access - user exists, but does not belong to correct primary account ID or sub-account ID, do not execute function",
		402 -> "User is not an instructor, must be an instructor to run this function",
		403 -> "User is not a student, must be a student to run this function",
		404 -> "FCMD is not valid",
		405 -> "Class title is not unique",
		406 -> "Creating new class failed in fid 2",
		407 -> "Student failed to join or log in to a class in fid 3",
		408 -> "Attempt to join new user to account failed",
		409 -> "Function requires POST request",
		410 -> "Function requires GET request",
		411 -> "Creating/Updating/Deleting assignment failed in fid 4",
		412 -> "Assignment title is not unique",
		413 -> "Error while trying to save the paper",
		414 -> "Originality report not generated yet in fid 6, fcmd 1",
		415 -> "Originality score not available yet in fid 6, fcmd 2",
		416 -> "Error checking if submission existed for user",
		417 -> "Error trying to change user password",
		418 -> "Error trying to delete submission",
		419 -> "Could not create a new assignment. An assignment with this title already exists for this class and instructor. Please change the assignment title.",
		420 -> "Error trying to build up session data for user/class",
		421 -> "Error trying to retrieve paper submission info for assignment",
		422 -> "Updating user information failed",
		423 -> "Updating user information failed because user email was changed to an address that is already associated with an account",
		424 -> "Updating class title failed",
		425 -> "Error trying to sync grades between servers",
		426 -> "Error trying to sync roster between servers",
		427 -> "Unable to establish web services session with remove webct server",
		428 -> "Unable to create WebCT gradebook column for assignment",
		429 -> "Web services parameters error",
		430 -> "(This is a general webservices error – there could be a number of different messages that come with it)",
		431 -> "Error trying to connect back to the Blackboard web service.",
		432 -> "Students are not allowed to view reports in this assignment",
		434 -> "The start time you specified is invalid or not formatted correctly (YYYY-MM-DD HH:MM:SS).",
		435 -> "Error trying to grade submission.",
		436 -> "Institution repository is not available for this account, please change the submit_papers_to parameter to either 0 (no repository) or 1 (standards repository).",
		437 -> "The account administrator has set that all papers must be submitted to a repository, please change the submit_papers_to parameter.",
		438 -> "Error trying to set submissions to be stored in a standard repository. The account is using its own private institutional node. Please change the submit_papers_to parameter.",
		439 -> "The institutional check is not available for this account. Please set the institution_check parameter to 0",
		440 -> "Please specify an email for the new instructor for this course.",
		441 -> "New instructor specified doesn’t exist, please create the user first.",
		442 -> "New Instructor is not joined to this account, please join them to the account first.",
		443 -> "Anonymous Marking is enabled for this assignment. Cannot download file until after the assignment post date.",
		444 -> "Usage of this account is not currently high enough to generate meaningful statistics. If you would like more information or would like to see these statistics any way, please contact your Turnitin sales representative.",
		445 -> "User login failed.",
		446 -> "Session ID missing from URL",
		447 -> "Error trying to set user as an instructor for the class.",
		448 -> "Error migrating this account to another product.",
		449 -> "Error removing user from class.",
		450 -> "Cannot remove user from class because the user is the only instructor for the class.",
		451 -> "Error initiating GradeMark Bulk Download.",

		// Paper Submission Errors 1000-1099

		1000 -> "The due date for this assignment has passed. Please see your instructor to request a late submission.",
		1001 -> "You may not submit a paper to this assignment until the assignment start date",
		1002 -> "You may not submit a paper to this assignment because the Plagiarism Prevention product is unavailable",
		1003 -> "You have reached the maximum limit of 10 papers for the InSite demo account",
		1004 -> "Paper author’s first name missing or incorrect length in URL",
		1005 -> "Paper author’s last name missing or incorrect length in URL",
		1006 -> "Paper title missing",
		1007 -> "The file you have uploaded is too big (File size may not exceed 20 MB)",
		1008 -> "No file uploaded! Please make sure that you have attached the file that you wish to submit before sending the request",
		1009 -> "Invalid file type! Valid file types are MS Word, Acrobat PDF, Postscript, Text, HTML, WordPerfect (WPD) and Rich Text Format. Please make sure the format of your file is one of the valid file types.",
		1010 -> "You must submit more than 100 characters of non-whitespace",
		1011 -> "The paper you are tyring to submit is incorrectly formatted. There seems to be spaces between each letter in your paper. Please try submitting again or contact our helpdesk if the problem persists.",
		1012 -> "Paper length exceeds limits",
		1013 -> "You must submit more than 20 words of text",
		1014 -> "You must select an enrolled student as the author of this paper",
		1015 -> "You have already submitted a paper to this assignment. Please contact your instructor to request a resubmission",
		1016 -> "This student has already submitted a paper to this assignment. Please delete the original paper before submitting a new one.",
		1017 -> "Paper author's first name missing or incorrect length in URL",
		1018 -> "Paper author's last name missing or incorrect length in URL",
		1019 -> "Paper title exceeds maximum of 200 characters",
		1020 -> "This document cannot be accepted because it contains characters from a character set that is not supported.",
		1021 -> "You have already submitted a paper to this assignment. Please contact your instructor to request a resubmission.",
		1022 -> "This student has already submitted a portfolio item to this assignment. Please delete the original portfolio item before submitting a new one.",
		1023 -> "We're sorry, but we could not read the PDF you submitted. Please make sure that the file is not password protected and contains selectable text rather than scanned images.",
		1024 -> "The paper you are tyring to submit does not meet our cirteria for a legitimate paper. Please try submitting again or contact our helpdesk if the problem persists.",

		// Assignment Creation Errors 2025-2324

		2025 -> "The class name must be between 5-200 characters",
		2026 -> "The class enrollment password must be between 4-12 characters",
		2027 -> "There was an error processing your request",
		2028 -> "The class end date must be within 6 months of the start date",
		2029 -> "The end date for this class must occur on or after today",
		2030 -> "The end date for this class must occur on or after the start date",
		2031 -> "The end date for this class must occur at least 3 months after the start date",
		2032 -> "There was an error updating the class end date",
		2035 -> "There was an error processing your request",
		2036 -> "There was an error processing your request",
		2100 -> "User first name missing from URL",
		2101 -> "User last name missing from URL",
		2102 -> "Email is missing. Please make sure that your email address has been set",
		2108 -> "The email address needs to conform to Internet Standard RFC822",
		2109 -> "The email address needs to have a fully qualified domain name.",
		2110 -> "We only allow email addressses with 5-75 characters",
		2111 -> "The email address cannot contain white space",
		2112 -> "Please make sure you are entering a valid email address. The email address you enter can only contain letters, numbers, and the symbols _ (underscore), - (dash), . (period), ' (apostrophe), and + (plus).”",
		2300 -> "You have entered an invalid date!",
		2301 -> "You must select a rubric set to user with remediation",
		2302 -> "The assignment title must be between 2-100 characters",
		2303 -> "The assignment must have a point value between 0 and 1000",
		2304 -> "The assignment instructions must be less than 1000 characters",
		2305 -> "The start date for this assignment must occur on or after today",
		2306 -> "The due date for this assignment must occur on or after the start date",
		2307 -> "The due date for this assignment must occur within 6 months of the start date",
		2308 -> "When creating your assignment, the post date must occur on or before the class end date. The class end date in Turnitin is by default set to 6 months from the day the class was created. The class end date can be chaned in the class update area in Turnitin",
		2309 -> "When creating your assignment, please make sure that the post date is on or after the due date of the assignment",
		2310 -> "You must specify a point value for this assignment because this class is using distributed grading",
		2314 -> "When modifying your assignment, please make sure that the post date is on or after the due date of the assignment",
		2315 -> "There was an error processing your request",
		2316 -> "There was an error processing your request",
		2317 -> "There was an error processing your request",
		2318 -> "You cannot change the search targets because papers have already been submitted to this assignment. You must create a new assignment if you would like to change the search targets",
		2319 -> "There was an error processing your request",
		2320 -> "There was an error processing your request",
		2321 -> "When excluding small matches by word, you must enter a positive number",
		2324 -> "When excluding small matches by percentage, you must enter a number between 1 and 100",

		// Custom Tabula errors 9000+
		9000 -> "Turnitin server unavailable",
		9001 -> "Unexpected formatting in response from Turnitin"
	)


}