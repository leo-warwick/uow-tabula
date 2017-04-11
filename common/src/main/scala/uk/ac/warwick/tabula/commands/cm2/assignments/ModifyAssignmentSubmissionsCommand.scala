package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ModifyAssignmentSubmissionsCommand {
	def apply(assignment: Assignment) =
		new ModifyAssignmentSubmissionsCommandInternal(assignment)
			with ComposableCommand[Assignment]
			with ModifyAssignmentSubmissionsPermissions
			with ModifyAssignmentSubmissionsDescription
			with ModifyAssignmentSubmissionsCommandState
			with AutowiringAssessmentServiceComponent
			with SharedAssignmentProperties
}

class ModifyAssignmentSubmissionsCommandInternal(override val assignment: Assignment)
	extends CommandInternal[Assignment] with PopulateOnForm {

	self: AssessmentServiceComponent with ModifyAssignmentSubmissionsCommandState with SharedAssignmentProperties =>


	override def applyInternal(): Assignment = {
		this.copyTo(assignment)
		assessmentService.save(assignment)
		assignment
	}

	override def populate(): Unit = {
		copySharedFrom(assignment)
	}

}

trait ModifyAssignmentSubmissionsCommandState {

	self: AssessmentServiceComponent with SharedAssignmentProperties =>

	def assignment: Assignment

	def module: Module = assignment.module

	def copyTo(assignment: Assignment) {
		assignment.collectSubmissions = collectSubmissions
		assignment.automaticallySubmitToTurnitin = automaticallySubmitToTurnitin
		assignment.displayPlagiarismNotice = displayPlagiarismNotice
		assignment.restrictSubmissions = restrictSubmissions
		assignment.allowResubmission = allowResubmission
		assignment.allowLateSubmissions = allowLateSubmissions
		assignment.allowExtensions = allowExtensions
		assignment.extensionAttachmentMandatory = extensionAttachmentMandatory
		assignment.allowExtensionsAfterCloseDate = allowExtensionsAfterCloseDate
	}

}


trait ModifyAssignmentSubmissionsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ModifyAssignmentSubmissionsCommandState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.Assignment.Update, assignment.module)
	}
}


trait ModifyAssignmentSubmissionsDescription extends Describable[Assignment] {
	self: ModifyAssignmentSubmissionsCommandState with SharedAssignmentProperties =>

	override def describe(d: Description) {
		d.assignment(assignment)
		d.properties(
			"collectSubmissions" -> collectSubmissions,
			"automaticallySubmitToTurnitin" -> automaticallySubmitToTurnitin,
			"displayPlagiarismNotice" -> displayPlagiarismNotice,
			"restrictSubmissions" -> restrictSubmissions,
			"allowResubmission" -> allowResubmission,
			"allowLateSubmissions" -> allowLateSubmissions,
			"allowExtensions" -> allowExtensions,
			"extensionAttachmentMandatory" -> extensionAttachmentMandatory,
			"allowExtensionsAfterCloseDate" -> allowExtensionsAfterCloseDate
		)
	}
}



