package uk.ac.warwick.tabula.commands.sysadmin

import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.Permissions

import uk.ac.warwick.tabula.system.permissions.{PermissionsCheckingMethods, PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.turnitinlti._

import uk.ac.warwick.tabula.CurrentUser
import org.springframework.validation.Errors

object TurnitinLtiSubmitAssignmentCommand {
	def apply(user: CurrentUser) =
		new TurnitinLtiSubmitAssignmentCommandInternal(user)
			with TurnitinLtiSubmitAssignmentCommandPermissions
			with ComposableCommand[TurnitinLtiResponse]
			with ReadOnly with Unaudited
			with TurnitinLtiSubmitAssignmentCommandState
			with TurnitinLtiSubmitAssignmentValidation
			with AutowiringTurnitinLtiServiceComponent
}

class TurnitinLtiSubmitAssignmentCommandInternal(val user: CurrentUser) extends CommandInternal[TurnitinLtiResponse] {

	self: TurnitinLtiSubmitAssignmentCommandState with TurnitinLtiServiceComponent =>

	override def applyInternal(): TurnitinLtiResponse = transactional() {
		turnitinLtiService.submitAssignment(assignment, user)
	}

}

trait TurnitinLtiSubmitAssignmentCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}
}

trait TurnitinLtiSubmitAssignmentValidation extends SelfValidating {
	self: TurnitinLtiSubmitAssignmentCommandState =>

	override def validate(errors: Errors) {
		if (null == assignment) {
			errors.rejectValue("assignment", "turnitin.assignment.empty")
		}
	}
}

trait TurnitinLtiSubmitAssignmentCommandState {
	var assignment: Assignment = _
}