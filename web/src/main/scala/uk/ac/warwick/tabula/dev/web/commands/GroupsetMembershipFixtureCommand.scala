package uk.ac.warwick.tabula.dev.web.commands

import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.data.{AutowiringSmallGroupDaoComponent, Daoisms, SessionComponent, SmallGroupDaoComponent}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

class GroupsetMembershipFixtureCommand extends CommandInternal[SmallGroupSet] with Logging{
	this: UserLookupComponent with SmallGroupDaoComponent with SessionComponent =>

	var groupSetId: String = _
	var userId: String = _

	protected def applyInternal(): SmallGroupSet =
		transactional() {
			val user = userLookup.getUserByUserId(userId)
			val groupset = smallGroupDao.getSmallGroupSetById(groupSetId).get
			if (groupset.members.isEmpty){
				groupset.members = UserGroup.ofUsercodes // have to use usercodes in fixtures because test users cant be looked
			}		                                         // up by university ID
			groupset.members.add(user)
			logger.info(s"Added user $userId to groupset $groupSetId")
			groupset
		}
}

object GroupsetMembershipFixtureCommand{
	def apply(): GroupsetMembershipFixtureCommand with ComposableCommand[SmallGroupSet] with AutowiringUserLookupComponent with AutowiringSmallGroupDaoComponent with Daoisms with Unaudited with PubliclyVisiblePermissions = {
		new GroupsetMembershipFixtureCommand
			with ComposableCommand[SmallGroupSet]
			with AutowiringUserLookupComponent
			with AutowiringSmallGroupDaoComponent
			with Daoisms
			with Unaudited
			with PubliclyVisiblePermissions
	}
}
