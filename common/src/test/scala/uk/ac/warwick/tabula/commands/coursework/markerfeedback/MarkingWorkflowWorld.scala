package uk.ac.warwick.tabula.commands.coursework.markerfeedback


import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.{Features, MockUserLookup, TestHelpers}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

// reusable environment for marking workflow tests
trait MarkingWorkflowWorld extends TestHelpers {

	val mockUserLookup = new MockUserLookup

	mockUserLookup.registerUserObjects(
		new User("cusxad") { setWarwickId("9876004"); setFoundUser(true); setVerified(true); },
		new User("cuscao") { setWarwickId("0270954"); setFoundUser(true); setVerified(true); },
		new User("curef") { setWarwickId("9170726"); setFoundUser(true); setVerified(true); },
		new User("cusebr") { setWarwickId("0672088"); setFoundUser(true); setVerified(true); },
		new User("cuscav") { setWarwickId("0672089"); setFoundUser(true); setVerified(true); },

		new User("cuslaj") { setWarwickId("1170836"); setFoundUser(true); setVerified(true); },
		new User("cuslat") { setWarwickId("1171795"); setFoundUser(true); setVerified(true); },
		new User("cuday") { setWarwickId("7170124"); setFoundUser(true); setVerified(true); }
	)

	val assignment: Assignment = newDeepAssignment(moduleCode = "IN101")
	generateSubmission(assignment, "9876004", "cusxad")
	generateSubmission(assignment, "0270954", "cuscao")
	generateSubmission(assignment, "9170726", "curef")
	generateSubmission(assignment, "0672088", "cusebr")
	generateSubmission(assignment, "0672089", "cuscav")
	addFeedback(assignment)

	var markingWorkflow = new SeenSecondMarkingWorkflow()
	markingWorkflow.department = assignment.module.adminDepartment
	markingWorkflow.firstMarkers = makeUserGroup("cuslaj", "cuscav")
	markingWorkflow.secondMarkers = makeUserGroup("cuslat", "cuday")
	markingWorkflow.userLookup = mockUserLookup
	assignment.markingWorkflow = markingWorkflow
	assignment.userLookup = mockUserLookup

	val firstMarkers: JList[FirstMarkersMap] = JArrayList()
	firstMarkers.addAll(Seq(
		FirstMarkersMap(assignment, "cuslaj", makeUserGroup("cusxad", "cuscao", "curef")),
		FirstMarkersMap(assignment, "cuscav", makeUserGroup("cusebr", "cuscav"))
	).asJava)

	assignment.firstMarkers = firstMarkers

	val secondMarkers: JList[SecondMarkersMap] = JArrayList()
	secondMarkers.addAll(Seq(
		SecondMarkersMap(assignment, "cuslat", makeUserGroup("cusxad", "cuscao", "curef")),
		SecondMarkersMap(assignment, "cuday", makeUserGroup("cusebr", "cuscav"))
	).asJava)

	assignment.secondMarkers = secondMarkers

	val zipService = new ZipService
	zipService.userLookup = mockUserLookup
	zipService.features = Features.empty
	zipService.objectStorageService = createTransientObjectStore()

	def addFeedback(assignment:Assignment){
		val feedback = assignment.submissions.asScala.map{ s =>
			val newFeedback = new AssignmentFeedback
			newFeedback.assignment = assignment
			newFeedback.uploaderId = "cuslaj"
			newFeedback._universityId = s._universityId
			newFeedback.usercode = s.usercode
			newFeedback.released = false
			addMarkerFeedback(newFeedback,FirstFeedback)
			newFeedback
		}
		assignment.feedbacks = feedback.asJava
	}

	def setFirstMarkerFeedbackState(state: MarkingState): Unit =
		assignment.feedbacks.asScala.foreach( mf => mf.firstMarkerFeedback.state = state )

	def setSecondMarkerFeedbackState(state: MarkingState): Unit =
		assignment.feedbacks.asScala.foreach( mf => mf.secondMarkerFeedback.state = state )

	def setFinalMarkerFeedbackState(state: MarkingState): Unit =
		assignment.feedbacks.asScala.foreach( mf => mf.thirdMarkerFeedback.state = state )

	def addMarkerFeedback(feedback: Feedback, position: FeedbackPosition): Unit = {
		val mf = position match {
			case ThirdFeedback =>
				val mf = new MarkerFeedback(feedback)
				feedback.thirdMarkerFeedback = mf
				mf
			case SecondFeedback =>
				val mf = new MarkerFeedback(feedback)
				feedback.secondMarkerFeedback = mf
				mf
			case _ =>
				val mf = new MarkerFeedback(feedback)
				feedback.firstMarkerFeedback = mf
				mf
		}
		mf.userLookup = mockUserLookup
		mf.state = MarkingState.InProgress
	}


	def generateSubmission(assignment:Assignment, uniId: String, userCode: String) {
		val submission = new Submission()
		submission.assignment = assignment
		submission._universityId = uniId
		submission.usercode = userCode
		assignment.submissions.add(submission)
	}

	def makeUserGroup(users: String*): UserGroup = {
		val ug = UserGroup.ofUsercodes
		ug.includedUserIds = users
		ug.userLookup = mockUserLookup
		ug
	}
}
