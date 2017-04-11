package uk.ac.warwick.tabula.commands.profiles.relationships

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Department, StudentRelationship, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringRelationshipServiceComponent, RelationshipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ScheduledStudentRelationshipChangesCommand {
	def apply(department: Department, relationshipType: StudentRelationshipType) =
		new ScheduledStudentRelationshipChangesCommandInternal(department, relationshipType)
			with ComposableCommand[Map[DateTime, Seq[StudentRelationship]]]
			with AutowiringRelationshipServiceComponent
			with ScheduledStudentRelationshipChangesPermissions
			with ScheduledStudentRelationshipChangesCommandState
			with ScheduledStudentRelationshipChangesCommandRequest
			with ReadOnly with Unaudited
}


class ScheduledStudentRelationshipChangesCommandInternal(val department: Department, val relationshipType: StudentRelationshipType)
	extends CommandInternal[Map[DateTime, Seq[StudentRelationship]]] {

	self: RelationshipServiceComponent =>

	override def applyInternal(): Map[DateTime, Seq[StudentRelationship]] = {
		relationshipService.listScheduledRelationshipChanges(relationshipType, department).filter(_.replacedBy == null).groupBy(relationship =>
			if (relationship.startDate.isAfterNow) {
				relationship.startDate
			} else {
				relationship.endDate
			}
		)
	}

}

trait ScheduledStudentRelationshipChangesPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ScheduledStudentRelationshipChangesCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Read(mandatory(relationshipType)), department)
	}

}

trait ScheduledStudentRelationshipChangesCommandState {
	def department: Department
	def relationshipType: StudentRelationshipType
}

trait ScheduledStudentRelationshipChangesCommandRequest {
}
