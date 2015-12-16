package uk.ac.warwick.tabula.data.commands

import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.data.{AutowiringSmallGroupDaoComponent, SmallGroupDaoComponent}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

import scala.collection.JavaConverters._

class GroupMembershipFixtureCommand extends CommandInternal[SmallGroup] with Logging{
	this: UserLookupComponent with SmallGroupDaoComponent =>

	var groupSetId: String = _
	var groupName: String = _
	var userId: String = _

	protected def applyInternal() =
		transactional() {
			val user = userLookup.getUserByUserId(userId)
			val groupset = smallGroupDao.getSmallGroupSetById(groupSetId).get
			val group = groupset.groups.asScala.find(_.name == groupName).get
			group.students.add(user)
			logger.info(s"Added user $userId to group $groupName  in groupset $groupSetId")
			group
		}
}
object GroupMembershipFixtureCommand {
	def apply() = {
		new GroupMembershipFixtureCommand
			with ComposableCommand[SmallGroup]
			with AutowiringUserLookupComponent
			with AutowiringSmallGroupDaoComponent
			with Unaudited
			with PubliclyVisiblePermissions
	}
}
