package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.JavaImports
import uk.ac.warwick.tabula.permissions.PermissionsSelector
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

case class RouteAuditor(route: model.Route) extends BuiltInRole(RouteAuditorRoleDefinition, route)

case object RouteAuditorRoleDefinition extends BuiltInRoleDefinition {

	override def description = "Route Auditor"

	GrantsScopedPermission(
		Route.Administer,

		MonitoringPoints.View,

		Profiles.Read.Core,
		Profiles.Read.Photo,
		Profiles.Read.NextOfKin,
	  Profiles.Read.Timetable,
		Profiles.Read.StudentCourseDetails.Core,
		Profiles.Read.StudentCourseDetails.Status,
		Profiles.Read.RelationshipStudents(PermissionsSelector.Any[StudentRelationshipType]),

		Profiles.Read.Disability, // TAB-4386

		Profiles.Search,
		Profiles.ViewSearchResults,

		Profiles.MeetingRecord.Read(PermissionsSelector.Any[StudentRelationshipType]),

		MemberNotes.Read,

		Profiles.Read.SmallGroups,
		Profiles.Read.Coursework,
		Profiles.Read.AccreditedPriorLearning,

		// Can read Coursework info for any student on this Route
		Submission.Read,
		AssignmentFeedback.Read,
		ExamFeedback.Read,
		Extension.Read,

		Profiles.Read.ModuleRegistration.Core,
		Profiles.Read.ModuleRegistration.Results
	)

	def canDelegateThisRolesPermissions: JavaImports.JBoolean = false
}