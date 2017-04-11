package uk.ac.warwick.tabula.commands.coursework.markingworkflows
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.MarkingMethod.{SeenSecondMarking, FirstMarkerOnly, ModeratedMarking, StudentsChooseMarker, SeenSecondMarkingLegacy}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.services.MarkingWorkflowServiceComponent
import uk.ac.warwick.tabula.services.AutowiringMarkingWorkflowServiceComponent

object OldAddMarkingWorkflowCommand {
	def apply(department: Department) =
		new AddMarkingWorkflowCommandInternal(department)
			with ComposableCommand[MarkingWorkflow]
			with AddMarkingWorkflowCommandPermissions
			with AddMarkingWorkflowCommandValidation
			with AddMarkingWorkflowCommandDescription
			with AutowiringMarkingWorkflowServiceComponent
}

class AddMarkingWorkflowCommandInternal(department: Department) extends ModifyMarkingWorkflowCommand(department) {
	self: MarkingWorkflowServiceComponent =>

	// Copy properties to a new MarkingWorkflow, save it transactionally, return it.
	def applyInternal(): MarkingWorkflow = {
		transactional() {
			val markingWorkflow = markingMethod match {
				case SeenSecondMarkingLegacy => new SeenSecondMarkingLegacyWorkflow(department)
				case SeenSecondMarking => new SeenSecondMarkingWorkflow(department)
				case StudentsChooseMarker => new OldStudentsChooseMarkerWorkflow(department)
				case ModeratedMarking => new ModeratedMarkingWorkflow(department)
				case FirstMarkerOnly => new FirstMarkerOnlyWorkflow(department)
				case _ => throw new UnsupportedOperationException(markingMethod + " not specified")
			}
			this.copyTo(markingWorkflow)
			markingWorkflowService.save(markingWorkflow)
			markingWorkflow
		}
	}
}

trait AddMarkingWorkflowCommandValidation extends MarkingWorkflowCommandValidation {
	self: MarkingWorkflowCommandState =>

	// For validation. Not editing an existing MarkingWorkflow so return None
	def currentMarkingWorkflow = None

	def contextSpecificValidation(errors:Errors){}
}

trait AddMarkingWorkflowCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: MarkingWorkflowCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MarkingWorkflow.Manage, mandatory(department))
	}
}

trait AddMarkingWorkflowCommandDescription extends Describable[MarkingWorkflow] {
	self: MarkingWorkflowCommandState =>

	def describe(d: Description) {
		d.department(department)
	}
}