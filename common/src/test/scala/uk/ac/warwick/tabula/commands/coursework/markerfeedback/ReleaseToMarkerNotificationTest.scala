package uk.ac.warwick.tabula.commands.coursework.markerfeedback

import uk.ac.warwick.tabula.data.model.notifications.coursework.ReleaseToMarkerNotification
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.web.Routes

class ReleaseToMarkerNotificationTest  extends TestBase with Mockito {

	val TEST_CONTENT = "test"
	val userLookupService: UserLookupService = mock[UserLookupService]

	val cm1Prefix = "coursework"
	Routes.coursework._cm1Prefix = Some(cm1Prefix)

	def createNotification(agent: User, recipient: User, _object: Seq[MarkerFeedback], assignment: Assignment, isFirstMarker: Boolean): ReleaseToMarkerNotification = {
		val n = Notification.init(new ReleaseToMarkerNotification, agent, _object, assignment)
		userLookupService.getUserByUserId(recipient.getUserId) returns recipient
		n.userLookup = userLookupService
		n.recipientUserId = recipient.getUserId
		n.whichMarker.value = if (isFirstMarker) 1 else 2
		n
	}

	trait ReleaseNotificationFixture extends MarkingNotificationFixture {

		testAssignment.markingWorkflow = new ModeratedMarkingWorkflow

		val (f1, mf1) = makeMarkerFeedback(student1)(MarkingNotificationFixture.FirstMarkerLink)
		val (f2, mf2) = makeMarkerFeedback(student2)(MarkingNotificationFixture.FirstMarkerLink)
	}

	@Test
	def titleIncludesModuleAndAssignmentName(){ new ReleaseNotificationFixture {
		val n: ReleaseToMarkerNotification =  createNotification(marker1, marker2, Seq(mf1, mf2), testAssignment, isFirstMarker = true)
		n.title should be("HERON101: Submissions for \"Test assignment\" have been released for marking")
	} }

	@Test
	def urlIsProfilePageForStudents():Unit = new ReleaseNotificationFixture{
		val n: ReleaseToMarkerNotification =  createNotification(marker1, marker2, Seq(mf1, mf2), testAssignment, isFirstMarker = true)
		n.url should be(s"/$cm1Prefix/admin/module/heron101/assignments/1/marker/marker2/list")
	}


	@Test
	def shouldCallTextRendererWithCorrectTemplate():Unit = new ReleaseNotificationFixture {
		val n: ReleaseToMarkerNotification =  createNotification(marker1, marker2, Seq(mf1, mf2), testAssignment, isFirstMarker = true)

		val content: FreemarkerModel = n.content
		content.template should be ("/WEB-INF/freemarker/emails/released_to_marker_notification.ftl")
	}

	@Test
	def shouldCallTextRendererWithCorrectModel():Unit = new ReleaseNotificationFixture {
		val n: ReleaseToMarkerNotification =  createNotification(marker1, marker2, Seq(mf1, mf2), testAssignment, isFirstMarker = true)

		val model: Map[String, Any] = n.content.model

		n.url should be(s"/$cm1Prefix/admin/module/heron101/assignments/1/marker/marker2/list")
		model("assignment") should be(testAssignment)
		model("numReleasedFeedbacks") should be(2)
		model("workflowVerb") should be("mark")
	}
}