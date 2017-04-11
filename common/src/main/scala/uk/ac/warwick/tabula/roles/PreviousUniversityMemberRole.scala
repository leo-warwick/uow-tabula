package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.permissions.PermissionsSelector
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

case class PreviousUniversityMemberRole(member: model.Member) extends BuiltInRole(PreviousUniversityMemberRoleDefinition, member)

case object PreviousUniversityMemberRoleDefinition extends UnassignableBuiltInRoleDefinition {

	override def description = "Previous University Member"

	GrantsScopedPermission(
		Profiles.Read.Core,
		Profiles.Read.Photo,
		Profiles.Read.NextOfKin,
		Profiles.Read.HomeAndTermTimeAddresses,
		Profiles.Read.TelephoneNumber,
		Profiles.Read.MobileNumber,
		Profiles.Read.Usercode,
		Profiles.Read.StudentCourseDetails.Core,
		Profiles.Read.StudentCourseDetails.Status,
		Profiles.Read.SmallGroups,
		Profiles.Read.Coursework,
		Profiles.Read.Timetable,
		Profiles.Read.TimetablePrivateFeed,
		Profiles.Read.Disability,

		MemberNotes.Read,
		Profiles.Read.ModuleRegistration.Core,
		Profiles.Read.ModuleRegistration.Results,

    Profiles.Read.RelationshipStudents(PermissionsSelector.Any[StudentRelationshipType]),

    Profiles.StudentRelationship.Read(PermissionsSelector.Any[StudentRelationshipType]),

    Profiles.MeetingRecord.Read(PermissionsSelector.Any[StudentRelationshipType]),
    Profiles.MeetingRecord.ReadDetails(PermissionsSelector.Any[StudentRelationshipType]),

		MonitoringPoints.View,
		SmallGroupEvents.ViewRegister,

		// Can read own coursework info
		Submission.Read,
		AssignmentFeedback.Read,
		Extension.Read
	)

	GrantsScopelessPermission(
		UserPicker
	)
}
