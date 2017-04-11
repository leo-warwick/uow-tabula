package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.permissions.PermissionsSelector

case class ScheduledMeetingRecordCreator(meeting: model.ScheduledMeetingRecord, relationshipType: StudentRelationshipType) extends BuiltInRole(ScheduledMeetingRecordCreatorRoleDefinition(relationshipType), meeting)

case class ScheduledMeetingRecordCreatorRoleDefinition(relationshipType: PermissionsSelector[StudentRelationshipType]) extends SelectorBuiltInRoleDefinition(relationshipType) with UnassignableBuiltInRoleDefinition {
	override def description = "Scheduled Meeting Record Creator"

	GrantsScopedPermission(
		Profiles.ScheduledMeetingRecord.Manage(relationshipType),
		Profiles.ScheduledMeetingRecord.Confirm
	)

	def duplicate(selectorOption: Option[PermissionsSelector[StudentRelationshipType]]): SelectorBuiltInRoleDefinition[StudentRelationshipType] =
		selectorOption.map{ selector =>
			ScheduledMeetingRecordCreatorRoleDefinition(selector)
		}.getOrElse(
			ScheduledMeetingRecordCreatorRoleDefinition(relationshipType)
		)
}
