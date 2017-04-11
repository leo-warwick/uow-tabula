package uk.ac.warwick.tabula.commands.groups.admin.reusable

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.{DepartmentSmallGroup, DepartmentSmallGroupSet}
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent, AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._

object UpdateStudentsForDepartmentSmallGroupSetCommand {
	def apply(department: Department, set: DepartmentSmallGroupSet) =
		new UpdateStudentsForDepartmentSmallGroupSetCommandInternal(department, set)
			with ComposableCommand[DepartmentSmallGroupSet]
			with UpdateStudentsForDepartmentSmallGroupSetPermissions
			with UpdateStudentsForDepartmentSmallGroupSetDescription
			with RemovesUsersFromDepartmentGroupsCommand
			with AutowiringUserLookupComponent
			with AutowiringSmallGroupServiceComponent
}

trait UpdateStudentsForDepartmentSmallGroupSetCommandFactory {
	def apply(department: Department, set: DepartmentSmallGroupSet): Appliable[DepartmentSmallGroupSet] with UpdateStudentsForDepartmentSmallGroupSetCommandState
}

object UpdateStudentsForDepartmentSmallGroupSetCommandFactoryImpl
	extends UpdateStudentsForDepartmentSmallGroupSetCommandFactory {

	def apply(department: Department, set: DepartmentSmallGroupSet) =
		UpdateStudentsForDepartmentSmallGroupSetCommand(department, set)
}

class UpdateStudentsForDepartmentSmallGroupSetCommandInternal(val department: Department, val set: DepartmentSmallGroupSet)
	extends CommandInternal[DepartmentSmallGroupSet] with UpdateStudentsForDepartmentSmallGroupSetCommandState {
	self: UserLookupComponent with SmallGroupServiceComponent with RemovesUsersFromDepartmentGroups =>

	override def applyInternal(): DepartmentSmallGroupSet = {
		val autoDeregister = set.department.autoGroupDeregistration

		val oldUsers =
			if (autoDeregister) set.members.users.toSet
			else Set[User]()

		if (linkToSits) {
			set.members.knownType.staticUserIds = staticStudentIds.asScala
			set.members.knownType.includedUserIds = includedStudentIds.asScala
			set.members.knownType.excludedUserIds = excludedStudentIds.asScala
			set.memberQuery = filterQueryString
		} else {
			set.members.knownType.staticUserIds = Seq()
			set.members.knownType.excludedUserIds = Seq()
			set.memberQuery = ""
			set.members.knownType.includedUserIds = ((staticStudentIds.asScala diff excludedStudentIds.asScala) ++ includedStudentIds.asScala).distinct
		}

		val newUsers =
			if (autoDeregister) set.members.users.toSet
			else Set[User]()

		// TAB-1561
		if (autoDeregister) {
			for {
				user <- oldUsers -- newUsers
				group <- set.groups.asScala
				if group.students.includesUser(user)
			} removeFromGroup(user, group)
		}

		smallGroupService.saveOrUpdate(set)

		set
	}
}

trait UpdateStudentsForDepartmentSmallGroupSetCommandState {
	self: UserLookupComponent =>

	def department: Department
	def set: DepartmentSmallGroupSet

	def membershipItems: Seq[DepartmentSmallGroupSetMembershipItem] = {
		def toMembershipItem(universityId: String, itemType: DepartmentSmallGroupSetMembershipItemType) = {
			val user = userLookup.getUserByWarwickUniId(universityId)
			DepartmentSmallGroupSetMembershipItem(itemType, user.getFirstName, user.getLastName, user.getWarwickId, user.getUserId)
		}

		val staticMemberItems =
			((staticStudentIds.asScala diff excludedStudentIds.asScala) diff includedStudentIds.asScala)
				.map(toMembershipItem(_, DepartmentSmallGroupSetMembershipStaticType))

		val includedMemberItems = includedStudentIds.asScala.map(toMembershipItem(_, DepartmentSmallGroupSetMembershipIncludeType))

		(staticMemberItems ++ includedMemberItems).sortBy(membershipItem => (membershipItem.lastName, membershipItem.firstName))
	}

	// Bind variables

	var includedStudentIds: JList[String] = LazyLists.create()
	var excludedStudentIds: JList[String] = LazyLists.create()
	var staticStudentIds: JList[String] = LazyLists.create()
	var filterQueryString: String = ""
	var linkToSits = true
}

trait UpdateStudentsForDepartmentSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: UpdateStudentsForDepartmentSmallGroupSetCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(set, department)
		p.PermissionCheck(Permissions.SmallGroups.Update, set)
	}

}

trait UpdateStudentsForDepartmentSmallGroupSetDescription extends Describable[DepartmentSmallGroupSet] {
	self: UpdateStudentsForDepartmentSmallGroupSetCommandState =>

	override lazy val eventName = "UpdateStudentsForDepartmentSmallGroupSet"

	override def describe(d: Description) {
		d.properties("smallGroupSet" -> set.id)
	}
}

trait RemovesUsersFromDepartmentGroups {
	def removeFromGroup(user: User, group: DepartmentSmallGroup)
}

trait RemovesUsersFromDepartmentGroupsCommand extends RemovesUsersFromDepartmentGroups {
	def removeFromGroup(user: User, group: DepartmentSmallGroup): Unit = new RemoveUserFromDepartmentSmallGroupCommand(user, group).apply()
}