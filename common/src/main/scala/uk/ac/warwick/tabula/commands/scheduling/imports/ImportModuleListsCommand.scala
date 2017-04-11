package uk.ac.warwick.tabula.commands.scheduling.imports

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{Daoisms, SessionComponent}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.scheduling.ModuleListImporter
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.services.exams.grids.UpstreamModuleListService

object ImportModuleListsCommand {
	def apply() = new ComposableCommandWithoutTransaction[Unit]
		with ImportModuleListsCommand
		with ImportModuleListsDescription
		with Daoisms
}


trait ImportModuleListsCommand extends CommandInternal[Unit]
	with RequiresPermissionsChecking with Logging with SessionComponent {

	def permissionsCheck(p:PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}

	var moduleListImporter: ModuleListImporter = Wire[ModuleListImporter]
	var upstreamModuleListService: UpstreamModuleListService = Wire[UpstreamModuleListService]

	val ImportGroupSize = 100

	def applyInternal() {
		benchmark("ImportModuleLists") {
			doLists()
			logger.debug("Imported lists. Importing entries...")
			doEntries()
		}
	}

	def doLists() {
		// Split into chunks so we commit transactions periodically.
		for (lists <- logSize(moduleListImporter.getModuleLists).grouped(ImportGroupSize)) {
			saveLists(lists)
			transactional() {
				session.flush()
			}
		}
	}

	private def saveLists(lists: Seq[UpstreamModuleList]) = transactional() {
		logger.debug(s"Importing ${lists.size} upstream module lists")
		for (list <- lists) {
			upstreamModuleListService.save(list)
		}
	}

	def doEntries() {
		// Split into chunks so we commit transactions periodically.
		val listCount = transactional(readOnly = true) { upstreamModuleListService.countAllModuleLists }
		(1 to listCount).grouped(ImportGroupSize).map(_.head).foreach(start => benchmark(s"Import entities for $ImportGroupSize lists") {
			val lists = transactional(readOnly = true) { upstreamModuleListService.listModuleLists(start, ImportGroupSize) }
			val entries = moduleListImporter.getModuleListEntries(lists)
			transactional() {
				entries.groupBy(_.list).foreach { case(list, listEntries) =>
					list.entries.clear()
					list.entries.addAll(JArrayList(listEntries))
					upstreamModuleListService.saveOrUpdate(list)
					session.flush()
				}
				entries.foreach(session.evict)
				lists.foreach(session.evict)
			}
		})
	}

}


trait ImportModuleListsDescription extends Describable[Unit] {
	def describe(d: Description) {}
}