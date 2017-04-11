package uk.ac.warwick.tabula.commands.profiles

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Department, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringMeetingRecordServiceComponent, AutowiringRelationshipServiceComponent, MeetingRecordServiceComponent, RelationshipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.immutable.TreeMap

object ViewUnconfirmedMeetingRecordsCommand {
	def apply(department: Department, relationshipType: StudentRelationshipType) =
		new ViewUnconfirmedMeetingRecordsCommandInternal(department, relationshipType)
			with AutowiringRelationshipServiceComponent
			with AutowiringMeetingRecordServiceComponent
			with ComposableCommand[Map[String, Int]]
			with ViewUnconfirmedMeetingRecordsPermissions
			with ViewUnconfirmedMeetingRecordsCommandState
			with ReadOnly with Unaudited
}


class ViewUnconfirmedMeetingRecordsCommandInternal(val department: Department, val relationshipType: StudentRelationshipType)
	extends CommandInternal[Map[String, Int]] {

	self: RelationshipServiceComponent with MeetingRecordServiceComponent =>

	override def applyInternal(): TreeMap[String, Int] = {
		val relationshipsByAgent = relationshipService.listAgentRelationshipsByDepartment(relationshipType, department)
		val allRelationships = relationshipsByAgent.values.flatten.toSeq
		val counts = meetingRecordService.unconfirmedScheduledCount(allRelationships)
		relationshipsByAgent.map { case (_, relationships) => relationships.head.agentName -> relationships.map(counts.getOrElse(_, 0)).sum }
	}
}

trait ViewUnconfirmedMeetingRecordsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ViewUnconfirmedMeetingRecordsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Read(relationshipType), department)
	}

}

trait ViewUnconfirmedMeetingRecordsCommandState {
	def department: Department
	def relationshipType: StudentRelationshipType
}
