package uk.ac.warwick.tabula.data.model.notifications.coursework

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

class FeedbackAdjustmentNotificationTest extends TestBase with Mockito {

	val cm1Prefix = "coursework"
	Routes.coursework._cm1Prefix = Some(cm1Prefix)

	val admin: User = Fixtures.user("1170836", "1170836")
	val marker: User = Fixtures.user("1234567", "1234567")
	val student: User = Fixtures.user("7654321", "7654321")

	val mockLookup: UserLookupService = mock[UserLookupService]
	mockLookup.getUserByUserId(student.getUserId) returns student
	mockLookup.getUserByUserId(marker.getUserId) returns marker
	mockLookup.getUserByWarwickUniId(student.getWarwickId) returns student
	mockLookup.getUserByWarwickUniId(marker.getWarwickId) returns marker

	val module: Module = Fixtures.module("hnz101")
	val assignment: Assignment = Fixtures.assignment("hernz")
	assignment.id = "heronzzzz"
	assignment.module = module
	val feedback: AssignmentFeedback = Fixtures.assignmentFeedback(student.getWarwickId)
	feedback.assignment = assignment
	val workflow = new FirstMarkerOnlyWorkflow()
	workflow.userLookup = mockLookup
	assignment.markingWorkflow = workflow
	assignment.firstMarkers = Seq(FirstMarkersMap(assignment, "1234567", Fixtures.userGroup(student))).asJava


	def createNotification: FeedbackAdjustmentNotification = {
		val n = Notification.init(new FeedbackAdjustmentNotification, marker, feedback, assignment)
		n.userLookup = mockLookup
		n
	}

	@Test
	def urlIsMarkerPage() {
		val n = createNotification
		n.url should be(s"/$cm1Prefix/admin/module/hnz101/assignments/heronzzzz/marker/1234567/list")
	}

	@Test
	def titleShouldContainMessage() {
		val n = createNotification
		n.title should be("HNZ101 - for hernz : Adjustments have been made to feedback for 7654321")
	}

	@Test
	def recipientsContainsAllAdmins() {
		val n = createNotification
		n.recipients should be (Seq(marker))
	}

	@Test
	def shouldCallTextRendererWithCorrectTemplate() {
		val n = createNotification
		n.content.template should be { "/WEB-INF/freemarker/emails/feedback_adjustment_notification.ftl" }
	}

	@Test
	def shouldCallTextRendererWithCorrectModel() {
		val n = createNotification
		n.content.model("assignment") should be(assignment)
		n.content.model("feedback") should be(feedback)
	}

}
