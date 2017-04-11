package uk.ac.warwick.tabula.commands.sysadmin

import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.Permissions

import uk.ac.warwick.tabula.system.permissions.{PermissionsCheckingMethods, PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.turnitinlti._

import uk.ac.warwick.tabula.helpers.StringUtils._

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.helpers.Logging
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.Assignment

object LtiConformanceTesterPopulateFormCommand {
	def apply(user: CurrentUser) =
		new LtiConformanceTesterPopulateFormCommandInternal(user)
			with LtiConformanceTesterPopulateFormCommandPermissions
			with ComposableCommand[Map[String, String]]
			with ReadOnly with Unaudited
			with LtiConformanceTesterPopulateFormCommandState
			with LtiConformanceTesterPopulateFormValidation
			with AutowiringTurnitinLtiServiceComponent
			with Logging
}

class LtiConformanceTesterPopulateFormCommandInternal(val user: CurrentUser) extends CommandInternal[Map[String, String]] {

	self: LtiConformanceTesterPopulateFormCommandState with TurnitinLtiServiceComponent with Logging =>

	override def applyInternal(): Map[String, String] = transactional() {
		turnitinLtiService.ltiConformanceTestParams(
			endpoint, secret, key, givenName, familyName, email, role, mentee, customParams, tool_consumer_info_version, assignment, user
		)
	}

}

trait LtiConformanceTesterPopulateFormCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}
}

trait LtiConformanceTesterPopulateFormValidation extends SelfValidating {
	self: LtiConformanceTesterPopulateFormCommandState =>

	override def validate(errors: Errors) {
		if (endpoint.isEmptyOrWhitespace) {
			errors.rejectValue("endpoint", "turnitin.turnitinassignment.empty", "Please specify an endpoint")
		}
	}
}

trait LtiConformanceTesterPopulateFormCommandState {
	var endpoint: String = _
	var secret: String = _
	var key: String = _
	var givenName: String = _
	var familyName: String = _
	var email: String = _
	var role: String = _
	var mentee: String = _
	var customParams: String = _
	var tool_consumer_info_version: String = _
	var assignment: Assignment = _
}