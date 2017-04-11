package uk.ac.warwick.tabula.commands.sysadmin

import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.Permissions

import uk.ac.warwick.tabula.system.permissions.{PermissionsCheckingMethods, PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.turnitinlti._

import uk.ac.warwick.tabula.helpers.StringUtils._

import uk.ac.warwick.tabula.CurrentUser
import org.springframework.validation.Errors

object TurnitinLtiSubmissionDetailsCommand {
	def apply(user: CurrentUser) =
		new TurnitinLtiSubmissionDetailsCommandInternal(user)
			with TurnitinLtiSubmissionDetailsCommandPermissions
			with ComposableCommand[TurnitinLtiResponse]
			with ReadOnly with Unaudited
			with TurnitinLtiSubmissionDetailsCommandState
			with TurnitinLtiSubmissionDetailsValidation
			with AutowiringTurnitinLtiServiceComponent
}

class TurnitinLtiSubmissionDetailsCommandInternal(val user: CurrentUser) extends CommandInternal[TurnitinLtiResponse] {

	self: TurnitinLtiSubmissionDetailsCommandState with TurnitinLtiServiceComponent =>

	override def applyInternal(): TurnitinLtiResponse = transactional() {
		turnitinLtiService.getSubmissionDetails(turnitinSubmissionId, user)
	}

}

trait TurnitinLtiSubmissionDetailsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}
}

trait TurnitinLtiSubmissionDetailsValidation extends SelfValidating {
	self: TurnitinLtiSubmissionDetailsCommandState =>

	override def validate(errors: Errors) {
		if (turnitinSubmissionId.isEmptyOrWhitespace) {
			errors.rejectValue("turnitinSubmissionId", "turnitin.submission.empty")
		}
	}
}

trait TurnitinLtiSubmissionDetailsCommandState {
	var turnitinSubmissionId: String = _
}