package uk.ac.warwick.tabula.commands.groups.admin.reusable

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ModifyDepartmentSmallGroupSetCommand {
	def create(department: Department, academicYear: AcademicYear) =
		new CreateDepartmentSmallGroupSetCommandInternal(department, academicYear)
			with ComposableCommand[DepartmentSmallGroupSet]
			with ModifyDepartmentSmallGroupSetCommandValidation
			with CreateDepartmentSmallGroupSetPermissions
			with CreateDepartmentSmallGroupSetDescription
			with AutowiringSmallGroupServiceComponent

	def edit(department: Department, academicYear: AcademicYear, set: DepartmentSmallGroupSet) =
		new EditDepartmentSmallGroupSetCommandInternal(department, academicYear, set)
			with ComposableCommand[DepartmentSmallGroupSet]
			with ModifyDepartmentSmallGroupSetCommandValidation
			with EditDepartmentSmallGroupSetPermissions
			with EditDepartmentSmallGroupSetDescription
			with AutowiringSmallGroupServiceComponent
}

trait ModifyDepartmentSmallGroupSetState {
	def department: Department
	def academicYear: AcademicYear
	def existingSet: Option[DepartmentSmallGroupSet]

	var name: String = _
}

trait CreateDepartmentSmallGroupSetCommandState extends ModifyDepartmentSmallGroupSetState {
	val existingSet = None
}

class CreateDepartmentSmallGroupSetCommandInternal(val department: Department, val academicYear: AcademicYear)
	extends ModifyDepartmentSmallGroupSetCommandInternal with CreateDepartmentSmallGroupSetCommandState {

	self: SmallGroupServiceComponent =>

	def applyInternal(): DepartmentSmallGroupSet = transactional() {
		val set = new DepartmentSmallGroupSet(department)
		set.academicYear = academicYear
		copyTo(set)

		smallGroupService.saveOrUpdate(set)
		set
	}

}

trait EditDepartmentSmallGroupSetCommandState extends ModifyDepartmentSmallGroupSetState {
	def smallGroupSet: DepartmentSmallGroupSet
	lazy val existingSet = Some(smallGroupSet)
}

class EditDepartmentSmallGroupSetCommandInternal(val department: Department, val academicYear: AcademicYear, val smallGroupSet: DepartmentSmallGroupSet)
	extends ModifyDepartmentSmallGroupSetCommandInternal with EditDepartmentSmallGroupSetCommandState {

	self: SmallGroupServiceComponent =>

	copyFrom(smallGroupSet)

	def applyInternal(): DepartmentSmallGroupSet = transactional() {
		copyTo(smallGroupSet)

		smallGroupService.saveOrUpdate(smallGroupSet)
		smallGroupSet
	}

}

abstract class ModifyDepartmentSmallGroupSetCommandInternal
	extends CommandInternal[DepartmentSmallGroupSet] with ModifyDepartmentSmallGroupSetState {

	def copyFrom(set: DepartmentSmallGroupSet) {
		name = set.name
	}

	def copyTo(set: DepartmentSmallGroupSet) {
		set.name = name

		if (set.members == null) set.members = UserGroup.ofUniversityIds
	}
}

trait ModifyDepartmentSmallGroupSetCommandValidation extends SelfValidating {
	self: ModifyDepartmentSmallGroupSetState =>

	override def validate(errors: Errors) {
		if (!name.hasText) errors.rejectValue("name", "smallGroupSet.name.NotEmpty")
		else if (name.orEmpty.length > 200) errors.rejectValue("name", "smallGroupSet.name.Length", Array[Object](200: JInteger), "")
	}
}

trait CreateDepartmentSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: CreateDepartmentSmallGroupSetCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroups.Create, mandatory(department))
	}
}

trait CreateDepartmentSmallGroupSetDescription extends Describable[DepartmentSmallGroupSet] {
	self: CreateDepartmentSmallGroupSetCommandState =>

	override def describe(d: Description) {
		d.department(department).properties("name" -> name)
	}

	override def describeResult(d: Description, set: DepartmentSmallGroupSet): Unit =
		d.department(set.department).properties("smallGroupSet" -> set.id)
}

trait EditDepartmentSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditDepartmentSmallGroupSetCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(smallGroupSet, department)
		p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(smallGroupSet))
	}
}

trait EditDepartmentSmallGroupSetDescription extends Describable[DepartmentSmallGroupSet] {
	self: EditDepartmentSmallGroupSetCommandState =>

	override def describe(d: Description) {
		d.department(smallGroupSet.department).properties("smallGroupSet" -> smallGroupSet.id)
	}

}