package uk.ac.warwick.tabula.commands.cm2.markingworkflows

import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.commands.ValidatorHelpers
import uk.ac.warwick.tabula.data.model.markingworkflow.SingleMarkerWorkflow
import uk.ac.warwick.tabula.services.UserLookupComponent
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

// scalastyle:off public.methods.have.type
// scalastyle:off public.property.type.annotation

class ReplaceMarkerCommandTest extends TestBase with Mockito with ValidatorHelpers {

	trait Fixture {

		val marker1 = Fixtures.user("1170836", "cuslaj")
		val marker2 = Fixtures.user("1170837", "cuslak")
		val marker3 = Fixtures.user("1170838", "cuslal")

		val userlookupService = Fixtures.userLookupService(marker1, marker2, marker3)
		val dept = Fixtures.department("in")

		val wflow = SingleMarkerWorkflow("workflow1", dept, Seq(marker1))
		wflow.id = "workflow1"
		wflow.academicYear = AcademicYear(2016)

		val a1 = Fixtures.assignment("a1")
		a1.cm2MarkingWorkflow = wflow
		val a2 = Fixtures.assignment("a2")
		a2.cm2MarkingWorkflow = wflow
		wflow.assignments = JArrayList(a1, a2)

		val f1 = Fixtures.assignmentFeedback()
		val mf1 = Fixtures.markerFeedback(f1)
		mf1.marker = marker1
		mf1.userLookup = userlookupService
		f1.released = true
		a1.feedbacks = JArrayList(f1)

		val f2 = Fixtures.assignmentFeedback()
		val mf2 = Fixtures.markerFeedback(f2)
		mf2.userLookup = userlookupService
		mf2.marker = marker2
		a2.feedbacks = JArrayList(f2)

		val validator = new ReplaceMarkerValidation with ReplaceMarkerState with UserLookupComponent {
			val userLookup = userlookupService
			val department = dept
			val markingWorkflow = wflow

			override lazy val allMarkers = Seq(marker1, marker2, marker3)
		}
	}


	@Test
	def testState(): Unit = new Fixture {

		val state = new ReplaceMarkerState with UserLookupComponent {
			val department = dept
			val markingWorkflow = wflow
			val userLookup = userlookupService
		}
		state.oldMarker = "cuslaj"
		state.newMarker = "cuslak"

		state.finishedAssignments should be (Set(a1))
	}

	@Test
	def testOldMarkerValidation(): Unit = new Fixture {
		hasError(validator, "oldMarker")
		validator.oldMarker = "dsgfeg"
		hasError(validator, "oldMarker")
		validator.oldMarker = "cuslaj"
		hasNoError(validator, "oldMarker")
	}


	@Test
	def testNewMarkerValidation(): Unit = new Fixture {
		hasError(validator, "newMarker")
		validator.newMarker = "dsgfeg"
		hasError(validator, "newMarker")
		validator.newMarker = "cuslak"
		hasNoError(validator, "newMarker")
	}

	@Test
	def testConfirmValidation(): Unit = new Fixture {
		hasError(validator, "confirm")
		validator.confirm = true
		hasNoError(validator, "confirm")
	}

}