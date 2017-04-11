package uk.ac.warwick.tabula.commands.coursework.feedback

import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringAssessmentServiceComponent, AssessmentServiceComponent}

object GenericFeedbackCommand {
	def apply(module: Module, assignment: Assignment) =
		new GenericFeedbackCommand(module, assignment)
			with ComposableCommand[Assignment]
			with AutowiringAssessmentServiceComponent
			with GenericFeedbackPermissions
			with GenericFeedbackFormDescription[Assignment] {
			override lazy val eventName = "GenericFeedback"
		}
}

abstract class GenericFeedbackCommand (val module: Module, val assignment: Assignment)
	extends CommandInternal[Assignment] with Appliable[Assignment] with GenericFeedbackState {
	this: AssessmentServiceComponent =>

	genericFeedback = assignment.genericFeedback

	def applyInternal() : Assignment = {
		assignment.genericFeedback = genericFeedback
		assessmentService.save(assignment)
		assignment
	}
}

trait GenericFeedbackPermissions extends RequiresPermissionsChecking {
	this: GenericFeedbackState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(assignment, module)
		p.PermissionCheck(Permissions.AssignmentFeedback.Manage, assignment)
	}
}

trait GenericFeedbackState {
	val assignment: Assignment
	val module: Module
	var genericFeedback: String = _
}

trait GenericFeedbackFormDescription[A] extends Describable[A] {
	this: GenericFeedbackState  =>
	def describe(d: Description) {
		d.assignment(assignment)
	}
}