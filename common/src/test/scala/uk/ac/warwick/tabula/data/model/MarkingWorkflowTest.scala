package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.forms.{MarkerSelectField, SavedFormValue}

import collection.JavaConverters._
import uk.ac.warwick.tabula.services.{SubmissionService, UserLookupService}
import org.mockito.Mockito._
import uk.ac.warwick.tabula.data.model.MarkingMethod.{ModeratedMarking, SeenSecondMarking, SeenSecondMarkingLegacy}
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.userlookup.User

class MarkingWorkflowTest extends TestBase with Mockito {


	trait MarkingWorkflowFixture {

		val cm1Prefix = "coursework"
		Routes.coursework._cm1Prefix = Some(cm1Prefix)

		val module: Module = Fixtures.module("heron101")
		val assignment: Assignment = Fixtures.assignment("my assignment")
		assignment.id = "1"
		assignment.module = module

		val sub1: Submission = Fixtures.submission(universityId="0000001", userId="student1")
		val sub2: Submission = Fixtures.submission(universityId="0000002", userId="student2")
		val sub3: Submission = Fixtures.submission(universityId="0000003", userId="student3")
		val sub4: Submission = Fixtures.submission(universityId="0000004", userId="student4")

		val f1: AssignmentFeedback = Fixtures.assignmentFeedback(universityId="0000001", userId="student1")
		val f2: AssignmentFeedback = Fixtures.assignmentFeedback(universityId="0000002", userId="student2")
		val f3: AssignmentFeedback = Fixtures.assignmentFeedback(universityId="0000003", userId="student3")
		val f4: AssignmentFeedback = Fixtures.assignmentFeedback(universityId="0000004", userId="student4")

		assignment.submissions.addAll(Seq(sub1, sub2, sub3, sub4).toList.asJava)
		assignment.submissions.asScala.toList foreach { _.assignment = assignment }
		assignment.feedbacks.addAll(Seq(f1, f2, f3, f4).toList.asJava)

		// f1 isn't released yet
		f2.firstMarkerFeedback = Fixtures.markerFeedback(f2)
		f3.firstMarkerFeedback = Fixtures.markerFeedback(f3)
		f4.firstMarkerFeedback = Fixtures.markerFeedback(f4)

		val s1: User = Fixtures.user(universityId="0000001", userId="student1")
		val s2: User = Fixtures.user(universityId="0000002", userId="student2")
		val s3: User = Fixtures.user(universityId="0000003", userId="student3")
		val s4: User = Fixtures.user(universityId="0000004", userId="student4")

		val m1: User = Fixtures.user(universityId="0000005", userId="cuscav")
		val m2: User = Fixtures.user(universityId="0000006", userId="cusebr")
		val m3: User = Fixtures.user(universityId="0000007", userId="curef")
		val m4: User = Fixtures.user(universityId="0000008", userId="cusfal")

		val userLookup: UserLookupService = mock[UserLookupService]
		when(userLookup.getUserByUserId("student1")).thenReturn(s1)
		when(userLookup.getUserByUserId("student2")).thenReturn(s2)
		when(userLookup.getUserByUserId("student3")).thenReturn(s3)
		when(userLookup.getUserByUserId("student4")).thenReturn(s4)

		when(userLookup.getUserByUserId("cuscav")).thenReturn(m1)
		when(userLookup.getUserByUserId("cusebr")).thenReturn(m2)
		when(userLookup.getUserByUserId("curef")).thenReturn(m3)
		when(userLookup.getUserByUserId("cusfal")).thenReturn(m4)

		assignment.userLookup = userLookup
	}

	trait MarkerMapFixture extends MarkingWorkflowFixture {

		val workflow: MarkingWorkflow with AssessmentMarkerMap

		assignment.firstMarkers.addAll(Seq(
			FirstMarkersMap(assignment, "cuscav", UserGroup.ofUsercodes),
			FirstMarkersMap(assignment, "cusebr", UserGroup.ofUsercodes)
		).asJava)

