package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.permissions.PermissionsSelector
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.JavaImports._

case class DepartmentalAdministrator(department: model.Department) extends BuiltInRole(DepartmentalAdministratorRoleDefinition, department)

case object DepartmentalAdministratorRoleDefinition extends BuiltInRoleDefinition {

	override def description = "Departmental Administrator"

	//
	// If you're removing permissions from here, please consider whether you should be
	// adding them back into UserAccessManager at the same time.
	//

	// Implicitly grants module manager role for all modules in this department, and route manager for all routes in this department
	GeneratesSubRole(ModuleManagerRoleDefinition)
	GeneratesSubRole(RouteManagerRoleDefinition)

	GrantsScopedPermission(
		Masquerade,

		Department.ManageExtensionSettings,
		Department.ManageDisplaySettings,
		Department.ManageNotificationSettings,
		Department.DownloadFeedbackReport,

		Department.ArrangeRoutesAndModules,

		Assignment.ImportFromExternalSystem,
		SmallGroups.ImportFromExternalSystem,

		FeedbackTemplate.Read,
		FeedbackTemplate.Manage,

		MarkingWorkflow.Read,
		MarkingWorkflow.Manage,

		Department.ManageProfiles,

		MonitoringPoints.Report,

		MemberNotes.Delete,

		Profiles.MeetingRecord.ReadDetails(PermissionsSelector.Any[StudentRelationshipType]),
		Profiles.MeetingRecord.Manage(PermissionsSelector.Any[StudentRelationshipType]),

		Profiles.ScheduledMeetingRecord.Manage(PermissionsSelector.Any[StudentRelationshipType]),

		Profiles.MeetingRecord.Approve,
		Profiles.ScheduledMeetingRecord.Confirm,

		// TAB-1878
		Profiles.Read.TelephoneNumber,
		Profiles.Read.MobileNumber,

		SmallGroups.Read,

		Department.Reports,
		Department.ExamGrids
	)
	def canDelegateThisRolesPermissions:JBoolean = true

}
