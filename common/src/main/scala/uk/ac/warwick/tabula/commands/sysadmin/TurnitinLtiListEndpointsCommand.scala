package uk.ac.warwick.tabula.commands.sysadmin

import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.Permissions

import uk.ac.warwick.tabula.system.permissions.{PermissionsCheckingMethods, PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.turnitinlti._

import uk.ac.warwick.tabula.helpers.StringUtils._

import uk.ac.warwick.tabula.CurrentUser
import org.springframework.validation.Errors

object TurnitinLtiListEndpointsCommand {
	def apply(user: CurrentUser) =
		new TurnitinLtiListEndpointsCommandInternal(user)
			with TurnitinLtiListEndpointsCommandPermissions
			with ComposableCommand[TurnitinLtiResponse]
			with ReadOnly with Unaudited
			with TurnitinLtiListEndpointsCommandState
			with TurnitinLtiListEndpointsValidation
			with AutowiringTurnitinLtiServiceComponent
}

class TurnitinLtiListEndpointsCommandInternal(val user: CurrentUser) extends CommandInternal[TurnitinLtiResponse] {

	self: TurnitinLtiListEndpointsCommandState with TurnitinLtiServiceComponent =>

	override def applyInternal(): TurnitinLtiResponse = transactional() {
		turnitinLtiService.listEndpoints(turnitinAssignmentId, user)
	}

}

trait TurnitinLtiListEndpointsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}
}

trait TurnitinLtiListEndpointsValidation extends SelfValidating {
	self: TurnitinLtiListEndpointsCommandState =>

	override def validate(errors: Errors) {
		if (turnitinAssignmentId.isEmptyOrWhitespace) {
			errors.rejectValue("turnitinAssignmentId", "turnitin.turnitinassignment.empty")
		}
	}
}

trait TurnitinLtiListEndpointsCommandState {
	var turnitinAssignmentId: String = _
}