		assignment.firstMarkerMap.get("cuscav").get.addUserId("student1")
		assignment.firstMarkerMap.get("cuscav").get.addUserId("student2")

		assignment.secondMarkers.addAll(Seq(
			SecondMarkersMap(assignment, "curef", UserGroup.ofUsercodes),
			SecondMarkersMap(assignment, "cusfal", UserGroup.ofUsercodes)
		).asJava)

		assignment.secondMarkerMap.get("curef").get.addUserId("student4")

		// f1 and f2 aren't released to 2nd markers
		f3.secondMarkerFeedback = Fixtures.markerFeedback(f3)
		f4.secondMarkerFeedback = Fixtures.markerFeedback(f4)

		def setupWorkflow() {
			assignment.markingWorkflow = workflow

			workflow.firstMarkers.knownType.addUserId("cuscav")
			workflow.firstMarkers.knownType.addUserId("cusebr")

			workflow.secondMarkers.knownType.addUserId("curef")
			workflow.secondMarkers.knownType.addUserId("cusfal")

			workflow.userLookup = userLookup
			assignment.userLookup = userLookup
		}
	}

	@Test def studentsChooseMarker() = withUser("cuscav","0000001") { new MarkingWorkflowFixture {
		val workflow = new OldStudentsChooseMarkerWorkflow

		assignment.markingWorkflow = workflow

		workflow.getSubmissions(assignment, currentUser.apparentUser) should be (Seq())

		workflow.studentsChooseMarker should be {true}
		workflow.firstMarkerRoleName should be("Marker")
		workflow.hasSecondMarker should be {false}
		workflow.secondMarkerRoleName should be(None)
		workflow.secondMarkerVerb should be(None)

		workflow.getSubmissions(assignment, currentUser.apparentUser) should be (Seq())
		workflow.userLookup = userLookup

		val field = new MarkerSelectField
		field.name = Assignment.defaultMarkerSelectorName
		assignment.fields.add(field)

		workflow.submissionService = mock[SubmissionService]
		when(workflow.submissionService.getSubmissionByUsercode(assignment, f1.usercode)).thenReturn(Some(sub1))
		when(workflow.submissionService.getSubmissionByUsercode(assignment, f2.usercode)).thenReturn(Some(sub2))
		when(workflow.submissionService.getSubmissionByUsercode(assignment, f3.usercode)).thenReturn(Some(sub3))
		when(workflow.submissionService.getSubmissionByUsercode(assignment, f4.usercode)).thenReturn(Some(sub4))

		val valueMatches = new SavedFormValue()
		valueMatches.name = Assignment.defaultMarkerSelectorName
		valueMatches.value = "cuscav"

		val valueNotMatches = new SavedFormValue()
		valueNotMatches.name = Assignment.defaultMarkerSelectorName
		valueNotMatches.value = "cusebr"

		// f1 and f2 don't have the value
		sub3.values.add(valueNotMatches)
		sub4.values.add(valueMatches)

		workflow.getSubmissions(assignment, currentUser.apparentUser) should be (Seq(sub4))

		assignment.getStudentsFirstMarker(sub4.usercode) should be (Some(m1))
	}}

	@Test def seenSecondMarkingLegacy() = withUser("cuscav", "1234567") { new MarkingWorkflowFixture {
		val workflow = new SeenSecondMarkingLegacyWorkflow()
		assignment.markingWorkflow = workflow

		workflow.markingMethod should be(SeenSecondMarkingLegacy)
		workflow.courseworkMarkingUrl(assignment, currentUser.apparentUser, null) should be(s"/$cm1Prefix/admin/module/heron101/assignments/1/marker/1234567/feedback/online")

		workflow.firstMarkerRoleName should be("First marker")
		workflow.hasSecondMarker should be {true}
		workflow.secondMarkerRoleName should be(Some("Second marker"))
		workflow.secondMarkerVerb should be(Some("mark"))

	}}

