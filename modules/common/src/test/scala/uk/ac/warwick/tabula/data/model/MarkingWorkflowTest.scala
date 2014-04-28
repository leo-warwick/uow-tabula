package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.{Mockito, TestBase, Fixtures}
import uk.ac.warwick.tabula.data.model.forms.{SavedFormValue, MarkerSelectField}
import collection.JavaConverters._
import uk.ac.warwick.tabula.services.{UserLookupService, SubmissionService}
import org.mockito.Mockito._
import uk.ac.warwick.tabula.data.model.MarkingMethod.{SeenSecondMarking, ModeratedMarking, SeenSecondMarkingLegacy}

class MarkingWorkflowTest extends TestBase with Mockito {


	trait MarkingWorkflowFixture {

		val module = Fixtures.module("heron101")
		val assignment = Fixtures.assignment("my assignment")
		assignment.id = "1"
		assignment.module = module

		val sub1 = Fixtures.submission(universityId="0000001", userId="student1")
		val sub2 = Fixtures.submission(universityId="0000002", userId="student2")
		val sub3 = Fixtures.submission(universityId="0000003", userId="student3")
		val sub4 = Fixtures.submission(universityId="0000004", userId="student4")

		val f1 = Fixtures.feedback(universityId="0000001")
		val f2 = Fixtures.feedback(universityId="0000002")
		val f3 = Fixtures.feedback(universityId="0000003")
		val f4 = Fixtures.feedback(universityId="0000004")

		assignment.submissions.addAll(Seq(sub1, sub2, sub3, sub4).toList.asJava)
		assignment.submissions.asScala.toList foreach { _.assignment = assignment }
		assignment.feedbacks.addAll(Seq(f1, f2, f3, f4).toList.asJava)

		// f1 isn't released yet
		f2.firstMarkerFeedback = Fixtures.markerFeedback(f2)
		f3.firstMarkerFeedback = Fixtures.markerFeedback(f3)
		f4.firstMarkerFeedback = Fixtures.markerFeedback(f4)
	}

	trait MarkerMapFixture extends MarkingWorkflowFixture {

		val workflow: MarkingWorkflow with AssignmentMarkerMap

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

		val s1 = Fixtures.user(universityId="0000001", userId="student1")
		val s2 = Fixtures.user(universityId="0000002", userId="student2")
		val s3 = Fixtures.user(universityId="0000003", userId="student3")
		val s4 = Fixtures.user(universityId="0000004", userId="student4")

		// f1 and f2 aren't released to 2nd markers
		f3.secondMarkerFeedback = Fixtures.markerFeedback(f3)
		f4.secondMarkerFeedback = Fixtures.markerFeedback(f4)

		def setupWorkflow() {
			assignment.markingWorkflow = workflow

			workflow.firstMarkers.addUserId("cuscav")
			workflow.firstMarkers.addUserId("cusebr")

			workflow.secondMarkers.addUserId("curef")
			workflow.secondMarkers.addUserId("cusfal")

			workflow.userLookup = mock[UserLookupService]
			when(workflow.userLookup.getUserByWarwickUniId("0000001")).thenReturn(s1)
			when(workflow.userLookup.getUserByWarwickUniId("0000002")).thenReturn(s2)
			when(workflow.userLookup.getUserByWarwickUniId("0000003")).thenReturn(s3)
			when(workflow.userLookup.getUserByWarwickUniId("0000004")).thenReturn(s4)
		}
	}

