package uk.ac.warwick.tabula.commands.coursework.departments

import uk.ac.warwick.tabula.data.model._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.elasticsearch.AuditEventQueryMethods
import collection.JavaConversions._
import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.userlookup.User

import scala.concurrent.Future

// scalastyle:off magic.number
// reusable environment for marking workflow tests
trait ReportWorld extends TestBase with Mockito {

	var assignmentMembershipService: AssessmentMembershipService = mock[AssessmentMembershipService]
	assignmentMembershipService.determineMembershipUsers(any[Assignment]) answers { assignmentObj =>
		val assignment = assignmentObj.asInstanceOf[Assignment]
		val studentIds = assignment.members.knownType.includedUserIds
		val users = studentIds.map{userId =>
			val userOne = new User(userId)
			userOne.setWarwickId(userId)
			userOne
		}.toList
		users
	}

	val extensionService: ExtensionService = mock[ExtensionService]
	extensionService.hasExtensions(any[Assignment]) answers (assignmentObj => {
		val assignment = assignmentObj.asInstanceOf[Assignment]
		!assignment.extensions.isEmpty
	})

	val department = new Department
	department.code = "IN"
	department.fullName = "Test Department"

	val moduleOne = new Module("IN101", department)
	moduleOne.name = "Module One"
	val moduleTwo = new Module("IN102", department)
	moduleTwo.name = "Module Two"

	department.modules = List(moduleOne, moduleTwo)

	var auditEvents: List[AuditEvent] = List()

	val assignmentOne: Assignment = addAssignment("1001", "test one", dateTime(2013, 3, 10), 10, 5, moduleOne)
	val assignmentTwo: Assignment = addAssignment("1002", "test two", dateTime(2013, 4, 10), 29, 5, moduleOne)
	val assignmentThree: Assignment = addAssignment("1003", "test three", dateTime(2013, 5, 10), 13, 5, moduleOne)

	assignmentThree.summative = false

	val assignmentFour: Assignment = addAssignment("1004", "test four", dateTime(2013, 5, 30), 35, 5, moduleTwo)
	val assignmentFive: Assignment = addAssignment("1005", "test five", dateTime(2013, 8, 22), 100, 50, moduleTwo)
	val assignmentSix: Assignment = addAssignment("1006", "test six", dateTime(2013, 7, 1), 73, 3, moduleTwo)
	val assignmentSeven: Assignment = addAssignment("1007", "test seven", dateTime(2013, 7, 1), 100, 50, moduleTwo)
	val assignmentEight: Assignment = addAssignment("1008", "test eight", dateTime(2013, 7, 1), 100, 50, moduleTwo)

	assignmentSeven.dissertation = true

	createPublishEvent(assignmentOne, 15, studentData(1, 10)) 	// all on time
	createPublishEvent(assignmentTwo, 35, studentData(1, 29))	// all late
	createPublishEvent(assignmentThree, 10, studentData(1, 4))	// for test three - these on time
	createPublishEvent(assignmentThree, 35, studentData(5, 13))	// ... and these late

	createPublishEvent(assignmentFour, 29, studentData(1, 35))	// on time for the 7 (that is, 35 % 5) that have submitted late, late for the rest
	createPublishEvent(assignmentFive, 32, studentData(1, 100))	// on time for the 2 (100 % 50) that have submitted late, late for the rest
	createPublishEvent(assignmentSix, 15, studentData(1, 23))		// on time
	createPublishEvent(assignmentSix, 20, studentData(24, 65))	// on time
	createPublishEvent(assignmentSix, 31, studentData(66, 73))	// late
	createPublishEvent(assignmentSeven, 31, studentData(1, 50))	// seemingly late because it's a dissertation it's treated as on-time
	createPublishEvent(assignmentEight, 31, studentData(1, 50))	// late (same details as assignmentSeven, just not a dissertation)

	var auditEventQueryMethods: AuditEventQueryMethods = mock[AuditEventQueryMethods]

	auditEventQueryMethods.publishFeedbackForStudent(any[Assignment], any[String], any[Option[String]]) answers {argsObj => {
		val args = argsObj.asInstanceOf[Array[_]]
		val assignment = args(0).asInstanceOf[Assignment]
		val warwickId = args(1).asInstanceOf[String]
		Future.successful(auditEvents.filter(event => {event.students.contains(warwickId) && event.assignmentId.get == assignment.id}))
	}}


