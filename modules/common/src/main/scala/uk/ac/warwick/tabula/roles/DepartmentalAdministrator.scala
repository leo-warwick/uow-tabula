package uk.ac.warwick.tabula.roles

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data._

import uk.ac.warwick.tabula.permissions.Permissions._

case class DepartmentalAdministrator(department: model.Department) extends BuiltInRole(department, DepartmentalAdministratorRoleDefinition)

case object DepartmentalAdministratorRoleDefinition extends BuiltInRoleDefinition {
	
	// Implicitly grants module manager role for all modules in this department
	GeneratesSubRole(ModuleManagerRoleDefinition)
		
	GrantsScopedPermission( 
		Department.ManageExtensionSettings,
		Department.ManageDisplaySettings,
		Department.DownloadFeedbackReport,
		
		RolesAndPermissions.Create,
		RolesAndPermissions.Read,
		RolesAndPermissions.Update,
		RolesAndPermissions.Delete,
		
		Assignment.ImportFromExternalSystem,
		
		FeedbackTemplate.Create,
		FeedbackTemplate.Read,
		FeedbackTemplate.Update,
		FeedbackTemplate.Delete,
		
		MarkingWorkflow.Create,
		MarkingWorkflow.Read,
		MarkingWorkflow.Update,
		MarkingWorkflow.Delete,
		
		Profiles.Read,
		Profiles.Search,
		
		Profiles.PersonalTutor.Upload,
		Profiles.PersonalTutor.Create,
		Profiles.PersonalTutor.Read,
		Profiles.PersonalTutor.Update,
		Profiles.PersonalTutor.Delete
	)

}