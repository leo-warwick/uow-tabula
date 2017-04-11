package uk.ac.warwick.tabula.data.model.notifications.coursework

import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula.data.model.permissions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.{PermissionsTarget, Permissions}
import uk.ac.warwick.tabula.roles.{DepartmentalAdministratorRoleDefinition, ModuleManagerRoleDefinition}
import uk.ac.warwick.tabula.services.{UserSettingsService, SecurityService, UserGroupCacheManager, UserLookupService}
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula._

class SubmissionReceivedNotificationTest extends TestBase  with Mockito {


	val userLookup = new MockUserLookup

	@Test def titleOnTime() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 39, 0, 0)) { withUser("cuscav", "0672089") {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

		val submission = Fixtures.submission()
		submission.assignment = assignment
		submission.submittedDate = DateTime.now

		assignment.isLate(submission) should be (false)
		assignment.isAuthorisedLate(submission) should be (false)

		val notification = Notification.init(new SubmissionReceivedNotification, currentUser.apparentUser, submission, assignment)
		notification.title should be ("CS118: Submission received for \"5,000 word essay\"")
	}}

	@Test def titleOnTimeBeforeExtension() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 39, 0, 0)) { withUser("cuscav", "0672089") {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

		val submission = Fixtures.submission()
		submission.assignment = assignment
		submission.submittedDate = DateTime.now

		val extension = Fixtures.extension()
		extension.assignment = assignment
		extension.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)
		extension.approve()
		assignment.extensions.add(extension)

		assignment.isLate(submission) should be (false)
		assignment.isAuthorisedLate(submission) should be (false)

		val notification = Notification.init(new SubmissionReceivedNotification, currentUser.apparentUser, submission, assignment)
		notification.title should be ("CS118: Submission received for \"5,000 word essay\"")
	}}

	@Test def titleLate() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 39, 0, 0)) { withUser("cuscav", "0672089") {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

		val submission = Fixtures.submission()
		submission.assignment = assignment
		submission.submittedDate = DateTime.now

		assignment.isLate(submission) should be (true)
		assignment.isAuthorisedLate(submission) should be (false)

		val notification = Notification.init(new SubmissionReceivedNotification, currentUser.apparentUser, submission, assignment)
		notification.title should be ("CS118: Late submission received for \"5,000 word essay\"")
	}}

	@Test def titleLateWithinExtension() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 39, 0, 0)) { withUser("cuscav", "0672089") {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

		val submission = Fixtures.submission()
		submission.assignment = assignment
		submission.submittedDate = DateTime.now

		val extension = Fixtures.extension()
		extension.assignment = assignment
		extension.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)
		extension.approve()
		assignment.extensions.add(extension)

		assignment.isLate(submission) should be (false)
		assignment.isAuthorisedLate(submission) should be (true)

		val notification = Notification.init(new SubmissionReceivedNotification, currentUser.apparentUser, submission, assignment)
		notification.title should be ("CS118: Authorised late submission received for \"5,000 word essay\"")
	}}

	@Test def titleLateAfterExtension() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 39, 0, 0)) { withUser("cuscav", "0672089") {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

		val submission = Fixtures.submission()
		submission.assignment = assignment
		submission.submittedDate = DateTime.now

		val extension = Fixtures.extension()
		extension.assignment = assignment
		extension.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)
		extension.approve()
		assignment.extensions.add(extension)

		assignment.isLate(submission) should be (true)
		assignment.isAuthorisedLate(submission) should be (false)

		val notification = Notification.init(new SubmissionReceivedNotification, currentUser.apparentUser, submission, assignment)
		notification.title should be ("CS118: Late submission received for \"5,000 word essay\"")
	}}




	@Test def recipientsForLateNotificationWithNoAdminForSubDept() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 39, 0, 0)) { withUser("cuscav", "0672089") {

		val securityService = mock[SecurityService]
		val permissionsService = mock[PermissionsService]
		val service = mock[UserSettingsService]

		//create dept with one sub dept
		val department = Fixtures.department("ch")
		val subDepartment = Fixtures.department("ch-ug")
		subDepartment.parent = department

		val assignment = Fixtures.assignment("5,000 word essay")
		val module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.module = module
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

		val submission = Fixtures.submission()
		submission.assignment = assignment
		submission.submittedDate = DateTime.now
		module.adminDepartment = subDepartment

		val adminMember = new StaffMember
		adminMember.universityId = "admin"
		adminMember.userId = "admin"
		val admin = adminMember.asSsoUser

		val deptAdminMember = new StaffMember
		deptAdminMember.universityId = "admin2"
		deptAdminMember.userId = "admin2"
		val deptAdmin = deptAdminMember.asSsoUser

		val moduleManagerMember = new StaffMember
		moduleManagerMember.universityId = "admin3"
		moduleManagerMember.userId = "admin3"
		val moduleManager = moduleManagerMember.asSsoUser

		userLookup.users = Map("admin" -> admin, "admin2" -> deptAdmin, "admin3" -> moduleManager)
		department.permissionsService = permissionsService
		module.permissionsService = permissionsService

		val assignmentWithParents = Fixtures.withParents(assignment);
		val targetAssignment = assignmentWithParents(0);
		val targetModule = assignmentWithParents(1);
		val targetDept = assignmentWithParents(2);
		val targetParentDept = assignmentWithParents(3);

		val moduleGrantedRole = new ModuleGrantedRole(module, ModuleManagerRoleDefinition)
		moduleGrantedRole.users.add(moduleManager)
		wireUserLookup(moduleGrantedRole.users)

		val deptGrantedRole = new DepartmentGrantedRole(department, DepartmentalAdministratorRoleDefinition)
		deptGrantedRole.users.add(deptAdmin)
		wireUserLookup(deptGrantedRole.users)

		permissionsService.getAllGrantedRolesFor(targetAssignment) returns Nil
		permissionsService.getAllGrantedRolesFor(targetDept) returns Nil
		permissionsService.getAllGrantedRolesFor[PermissionsTarget](targetModule) returns (Stream(moduleGrantedRole).asInstanceOf[Stream[GrantedRole[PermissionsTarget]]])
		permissionsService.getAllGrantedRolesFor[PermissionsTarget](targetParentDept) returns (Stream(deptGrantedRole).asInstanceOf[Stream[GrantedRole[PermissionsTarget]]])



		val existing = GrantedPermission(targetDept, Permissions.Submission.Delete, true)
		existing.users.knownType.addUserId("admin3")

		permissionsService.getGrantedPermission(targetAssignment, Permissions.Submission.Delete, RoleOverride.Allow) returns (None)
		permissionsService.getGrantedPermission(targetDept, Permissions.Submission.Delete, true) returns (Some(existing))
		permissionsService.getGrantedPermission(targetModule, Permissions.Submission.Delete, RoleOverride.Allow) returns (None)
		permissionsService.getGrantedPermission(targetDept, Permissions.Submission.Delete, RoleOverride.Allow) returns (None)
		permissionsService.getGrantedPermission(targetParentDept, Permissions.Submission.Delete, RoleOverride.Allow) returns (None)


		val subNotification = new SubmissionReceivedNotification
		subNotification.permissionsService = permissionsService
		subNotification.securityService = securityService

		securityService.can(isA[CurrentUser], isEq(Permissions.Submission.Delete), isA[PermissionsTarget]) returns (true)

		val settings = new UserSettings("userId")
		subNotification.userSettings = service

		service.getByUserId("admin3") returns (None)
		service.getByUserId("admin2") returns (None)

		val n = Notification.init(subNotification, currentUser.apparentUser, submission, assignment)

		n.recipients.size should be (2)




		//notification.title should be ("CS118: Late submission received for \"5,000 word essay\"")
	}


	}

	def wireUserLookup(userGroup: UnspecifiedTypeUserGroup): Unit = userGroup match {
		case cm: UserGroupCacheManager => wireUserLookup(cm.underlying)
		case ug: UserGroup => ug.userLookup = userLookup
	}




}
