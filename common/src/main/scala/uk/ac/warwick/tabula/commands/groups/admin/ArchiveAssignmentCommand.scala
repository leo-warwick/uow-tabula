package uk.ac.warwick.tabula.commands.groups.admin

import uk.ac.warwick.spring.Wire

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.SmallGroupService

/** Simply marks a set as archived. */
class ArchiveSmallGroupSetCommand(val module: Module, val set: SmallGroupSet) extends Command[SmallGroupSet] {

	mustBeLinked(set, module)
	PermissionCheck(Permissions.SmallGroups.Archive, set)

	var service: SmallGroupService = Wire[SmallGroupService]

	var unarchive = false

	def applyInternal(): SmallGroupSet = transactional() {
		set.archived = !unarchive
		service.saveOrUpdate(set)
		set
	}

	def describe(description: Description): Unit = description
		.smallGroupSet(set)
		.property("unarchive" -> unarchive)

}