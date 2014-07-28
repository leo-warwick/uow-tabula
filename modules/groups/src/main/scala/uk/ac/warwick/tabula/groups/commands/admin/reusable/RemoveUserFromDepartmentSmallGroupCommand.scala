package uk.ac.warwick.tabula.groups.commands.admin.reusable

import uk.ac.warwick.tabula.commands.{Description, Command}
import uk.ac.warwick.tabula.data.model.UnspecifiedTypeUserGroup
import uk.ac.warwick.tabula.data.model.groups.{DepartmentSmallGroup, SmallGroup}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.userlookup.User

class RemoveUserFromDepartmentSmallGroupCommand(val user: User, val group: DepartmentSmallGroup) extends Command[UnspecifiedTypeUserGroup] {
	
	PermissionCheck(Permissions.SmallGroups.Update, group)
	
	def applyInternal() = {
		val ug = group.students
		ug.remove(user)
		ug
	}

	override def describe(d: Description) = 
		d.departmentSmallGroup(group).properties(
			"usercode" -> user.getUserId,
			"universityId" -> user.getWarwickId
		)

}