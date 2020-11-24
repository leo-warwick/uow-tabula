package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.permissions.PermissionsSelector
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

case class StaffRole(department: model.Department) extends BuiltInRole(StaffRoleDefinition, department)

case class SSOStaffRole(department: model.Department) extends BuiltInRole(SSOStaffRoleDefinition, department)

case object StaffRoleDefinition extends UnassignableBuiltInRoleDefinition {

  override def description = "Staff Member"

  GeneratesSubRole(SSOStaffRoleDefinition)

  GrantsScopedPermission(
    Profiles.Read.Disability, // TAB-4386
    Profiles.Read.Gender
  )
}

/**
  * In the case of users not having a member record, they may get SSOStaffRoleDefinition because
  * they are staff=true in SSO. Since staff=true is given to PGRs who would normally be a student,
  * we need to be more careful about exposing sensitive information in this role. If you're not sure,
  * put it in StaffRoleDefinition above.
  */
case object SSOStaffRoleDefinition extends UnassignableBuiltInRoleDefinition {

  override def description = "Staff Member (SSO)"

  GrantsScopelessPermission(
    UserPicker,
    MonitoringPointTemplates.View
  )

  GrantsGlobalPermission(
    Profiles.Search,
    Profiles.Read.Core, // As per discussion in TAB-753, anyone at the University can see anyone else's core information
    Profiles.Read.Photo,

    // TAB-1619
    Profiles.Read.Usercode,
    Profiles.Read.StudentCourseDetails.Core,
    Profiles.StudentRelationship.Read(PermissionsSelector.Any[StudentRelationshipType]),

    // TAB-4184
    Profiles.Read.Timetable
  )

  GrantsScopedPermission(
    Profiles.Read.StudentCourseDetails.Status,
    Profiles.ViewSearchResults,

    Profiles.Read.SmallGroups
  )

}