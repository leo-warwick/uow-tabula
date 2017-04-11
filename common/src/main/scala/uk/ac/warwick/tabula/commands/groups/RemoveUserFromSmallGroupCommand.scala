package uk.ac.warwick.tabula.commands.groups

import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.UnspecifiedTypeUserGroup

class RemoveUserFromSmallGroupCommand(val user: User, val group: SmallGroup) extends Command[UnspecifiedTypeUserGroup] {

	PermissionCheck(Permissions.SmallGroups.Update, group)

	def applyInternal(): UnspecifiedTypeUserGroup = {
		val ug = group.students
		ug.remove(user)
		ug
	}

	override def describe(d: Description): Unit =
		d.smallGroup(group).properties(
			"usercode" -> user.getUserId,
			"universityId" -> user.getWarwickId
		)

}