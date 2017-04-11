package uk.ac.warwick.tabula.commands.groups.admin.reusable

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

abstract class DepartmentSmallGroupSetMembershipItemType(val value: String)
case object DepartmentSmallGroupSetMembershipStaticType extends DepartmentSmallGroupSetMembershipItemType("static")
case object DepartmentSmallGroupSetMembershipIncludeType extends DepartmentSmallGroupSetMembershipItemType("include")
case object DepartmentSmallGroupSetMembershipExcludeType extends DepartmentSmallGroupSetMembershipItemType("exclude")

/**
 * Item in list of members for displaying in view.
 */
case class DepartmentSmallGroupSetMembershipItem(
	itemType: DepartmentSmallGroupSetMembershipItemType, // static, include or exclude
	firstName: String,
	lastName: String,
	universityId: String,
	userId: String
) {
	def itemTypeString: String = itemType.value
}

object ListDepartmentSmallGroupSetsCommand {
	def apply(department: Department, academicYear: AcademicYear) =
		new ListDepartmentSmallGroupSetsCommandInternal(department, academicYear)
			with ComposableCommand[Seq[DepartmentSmallGroupSet]]
			with ListDepartmentSmallGroupSetsPermissions
			with AutowiringSmallGroupServiceComponent
			with ReadOnly with Unaudited
}

class ListDepartmentSmallGroupSetsCommandInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[Seq[DepartmentSmallGroupSet]] with ListDepartmentSmallGroupSetsCommandState {
	self: SmallGroupServiceComponent =>

	def applyInternal(): Seq[DepartmentSmallGroupSet] = smallGroupService.getDepartmentSmallGroupSets(department, academicYear)
}

trait ListDepartmentSmallGroupSetsCommandState {
	def department: Department
	def academicYear: AcademicYear
}

trait ListDepartmentSmallGroupSetsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ListDepartmentSmallGroupSetsCommandState =>

	def permissionsCheck(p:PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroups.Create, mandatory(department))
	}
}