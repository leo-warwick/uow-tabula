package uk.ac.warwick.tabula.commands.cm2.markingworkflows

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType._
import uk.ac.warwick.tabula.data.model.markingworkflow._
import uk.ac.warwick.tabula.helpers.StringUtils
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.services.{AutoWiringCM2MarkingWorkflowServiceComponent, AutowiringUserLookupComponent, CM2MarkingWorkflowServiceComponent, UserLookupComponent}

import scala.collection.JavaConverters._

object AddMarkingWorkflowCommand {

	def apply(department:Department, academicYear: AcademicYear, isResuable: Boolean) =
		new AddMarkingWorkflowCommandInternal(department, academicYear, isResuable)
			with ComposableCommand[CM2MarkingWorkflow]
			with AddMarkingWorkflowValidation
			with MarkingWorkflowDepartmentPermissions
			with ModifyMarkingWorkflowDescription
			with AddMarkingWorkflowState
			with AutoWiringCM2MarkingWorkflowServiceComponent
			with AutowiringUserLookupComponent
}

class AddMarkingWorkflowCommandInternal(
	val department: Department,
	val academicYear: AcademicYear,
	val isResuable: Boolean) extends CommandInternal[CM2MarkingWorkflow] {

	self: AddMarkingWorkflowState with CM2MarkingWorkflowServiceComponent with UserLookupComponent =>

	markersA = JArrayList()
	markersB = JArrayList()

	def applyInternal(): CM2MarkingWorkflow = {

		val markersAUsers = userLookup.getUsersByUserIds(markersA.asScala).values.toSeq
		val markersBUsers = userLookup.getUsersByUserIds(markersB.asScala).values.toSeq

		val workflow = workflowType match {
			case StudentChoosesGroupMarking => StudentsChooseGroupWorkflow(name, department, markersAUsers)
			case DoubleMarking => DoubleWorkflow(name, department, markersAUsers, markersBUsers)
			case ModeratedMarking => ModeratedWorkflow(name, department, markersAUsers, markersBUsers)
			case SingleMarking => SingleMarkerWorkflow(name, department, markersAUsers)
			case DoubleBlindMarking => DoubleBlindWorkflow(name, department, markersAUsers, markersBUsers)
			case _ => throw new UnsupportedOperationException(workflowType + " not specified")
		}
		workflow.isReusable = isResuable
		cm2MarkingWorkflowService.save(workflow)
		workflow
	}
}

trait AddMarkingWorkflowValidation extends ModifyMarkingWorkflowValidation with StringUtils {

	self: AddMarkingWorkflowState with UserLookupComponent =>

	def validate(errors: Errors) {

		if (department.cm2MarkingWorkflows.exists(w => w.academicYear == academicYear && w.name == name )) {
			errors.rejectValue("name", "name.duplicate.markingWorkflow", Array(name), null)
		}

		if (workflowType == null)
			errors.rejectValue("workflowType", "markingWorkflow.workflowType.none")
		else
			genericValidate(errors, workflowType)
	}
}


trait AddMarkingWorkflowState extends ModifyMarkingWorkflowState {
	this: UserLookupComponent =>
	def isResuable: Boolean
	var workflowType: MarkingWorkflowType = _
}