	@Test def studentsChooseMarker() = withUser("cuscav") { new MarkingWorkflowFixture {
		val workflow = new StudentsChooseMarkerWorkflow

		assignment.markingWorkflow = workflow

		workflow.getSubmissions(assignment, currentUser.apparentUser) should be (Seq())

		workflow.studentsChooseMarker should be (true)
		workflow.firstMarkerRoleName should be("Marker")
		workflow.hasSecondMarker should be(false)
		workflow.secondMarkerRoleName should be(None)
		workflow.secondMarkerVerb should be(None)

		workflow.getSubmissions(assignment, currentUser.apparentUser) should be (Seq())

		val field = new MarkerSelectField
		field.name = Assignment.defaultMarkerSelectorName
		assignment.fields.add(field)

		workflow.submissionService = mock[SubmissionService]
		when(workflow.submissionService.getSubmissionByUniId(assignment, f1.universityId)).thenReturn(Some(sub1))
		when(workflow.submissionService.getSubmissionByUniId(assignment, f2.universityId)).thenReturn(Some(sub2))
		when(workflow.submissionService.getSubmissionByUniId(assignment, f3.universityId)).thenReturn(Some(sub3))
		when(workflow.submissionService.getSubmissionByUniId(assignment, f4.universityId)).thenReturn(Some(sub4))

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

		assignment.getStudentsFirstMarker(sub4) should be (Some("cuscav"))
	}}

	@Test def seenSecondMarkingLegacy() = withUser("cuscav") { new MarkingWorkflowFixture {
		val workflow = new SeenSecondMarkingLegacyWorkflow()
		assignment.markingWorkflow = workflow

		workflow.markingMethod should be(SeenSecondMarkingLegacy)
		workflow.onlineMarkingUrl(assignment, currentUser.apparentUser, null) should be("/coursework/admin/module/heron101/assignments/1/marker/feedback/online")

		workflow.firstMarkerRoleName should be("First marker")
		workflow.hasSecondMarker should be(true)
		workflow.secondMarkerRoleName should be(Some("Second marker"))
		workflow.secondMarkerVerb should be(Some("mark"))

	}}

	@Test def seenSecondMarking() = withUser("cuscao") { new MarkingWorkflowFixture {
		val workflow = new SeenSecondMarkingWorkflow()
		assignment.markingWorkflow = workflow

		workflow.markingMethod should be(SeenSecondMarking)
		workflow.onlineMarkingUrl(assignment, currentUser.apparentUser, null) should be("/coursework/admin/module/heron101/assignments/1/marker/feedback/online")

		workflow.firstMarkerRoleName should be("First marker")
		workflow.hasSecondMarker should be(true)
		workflow.secondMarkerRoleName should be(Some("Second marker"))
		workflow.secondMarkerVerb should be(Some("mark"))

	}}

	@Test def moderatedMarking() = withUser("cuscav") { new MarkingWorkflowFixture {
		val workflow = new ModeratedMarkingWorkflow
		assignment.markingWorkflow = workflow

		workflow.markingMethod should be(ModeratedMarking)
		workflow.onlineMarkingUrl(assignment, currentUser.apparentUser, null) should be("/coursework/admin/module/heron101/assignments/1/marker/feedback/online/moderation")

		workflow.firstMarkerRoleName should be("Marker")
		workflow.hasSecondMarker should be(true)
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
		assignment.getStudentsFirstMarker(sub1) should be (Some("cuscav"))
		assignment.getStudentsFirstMarker(sub2) should be (Some("cuscav"))
		assignment.getStudentsFirstMarker(sub3) should be (None)
		assignment.getStudentsFirstMarker(sub4) should be (None)

		assignment.getStudentsSecondMarker(sub1) should be (None)
		assignment.getStudentsSecondMarker(sub2) should be (None)
		assignment.getStudentsSecondMarker(sub3) should be (None)
		assignment.getStudentsSecondMarker(sub4) should be (Some("curef"))
	}}

	@Test def convertToObject() {
		val t = new MarkingMethodUserType
		t.convertToObject("StudentsChooseMarker") should be (MarkingMethod.StudentsChooseMarker)
		t.convertToObject("SeenSecondMarkingLegacy") should be (MarkingMethod.SeenSecondMarkingLegacy)
		evaluating { t.convertToObject("Q") } should produce [IllegalArgumentException]
	}

	@Test def convertToValue() {
		val t = new MarkingMethodUserType
		t.convertToValue(MarkingMethod.StudentsChooseMarker) should be ("StudentsChooseMarker")
		t.convertToValue(MarkingMethod.SeenSecondMarkingLegacy) should be ("SeenSecondMarkingLegacy")
	}

}