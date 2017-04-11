package uk.ac.warwick.tabula.commands.scheduling.imports

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.{Daoisms, SessionComponent}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.exams.grids.UpstreamRouteRuleService
import uk.ac.warwick.tabula.services.scheduling.RouteRuleImporter
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}

object ImportRouteRulesCommand {
	def apply() = new ComposableCommandWithoutTransaction[Unit]
		with ImportRouteRulesCommand
		with ImportRouteRulesDescription
		with Daoisms
}


trait ImportRouteRulesCommand extends CommandInternal[Unit]
	with RequiresPermissionsChecking with Logging with SessionComponent {

	def permissionsCheck(p:PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}

	var routeRuleImporter: RouteRuleImporter = Wire[RouteRuleImporter]
	var upstreamRouteRuleService: UpstreamRouteRuleService = Wire[UpstreamRouteRuleService]

	val ImportGroupSize = 100

	def applyInternal() {
		benchmark("ImportRouteRules") {
			// Get the new rules first, split into chunks
			val newRules = logSize(routeRuleImporter.getRouteRules).grouped(ImportGroupSize)
			// Dump all the old ones
			transactional() {
				upstreamRouteRuleService.removeAll()
				session.flush()
			}
			// Commit transactions periodically.
			for (rules <- newRules) { transactional() {
				rules.foreach(upstreamRouteRuleService.saveOrUpdate)
				session.flush()
			}}
		}
	}

}


trait ImportRouteRulesDescription extends Describable[Unit] {
	def describe(d: Description) {}
}