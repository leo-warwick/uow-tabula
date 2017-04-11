package uk.ac.warwick.tabula.commands.cm2.markingworkflows


import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.ValidatorHelpers
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType.{DoubleMarking, SingleMarking}
import uk.ac.warwick.tabula.data.model.markingworkflow.SingleMarkerWorkflow
import uk.ac.warwick.tabula.services.{CM2MarkingWorkflowService, CM2MarkingWorkflowServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}



class AddMarkingWorkflowCommandTest extends TestBase with Mockito with ValidatorHelpers {

	trait Fixture {

		val marker1 = Fixtures.user("1170836", "cuslaj")
		val marker2 = Fixtures.user("1170837", "cuslak")
		val marker3 = Fixtures.user("1170838", "cuslal")
		val marker4 = Fixtures.user("1170839", "cuslam")
		val userlookupService = Fixtures.userLookupService(marker1, marker2, marker3, marker4)
		val workflowService = smartMock[CM2MarkingWorkflowService]
		val dept = Fixtures.department("in")


		val validator = new AddMarkingWorkflowValidation with AddMarkingWorkflowState with UserLookupComponent {
			val userLookup = userlookupService
			val department = dept
			val academicYear = AcademicYear(2016)
			val isResuable = true

			markersA = JArrayList()
			markersB = JArrayList()
		}

		val cmd = new AddMarkingWorkflowCommandInternal(dept, AcademicYear(2016), isResuable=true) with AddMarkingWorkflowState with CM2MarkingWorkflowServiceComponent with UserLookupComponent {
			val userLookup = userlookupService
			val cm2MarkingWorkflowService = workflowService
			name = "name"
			markersA = JArrayList(marker1.getUserId, marker2.getUserId)
			markersB = JArrayList(marker3.getUserId)
		}
	}

	@Test
	def validateNullWorkflowType(): Unit = {
		new Fixture {
			hasError(validator, "workflowType")
			validator.workflowType = SingleMarking
			hasNoError(validator, "workflowType")
		}
	}

	@Test
	def validateEmptyName(): Unit = {
		new Fixture {
			validator.workflowType = SingleMarking
			hasError(validator, "name")
			validator.name = ""
			hasError(validator, "name")
			validator.name = "a name"
			hasNoError(validator, "name")
		}
	}

	@Test
	def validateMarkerA(): Unit = {
		new Fixture {
			validator.workflowType = SingleMarking
			validator.name = "a name"
			hasError(validator, "markersA")

			validator.markersA = JArrayList("notauser")
			hasError(validator, "markersA")

			validator.markersA = JArrayList("cuslaj", "cuslaj")
			hasError(validator, "markersA")

			validator.markersA = JArrayList("cuslaj", "cuslak")
			hasNoError(validator, "markersA")
		}
	}

	@Test
	def validateMarkerB(): Unit = {
		new Fixture {
			validator.workflowType = SingleMarking
			validator.name = "a name"
			validator.markersA = JArrayList("cuslaj", "cuslak")
			hasNoError(validator, "markersB")

			validator.workflowType = DoubleMarking
			hasError(validator, "markersB")

			validator.markersB = JArrayList("notauser")
			hasError(validator, "markersB")

			validator.markersB = JArrayList("cuslaj", "cuslaj")
			hasError(validator, "markersB")

			validator.markersB = JArrayList("cuslaj", "cuslak")
			hasNoError(validator, "markersB")
		}
	}

	@Test
	def validateDupeNames(): Unit = new Fixture {

		val wflow = SingleMarkerWorkflow("name", dept, Seq(marker1))
		wflow.academicYear = AcademicYear(2016)
		dept.addCM2MarkingWorkflow(wflow)

		validator.name = "name"
		hasError(validator, "name")

		wflow.academicYear = AcademicYear(2015)
		hasNoError(validator, "name")
	}

	@Test
	def applyTest(): Unit = new Fixture {
		cmd.workflowType = SingleMarking
		var wflow = cmd.applyInternal()
		wflow.workflowType should be (SingleMarking)
		wflow.name should be (cmd.name)
		wflow.department should be (cmd.department)
		wflow.academicYear should be (cmd.academicYear)
		wflow.workflowType should be (SingleMarking)
		verify(workflowService, times(1)).save(wflow)
	}


}