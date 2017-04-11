package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.data.model.forms.{SavedFormValue, MarkerSelectField}
import uk.ac.warwick.tabula.{Fixtures, TestBase, RequestInfo}
import uk.ac.warwick.tabula.JavaImports._
import collection.JavaConverters._

class SubmissionsForMarkerTest extends TestBase {

	@Test def markersSubmissionsTest() {

		val assignment = new Assignment
		assignment.addDefaultFields()
		assignment.markingWorkflow = newMarkingWorkflow()

		val markerSelect = new MarkerSelectField()
		markerSelect.name = Assignment.defaultMarkerSelectorName
		assignment.addFields(markerSelect)

		val values1 = Set(
			submissionValue(Assignment.defaultCommentFieldName, "comment"),
			submissionValue(Assignment.defaultUploadName, "junk"),
			submissionValue(Assignment.defaultMarkerSelectorName, "cuslaj")
		)

		val values2 = Set(
			submissionValue(Assignment.defaultCommentFieldName, "comment"),
			submissionValue(Assignment.defaultUploadName, "junk"),
			submissionValue(Assignment.defaultMarkerSelectorName, "cuslaj")
		)

		val values3 = Set(
			submissionValue(Assignment.defaultCommentFieldName, "comment"),
			submissionValue(Assignment.defaultUploadName, "junk"),
			submissionValue(Assignment.defaultMarkerSelectorName, "cusebr")
		)

		assignment.submissions = JArrayList(
			newSubmission(assignment, values1),
			newSubmission(assignment, values2),
			newSubmission(assignment, values3)
		)
		releaseAllSubmissions(assignment)

		withUser(code = "cusebr", universityId = "0678022") {
			val user = RequestInfo.fromThread.get.user
			val submissions = assignment.getMarkersSubmissions(user.apparentUser)
			submissions.size should be (1)
		}

		withUser(code = "cuslaj", universityId = "1170836") {
			val user = RequestInfo.fromThread.get.user
			val submissions = assignment.getMarkersSubmissions(user.apparentUser)
			submissions.size should be (2)
		}

	}


	def submissionValue(name: String, value: String): SavedFormValue = {
		val sv = new SavedFormValue()
		sv.name = name
		sv.value = value
		sv
	}

	def releaseAllSubmissions(assignment: Assignment){
		assignment.submissions.asScala.foreach{ s =>
			val newFeedback = Fixtures.assignmentFeedback(s._universityId)
			newFeedback.usercode = s.usercode
			newFeedback.assignment = assignment
			newFeedback.uploaderId = "test"
			newFeedback.released = false
			val markerFeedback = new MarkerFeedback(newFeedback)
			newFeedback.firstMarkerFeedback = markerFeedback
			markerFeedback.state = MarkingState.ReleasedForMarking
			assignment.feedbacks.add(newFeedback)
		}
	}

	def newMarkingWorkflow(): MarkingWorkflow = {
		val ug = UserGroup.ofUsercodes
		ug.addUserId("cuslaj")
		ug.addUserId("cusebr")

		val ms = new OldStudentsChooseMarkerWorkflow
		ms.name = "Test marking workflow"
		ms.firstMarkers = ug
		ms
	}

	def newSubmission(a: Assignment, values: Set[SavedFormValue]): Submission = {
		val s = new Submission
		s.assignment = a
		s.values.addAll(values.asJava)
		s
	}

}