	var submissionService: SubmissionService = mock[SubmissionService]
	submissionService.getSubmissionsByAssignment(any[Assignment]) answers { assignmentObj =>
		val assignment = assignmentObj.asInstanceOf[Assignment]
		assignment.submissions
	}


	var feedbackService: FeedbackService = mock[FeedbackService]
	feedbackService.getAssignmentFeedbackByUsercode(any[Assignment], any[String]) answers { argsObj => {
		val args = argsObj.asInstanceOf[Array[_]]
		val assignment = args(0).asInstanceOf[Assignment]
		val usercode = args(1).asInstanceOf[String]
		assignment.feedbacks.find(_.usercode == usercode)
	}}

	def studentData(start:Int, end:Int): List[String] = (start to end).map(i => s"u${idFormat(i)}").toList

	def createPublishEvent(assignment: Assignment, daysAfter: Int, students: List[String]) {
		val date = assignment.closeDate.plusDays(daysAfter)
		val event = AuditEvent(
				eventId="event", eventType="PublishFeedback", userId="cuslat", eventDate=date,
				eventStage="before", data="""{"assignment": """ + assignment.id + """,
											  "students" : """ + studentsData(students) + """  }"""
			)

		event.related = Seq(event)
		event.parsedData = Some(json.readValue(event.data, classOf[Map[String, Any]]))

		auditEvents = event :: auditEvents
	}

	def studentsData(students: List[String]): StringBuilder = students.addString(new StringBuilder(), """["""", """","""", """"]""")

	val extension = new Extension

	val id = idFormat(3)
	extension._universityId = id
	extension.usercode = s"u$id"
	extension.approve()
	extension.expiryDate = assignmentSix.closeDate.plusDays(2)
	extension.assignment =  assignmentSix
	assignmentSix.extensions = Seq(extension)

	def addAssignment(id: String, name: String, closeDate: DateTime, numberOfStudents: Int, lateModNumber: Int, module: Module): Assignment = {
		val assignment = new Assignment(module)
		assignment.assessmentMembershipService = assignmentMembershipService
		assignment.extensionService = extensionService
		assignment.setDefaultBooleanProperties()
		assignment.id = id
		assignment.name = name
		assignment.closeDate = closeDate
		assignment.collectSubmissions = true
		assignment.module = module

		for (i <- 1 to numberOfStudents) {
			generateSubmission(assignment, i, lateModNumber)
			addFeedback(assignment)
		}

		val userIds = (1 to numberOfStudents).map(idFormat)
		assignment.members = makeUserGroup(userIds)
		module.assignments.add(assignment)

		assignment
	}



	def addFeedback(assignment:Assignment) {
		withFakeTime(dateTime(2013, 3, 13)) {
			val feedback = assignment.submissions.map { s=>
				val newFeedback = new AssignmentFeedback
				newFeedback.assignment = assignment
				newFeedback.usercode = s.usercode
				newFeedback._universityId = s._universityId
				newFeedback.released = true
				newFeedback
			}
			assignment.feedbacks = feedback
		}
	}


	def idFormat(i:Int): String = "1" + ("%06d" format i)

	def generateSubmission(assignment: Assignment, num: Int, lateModNumber: Int) {
		val submissionDate = if (lateModNumber != 0 && num % lateModNumber == 0) {
			assignment.closeDate.plusDays(1)
		} else {
			assignment.closeDate.minusDays(1)
		}

		withFakeTime(submissionDate) {
			val submission = new Submission()
			submission.assignment = assignment
			val id = idFormat(num)
			submission._universityId = id
			submission.usercode = s"u$id"
			submission.submittedDate = submissionDate
			assignment.submissions.add(submission)

			val event = AuditEvent(
				eventId="event", eventType="SubmitAssignment", userId=idFormat(num), eventDate=DateTime.now,
				eventStage="before", data="""{"assignment": """ + assignment.id + """}"""
			)

			event.related = Seq(event)
			event.parsedData = Some(json.readValue(event.data, classOf[Map[String, Any]]))

			auditEvents = event :: auditEvents
		}
	}

	def makeUserGroup(users: Seq[String]): UserGroup = {
		val ug = UserGroup.ofUsercodes
		ug.includedUserIds = users
		ug
	}
}