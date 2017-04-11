package uk.ac.warwick.tabula.commands.coursework.markerfeedback

import uk.ac.warwick.tabula.{Mockito, Fixtures}
import uk.ac.warwick.tabula.services.UserLookupService
import org.mockito.Mockito._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.userlookup.User

object MarkingNotificationFixture {
	val FirstMarkerLink: (Feedback, MarkerFeedback) => Unit = {(f, mf) => f.firstMarkerFeedback = mf}
	val SecondMarkerLink: (Feedback, MarkerFeedback) => Unit = {(f, mf) => f.secondMarkerFeedback = mf}
}

trait MarkingNotificationFixture extends Mockito {

	val department = new Department()
	val testmodule = new Module("heron101", department)
	val testAssignment = new Assignment{ id = "1"; name = "Test assignment"; module = testmodule }

	val marker1: User = Fixtures.user("marker1", "marker1")
	val marker2: User = Fixtures.user("marker2", "marker2")
	marker2.setFullName("Snorkeldink Wafflesmack")
	val marker3: User = Fixtures.user("marker3", "marker3")
	val student1: User = Fixtures.user("student1", "student1")
	val student2: User = Fixtures.user("student2", "student2")
	val student3: User = Fixtures.user("student3", "student3")
	val student4: User = Fixtures.user("student4", "student4")

	val mockUserLookup: UserLookupService = mock[UserLookupService]
	when(mockUserLookup.getUserByUserId("marker1")) thenReturn marker1
	when(mockUserLookup.getUserByUserId("marker2")) thenReturn marker2
	when(mockUserLookup.getUserByUserId("marker3")) thenReturn marker3
	when(mockUserLookup.getUserByUserId("student1")) thenReturn student1
	when(mockUserLookup.getUserByUserId("student2")) thenReturn student2
	when(mockUserLookup.getUserByUserId("student3")) thenReturn student3
	when(mockUserLookup.getUserByUserId("student4")) thenReturn student4


	def userGroup(usercodes: String *): UserGroup = {
		val userGroup = UserGroup.ofUsercodes
		userGroup.includedUserIds = usercodes
		userGroup
	}

	def makeMarkerFeedback(student: User)(linkFunction: (Feedback, MarkerFeedback) => Unit): (AssignmentFeedback, MarkerFeedback) = {
		val feedback = new AssignmentFeedback
		feedback.usercode = student.getUserId
		feedback._universityId = student.getWarwickId
		val mf = new MarkerFeedback()
		mf.userLookup = mockUserLookup
		mf.feedback = feedback
		feedback.assignment = testAssignment
		testAssignment.feedbacks.add(feedback)
		// link the markerFeedback to the appropriate field of the feedback
		linkFunction(feedback, mf)
		(feedback, mf)
	}

	def makeBothMarkerFeedback(student: User) : (Feedback, MarkerFeedback, MarkerFeedback) = {
		val (f, mf1) = makeMarkerFeedback(student)(MarkingNotificationFixture.FirstMarkerLink)
		val mf2 = new MarkerFeedback()
		mf2.userLookup = mockUserLookup
		mf2.feedback = f
		f.secondMarkerFeedback = mf2
		(f, mf1, mf2)
	}
}
