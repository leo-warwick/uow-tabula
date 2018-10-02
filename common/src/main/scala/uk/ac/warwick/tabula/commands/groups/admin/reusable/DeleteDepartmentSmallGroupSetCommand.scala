package uk.ac.warwick.tabula.commands.groups.admin.reusable

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.{DepartmentSmallGroupSet, SmallGroupAllocationMethod}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import scala.collection.JavaConverters._

object DeleteDepartmentSmallGroupSetCommand {
	def apply(department: Department, set: DepartmentSmallGroupSet) =
		new DeleteDepartmentSmallGroupSetCommandInternal(department, set)
			with ComposableCommand[DepartmentSmallGroupSet]
			with DeleteDepartmentSmallGroupSetValidation
			with DeleteDepartmentSmallGroupSetPermissions
			with DeleteDepartmentSmallGroupSetDescription
			with AutowiringSmallGroupServiceComponent
}

trait DeleteDepartmentSmallGroupSetCommandState {
	def department: Department
	def set: DepartmentSmallGroupSet

	var confirm = false
}

class DeleteDepartmentSmallGroupSetCommandInternal(val department: Department, val set: DepartmentSmallGroupSet)
	extends CommandInternal[DepartmentSmallGroupSet] with DeleteDepartmentSmallGroupSetCommandState {
	self: SmallGroupServiceComponent =>

	override def applyInternal(): DepartmentSmallGroupSet = transactional() {
		set.markDeleted()
		smallGroupService.saveOrUpdate(set)
		set
	}
}

trait DeleteDepartmentSmallGroupSetValidation extends SelfValidating {
	self: DeleteDepartmentSmallGroupSetCommandState =>

	override def validate(errors: Errors) {
		if (!confirm) {
			errors.rejectValue("confirm", "smallGroupSet.delete.confirm")
		} else {
			validateCanDelete(errors)
		}
	}

	def validateCanDelete(errors: Errors) {
		if (set.deleted) {
			errors.reject("smallGroupSet.delete.deleted")
		} else if (set.linkedSets.asScala.exists { set => set.allocationMethod ==  SmallGroupAllocationMethod.StudentSignUp  &&  !set.canBeDeleted }) {
			errors.reject("smallGroupSet.delete.studentSignUpReleased")
		} else if (set.linkedSets.asScala.exists { set => set.allocationMethod !=  SmallGroupAllocationMethod.StudentSignUp &&  !set.canBeDeleted }) {
			errors.reject("smallGroupSet.delete.released")
		}
	}
}

trait DeleteDepartmentSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DeleteDepartmentSmallGroupSetCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(set, department)
		p.PermissionCheck(Permissions.SmallGroups.Delete, mandatory(set))
	}
}

trait DeleteDepartmentSmallGroupSetDescription extends Describable[DepartmentSmallGroupSet] {
	self: DeleteDepartmentSmallGroupSetCommandState =>

	override def describe(d: Description) {
		d.department(set.department).properties("smallGroupSet" -> set.id)
	}

}