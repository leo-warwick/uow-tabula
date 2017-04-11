package uk.ac.warwick.tabula.commands.sysadmin

import uk.ac.warwick.tabula.services.RelationshipServiceComponent
import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.services.AutowiringRelationshipServiceComponent
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.commands.ComposableCommand

object ListStudentRelationshipTypesCommand {
	def apply() =
		new ListStudentRelationshipTypesCommandInternal
			with ComposableCommand[Seq[StudentRelationshipType]]
			with AutowiringRelationshipServiceComponent
			with ListStudentRelationshipTypesCommandPermissions
			with Unaudited
}

class ListStudentRelationshipTypesCommandInternal extends CommandInternal[Seq[StudentRelationshipType]] {
	this: RelationshipServiceComponent =>

	override def applyInternal(): Seq[StudentRelationshipType] = relationshipService.allStudentRelationshipTypes
}

trait ListStudentRelationshipTypesCommandPermissions extends RequiresPermissionsChecking {
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.StudentRelationshipType.Read)
	}
}