	@Test def seenSecondMarking() = withUser("cuscao", "1234567") { new MarkingWorkflowFixture {
		val workflow = new SeenSecondMarkingWorkflow()
		assignment.markingWorkflow = workflow
		workflow.userLookup = userLookup

		workflow.markingMethod should be(SeenSecondMarking)
		workflow.courseworkMarkingUrl(assignment, currentUser.apparentUser, null) should be(s"/$cm1Prefix/admin/module/heron101/assignments/1/marker/1234567/feedback/online")

		workflow.firstMarkerRoleName should be("First marker")
		workflow.hasSecondMarker should  be {true}
		workflow.secondMarkerRoleName should be(Some("Second marker"))
		workflow.secondMarkerVerb should be(Some("mark"))

	}}

	@Test def moderatedMarking() = withUser("curef", "1234567") { new MarkingWorkflowFixture {

		val workflow = new ModeratedMarkingWorkflow
		workflow.userLookup = userLookup

		assignment.markingWorkflow = workflow
		assignment.secondMarkers = Seq(
			SecondMarkersMap(assignment, currentUser.apparentUser.getUserId, UserGroup.ofUsercodes)
		).asJava
		assignment.secondMarkerMap.get(currentUser.apparentUser.getUserId).get.addUserId(s4.getUserId)
		f4.secondMarkerFeedback = Fixtures.markerFeedback(f4)

		workflow.markingMethod should be(ModeratedMarking)
		workflow.courseworkMarkingUrl(assignment, currentUser.apparentUser, sub4.usercode) should be(s"/$cm1Prefix/admin/module/heron101/assignments/1/marker/1234567/feedback/online/moderation")

		workflow.firstMarkerRoleName should be("Marker")
		workflow.hasSecondMarker should  be {true}
		workflow.secondMarkerRoleName should be(Some("Moderator"))
		workflow.secondMarkerVerb should be(Some("moderate"))

	}}

	@Test def assignmentMarkerMap() { new MarkerMapFixture {

		val workflow = new SeenSecondMarkingLegacyWorkflow // this could be any workflow that extends AssignmentMarkerMap
		setupWorkflow()


		// cuscav is a first marker
		withUser("cuscav") {
			workflow.getSubmissions(assignment, currentUser.apparentUser) should be (Seq(sub2))
		}

		// curef is a second marker, but f3 isn't in their list
		withUser("curef") {
			workflow.getSubmissions(assignment, currentUser.apparentUser) should be (Seq(sub4))
		}

		// cusfal and cusebr are markers, but don't have any users in their list
		withUser("cusfal") { workflow.getSubmissions(assignment, currentUser.apparentUser) should be (Seq()) }
		withUser("cusebr") { workflow.getSubmissions(assignment, currentUser.apparentUser) should be (Seq()) }

		// Check transitivity. Submission1 isn't released but it's still ok to return the marker that it WILL be
		assignment.getStudentsFirstMarker(sub1.usercode) should be (Some(m1))
		assignment.getStudentsFirstMarker(sub2.usercode) should be (Some(m1))
		assignment.getStudentsFirstMarker(sub3.usercode) should be (None)
		assignment.getStudentsFirstMarker(sub4.usercode) should be (None)

		assignment.getStudentsSecondMarker(sub1.usercode) should be (None)
		assignment.getStudentsSecondMarker(sub2.usercode) should be (None)
		assignment.getStudentsSecondMarker(sub3.usercode) should be (None)
		assignment.getStudentsSecondMarker(sub4.usercode) should be (Some(m3))
	}}

	@Test def convertToObject() {
		val t = new MarkingMethodUserType
		t.convertToObject("StudentsChooseMarker") should be (MarkingMethod.StudentsChooseMarker)
		t.convertToObject("SeenSecondMarkingLegacy") should be (MarkingMethod.SeenSecondMarkingLegacy)
		an [IllegalArgumentException] should be thrownBy { t.convertToObject("Q") }
	}

	@Test def convertToValue() {
		val t = new MarkingMethodUserType
		t.convertToValue(MarkingMethod.StudentsChooseMarker) should be ("StudentsChooseMarker")
		t.convertToValue(MarkingMethod.SeenSecondMarkingLegacy) should be ("SeenSecondMarkingLegacy")
	}

}