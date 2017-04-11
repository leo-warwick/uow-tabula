package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.permissions.{Permission, PermissionsSelector}
import uk.ac.warwick.tabula.JavaImports

object StudentRelationshipAgent {

	final def profileReadOnlyPermissions(relationshipType: PermissionsSelector[StudentRelationshipType]) = Seq(
		Profiles.Read.Core,
		Profiles.Read.Photo,
		Profiles.Read.Usercode,
		Profiles.Read.Timetable,

		Profiles.Read.Disability, // TAB-4386

		Profiles.Read.StudentCourseDetails.Core,
		Profiles.Read.StudentCourseDetails.Status,
		Profiles.StudentRelationship.Read(PermissionsSelector.Any[StudentRelationshipType]), // Can read any relationship type for this student

		Profiles.MeetingRecord.Read(relationshipType),
		Profiles.MeetingRecord.ReadDetails(relationshipType),

		SmallGroups.Read,
		Profiles.Read.SmallGroups,
		SmallGroupEvents.ViewRegister,
		Profiles.Read.Coursework,
		Profiles.Read.AccreditedPriorLearning,

		Profiles.Read.ModuleRegistration.Core,
		Profiles.Read.ModuleRegistration.Results,

		MemberNotes.Read,

		MonitoringPoints.View,

		// Can read Coursework info for student
		Submission.Read,
		AssignmentFeedback.Read,
		ExamFeedback.Read,
		Extension.Read
	)

}

/**
 * Granted this role when a member is the agent in a current relationship with an active student
 */
case class StudentRelationshipAgent(student: model.Member, relationshipType: StudentRelationshipType)
	extends BuiltInRole(StudentRelationshipAgentRoleDefinition(relationshipType), student)

case class StudentRelationshipAgentRoleDefinition(relationshipType: PermissionsSelector[StudentRelationshipType])
	extends SelectorBuiltInRoleDefinition(relationshipType) {

	override def description: String =
		if (relationshipType.isWildcard) "Relationship agent (any relationship)"
		else relationshipType.description

	val readOnlyPermissions: Seq[Permission with Product with Serializable] = StudentRelationshipAgent.profileReadOnlyPermissions(relationshipType)
	val permissionsForCurrentRelationships = Seq (
		Profiles.Read.NextOfKin,
		Profiles.Read.HomeAndTermTimeAddresses,
		Profiles.Read.TelephoneNumber,
		Profiles.Read.MobileNumber,

		Profiles.MeetingRecord.Manage(relationshipType),

		Profiles.ScheduledMeetingRecord.Manage(relationshipType),

		MemberNotes.Create,

		MonitoringPoints.Record
	)

	GrantsScopedPermission(
		readOnlyPermissions ++ permissionsForCurrentRelationships :_*
	)
	final def canDelegateThisRolesPermissions: JavaImports.JBoolean = true

	def duplicate(selectorOption: Option[PermissionsSelector[StudentRelationshipType]]): SelectorBuiltInRoleDefinition[StudentRelationshipType] =
		selectorOption.map{ selector =>
			StudentRelationshipAgentRoleDefinition(selector)
		}.getOrElse(
			StudentRelationshipAgentRoleDefinition(relationshipType)
		)

}

/**
 * Granted this role when a member has had a relationship with the student at some point. The student doesn't need
 * to be active for this role to be granted.
 */
case class HistoricStudentRelationshipAgent(student: model.Member, relationshipType: StudentRelationshipType)
	extends BuiltInRole(HistoricStudentRelationshipAgentRoleDefinition(relationshipType), student)

case class HistoricStudentRelationshipAgentRoleDefinition(relationshipType: PermissionsSelector[StudentRelationshipType])
	extends SelectorBuiltInRoleDefinition(relationshipType) {

	override def description: String =
		if (relationshipType.isWildcard) "Previous relationship agent (any relationship)"
		else s"Previous ${relationshipType.description}"

	GrantsScopedPermission(
		StudentRelationshipAgent.profileReadOnlyPermissions(relationshipType):_*
	)
	final def canDelegateThisRolesPermissions: JavaImports.JBoolean = true

	def duplicate(selectorOption: Option[PermissionsSelector[StudentRelationshipType]]): SelectorBuiltInRoleDefinition[StudentRelationshipType] =
		selectorOption.map{ selector =>
			HistoricStudentRelationshipAgentRoleDefinition(selector)
		}.getOrElse(
			HistoricStudentRelationshipAgentRoleDefinition(relationshipType)
		)

}