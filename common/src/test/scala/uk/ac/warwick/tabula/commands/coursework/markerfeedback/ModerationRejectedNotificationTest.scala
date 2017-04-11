package uk.ac.warwick.tabula.commands.coursework.markerfeedback


import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.coursework.ModeratorRejectedNotification
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class ModerationRejectedNotificationTest  extends TestBase with Mockito {

	val HERON_PLUG = "Not enough time has been given to explaining the awesome nature of Herons"
	val userLookupService: UserLookupService = mock[UserLookupService]

	val cm1Prefix = "coursework"
	Routes.coursework._cm1Prefix = Some(cm1Prefix)

	def createNotification(agent: User, recipient: User, markerFeedback: MarkerFeedback): ModeratorRejectedNotification = {
		val n =  Notification.init(new ModeratorRejectedNotification, agent, Seq(markerFeedback))
		userLookupService.getUserByUserId(recipient.getUserId) returns recipient
		userLookupService.getUserByWarwickUniId(recipient.getWarwickId) returns recipient
		n.userLookup = userLookupService
		n
	}

	trait ModeratorRejectedNotificationFixture extends MarkingNotificationFixture {
		testAssignment.markingWorkflow = new ModeratedMarkingWorkflow
		testAssignment.markingWorkflow.userLookup = userLookupService

		val markerMap = new FirstMarkersMap {
			assignment = testAssignment
			marker_id = marker1.getUserId
			students = Fixtures.userGroup(student1)
		}

		testAssignment.firstMarkers = JArrayList(markerMap)

		val (f, mf1, mf2) = makeBothMarkerFeedback(student1)
		mf2.rejectionComments = HERON_PLUG
		mf2.mark = Some(41)
		mf2.grade = Some("3")

		userLookupService.getUserByWarwickUniId(student1.getWarwickId) returns student1
		userLookupService.getUserByUserId(student1.getWarwickId) returns student1
	}

	@Test
	def titleIncludesModuleAndAssignmentName(){ new ModeratorRejectedNotificationFixture {
		val n: ModeratorRejectedNotification =  createNotification(marker2, marker1, mf2)
		n.title should be("HERON101: Feedback for student1 for \"Test assignment\" has been rejected by the moderator")
	} }

	@Test
	def urlIsProfilePageForStudents():Unit = new ModeratorRejectedNotificationFixture{
		val n: ModeratorRejectedNotification =  createNotification(marker2, marker1, mf2)
		n.url should be(s"/$cm1Prefix/admin/module/heron101/assignments/1/marker/marker1/list")
	}


	@Test
	def shouldCallTextRendererWithCorrectTemplate():Unit = new ModeratorRejectedNotificationFixture {
		val n: ModeratorRejectedNotification =  createNotification(marker2, marker1, mf2)
		n.content.template should be("/WEB-INF/freemarker/emails/moderator_rejected_notification.ftl")
	}

	@Test
	def shouldCallTextRendererWithCorrectModel():Unit = new ModeratorRejectedNotificationFixture {

		val n: ModeratorRejectedNotification =  createNotification(marker2, marker1, mf2)
		n.url should be (s"/$cm1Prefix/admin/module/heron101/assignments/1/marker/marker1/list")
		n.content.model.get("assignment") should be(Some(testAssignment))
		n.content.model.get("studentId") should be(Some("student1"))
		n.content.model.get("moderatorName") should be(Some("Snorkeldink Wafflesmack"))
		n.content.model.get("rejectionComments") should be(Some(HERON_PLUG))
		n.content.model.get("adjustedMark") should be(Some(Some(41)))
		n.content.model.get("adjustedGrade") should be(Some(Some("3")))
	}
}
