package uk.ac.warwick.tabula.data.model.notifications.cm2

import uk.ac.warwick.tabula.data.model.Assignment.MarkerAllocation
import uk.ac.warwick.tabula.data.model.{Department, FirstMarkersMap, Notification, UserGroup}
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.SingleMarker
import uk.ac.warwick.tabula.data.model.markingworkflow.SingleMarkerWorkflow
import uk.ac.warwick.tabula.web.views.UserLookupTag
import uk.ac.warwick.tabula.{Fixtures, MockUserLookup, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

import collection.JavaConverters._

class ReleaseToMarkerNotificationTest extends TestBase with Mockito {


	val tag = new UserLookupTag

	val userLookup = new MockUserLookup
	tag.userLookup = userLookup

	userLookup.registerUsers("1170836", "1000001")
	val dept: Department = Fixtures.department("in")
	val marker: User = Fixtures.user("1170836", "1170836")
	val stu1 = Fixtures.user(universityId = "1000001", userId = "1000001")

	@Test
	def ss(): Unit = withUser("1170836") {

		val w = SingleMarkerWorkflow("test", dept, Seq(marker))

		w.stageMarkers = Seq().asJava
		val assignment = Fixtures.assignment("test")
		assignment.cm2Assignment = true
		assignment.cm2MarkingWorkflow = w

		assignment.firstMarkers.addAll(Seq(
			FirstMarkersMap(assignment, "1170836", UserGroup.ofUsercodes),
		).asJava)

		val feedback = Fixtures.assignmentFeedback("1000001", "1000001")
		feedback.assignment = assignment
		val markerFeedback = Fixtures.markerFeedback(feedback)
		markerFeedback.marker = marker
		markerFeedback.stage = SingleMarker

		val notification = Notification.init(new ReleaseToMarkerNotification, currentUser.apparentUser, markerFeedback, assignment)

		notification.allocatedStudents should be (11)
		notification.allocatedFirstMarker.size should be (1)

	}

}
