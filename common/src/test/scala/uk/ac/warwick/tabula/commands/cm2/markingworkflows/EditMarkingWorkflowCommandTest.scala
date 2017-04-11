package uk.ac.warwick.tabula.commands.cm2.markingworkflows

import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.commands.ValidatorHelpers
import uk.ac.warwick.tabula.data.model.markingworkflow.SingleMarkerWorkflow
import uk.ac.warwick.tabula.services.{CM2MarkingWorkflowService, UserLookupComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}


class EditMarkingWorkflowCommandTest extends TestBase with Mockito with ValidatorHelpers {

	trait Fixture {

		val marker1 = Fixtures.user("1170836", "cuslaj")
		val marker2 = Fixtures.user("1170837", "cuslak")
		val marker3 = Fixtures.user("1170838", "cuslal")

		val userlookupService = Fixtures.userLookupService(marker1, marker2, marker3)
		val workflowService = smartMock[CM2MarkingWorkflowService]
		val dept = Fixtures.department("in")
		val wflow = SingleMarkerWorkflow("workflow1", dept, Seq(marker1))
		wflow.id = "workflow1"
		wflow.academicYear = AcademicYear(2016)

		val validator = new EditMarkingWorkflowValidation with EditMarkingWorkflowState with UserLookupComponent {
			val userLookup = userlookupService
			val department = dept
			val academicYear = AcademicYear(2016)
			val isResuable = true
			val workflow = wflow
			markersA = JArrayList("cuslaj", "cuslak")
			markersB = JArrayList()
		}
	}

	@Test
	def validateDupeNames(): Unit = new Fixture {

		val wflow2 = SingleMarkerWorkflow("workflow2", dept, Seq(marker1))
		wflow2.id = "workflow2"
		wflow.academicYear = AcademicYear(2016)
		dept.addCM2MarkingWorkflow(wflow)
		dept.addCM2MarkingWorkflow(wflow2)

		validator.name = "workflow2"
		hasError(validator, "name")

		validator.name = "workflow1"
		hasNoError(validator, "name")

		validator.name = "workflow2"
		wflow2.academicYear = AcademicYear(2015)
		hasNoError(validator, "name")
	}

}