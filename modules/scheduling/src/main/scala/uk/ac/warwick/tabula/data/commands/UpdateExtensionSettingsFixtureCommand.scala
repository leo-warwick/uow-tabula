package uk.ac.warwick.tabula.data.commands

import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

class UpdateExtensionSettingsFixtureCommand extends CommandInternal[Department] {
	this: ModuleAndDepartmentServiceComponent with TransactionalComponent =>

	var departmentCode: String = _
	var allow: Boolean = _
	var userId: String = _

	protected def applyInternal() = {
		transactional() {
			val dept = moduleAndDepartmentService.getDepartmentByCode(departmentCode).get
			if (allow) {
				dept.allowExtensionRequests = true
				dept.extensionGuidelineLink = "http://warwick.ac.uk/"
				dept.extensionGuidelineSummary = "Don't ask, don't get."
				dept.extensionManagers.knownType.addUserId(userId)
			} else {
				dept.allowExtensionRequests = false
				dept.extensionGuidelineLink = ""
				dept.extensionGuidelineSummary = ""
				// can only remove the manager if specified
				if (!userId.isEmpty) dept.extensionManagers.knownType.removeUserId(userId)
			}
			dept
		}
	}
}

object UpdateExtensionSettingsFixtureCommand {
	def apply() = {
		new UpdateExtensionSettingsFixtureCommand
			with ComposableCommand[Department]
			with AutowiringModuleAndDepartmentServiceComponent
			with AutowiringTransactionalComponent
			with PubliclyVisiblePermissions
			with Unaudited
	}
}
