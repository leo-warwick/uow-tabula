package uk.ac.warwick.tabula.commands.profiles.relationships

import uk.ac.warwick.tabula.commands.{ReadOnly, Unaudited, _}
import uk.ac.warwick.tabula.data.model.{Department, Member, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, AutowiringRelationshipServiceComponent, ProfileServiceComponent, RelationshipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object MissingStudentRelationshipsCommand {
	def apply(department: Department, relationshipType: StudentRelationshipType) =
		new MissingStudentRelationshipsCommandInternal(department, relationshipType)
			with ComposableCommand[(Int, Seq[Member])]
			with AutowiringProfileServiceComponent
			with AutowiringRelationshipServiceComponent
			with MissingStudentRelationshipsPermissions
			with MissingStudentRelationshipsCommandState
			with Unaudited with ReadOnly
}


class MissingStudentRelationshipsCommandInternal(val department: Department, val relationshipType: StudentRelationshipType)
	extends CommandInternal[(Int, Seq[Member])] {

	self: ProfileServiceComponent with RelationshipServiceComponent =>

	override def applyInternal(): (Int, Seq[Member]) = {
		profileService.countStudentsByDepartment(department) match {
			case 0 => (0, Nil)
			case c => (c, relationshipService.listStudentsWithoutCurrentRelationship(relationshipType, department))
		}
	}

}

trait MissingStudentRelationshipsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: MissingStudentRelationshipsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Read(mandatory(relationshipType)), department)
	}

}

trait MissingStudentRelationshipsCommandState {
	def department: Department
	def relationshipType: StudentRelationshipType
}
