package uk.ac.warwick.tabula.commands.sysadmin

import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.AutowiringRelationshipServiceComponent

import uk.ac.warwick.tabula.services.RelationshipServiceComponent
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking

object AddStudentRelationshipTypeCommand {
	def apply() =
		new AddStudentRelationshipTypeCommandInternal
			with ComposableCommand[StudentRelationshipType]
			with AutowiringRelationshipServiceComponent
			with AddStudentRelationshipTypeCommandPermissions
			with ModifyStudentRelationshipTypeCommandDescription
}

class AddStudentRelationshipTypeCommandInternal extends ModifyStudentRelationshipTypeCommandInternal {
	this: RelationshipServiceComponent =>

	override def applyInternal(): StudentRelationshipType = transactional() {
		val tpe = new StudentRelationshipType
		copyTo(tpe)
		relationshipService.saveOrUpdate(tpe)
		tpe
	}
}

trait AddStudentRelationshipTypeCommandPermissions extends RequiresPermissionsChecking {
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.StudentRelationshipType.Manage)
	}
}