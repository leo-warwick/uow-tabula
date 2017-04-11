package uk.ac.warwick.tabula.system.permissions

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.permissions.RoleService
import uk.ac.warwick.tabula.system.CustomDataBinder
import uk.ac.warwick.tabula.services.permissions.PermissionDefinition
import uk.ac.warwick.tabula.data.model.{Department, FileAttachment, Module}
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue

class PermissionsCheckingMethodsTest extends TestBase with Mockito with PermissionsChecking {

	val dept: Department = Fixtures.department("in", "IT Services")
	dept.id = "dept"

	val mod1: Module = Fixtures.module("in101", "IN 1")
	mod1.id = "mod1"

	val mod2: Module = Fixtures.module("in102", "IN 2")
	mod2.id = "mod2"

	class Binder(obj:Any, name:String, val securityService:SecurityService)
			extends CustomDataBinder(obj, name)
			with PermissionsBinding

	private def clearAnyChecks() { permissionsAnyChecks.clear() }

	@Test def checks() {
		PermissionCheck(Permissions.Module.Create, dept)
		PermissionCheck(Permissions.Module.Delete, dept)
		PermissionCheckAll(Permissions.Module.ManageAssignments, Seq(mod1, mod2))
		PermissionCheck(Permissions.UserPicker)

		permissionsAllChecks should be (Map(
			Permissions.Module.Create -> Set(Some(dept)),
			Permissions.Module.Delete -> Set(Some(dept)),
			Permissions.Module.ManageAssignments -> Set(Some(mod1), Some(mod2)),
			Permissions.UserPicker -> Set(None)
		))
	}

	@Test def linkedAssignmentToModule() {
		val assignment = Fixtures.assignment("my assignment")
		assignment.module = mod1

		mustBeLinked(assignment, mod1)

		assignment.module = mod2

		try {
			mustBeLinked(assignment, mod1)
			fail("expected exception")
		} catch {
			case e: ItemNotFoundException =>
		}
	}

	@Test def linkedFeedbackToAssignment() {
		val ass1 = Fixtures.assignment("my assignment")
		ass1.id = "ass1"

		val ass2 = Fixtures.assignment("my assignment2")
		ass2.id = "ass2"

		val feedback = Fixtures.assignmentFeedback()

		feedback.assignment = ass1

		mustBeLinked(feedback, ass1)

		feedback.assignment = ass2

		try {
			mustBeLinked(feedback, ass1)
			fail("expected exception")
		} catch {
			case e: ItemNotFoundException =>
		}
	}

	@Test def linkedSubmissionToAssignment() {
		val ass1 = Fixtures.assignment("my assignment")
		ass1.id = "ass1"

		val ass2 = Fixtures.assignment("my assignment2")
		ass2.id = "ass2"

		val submission = Fixtures.submission()

		submission.assignment = ass1

		mustBeLinked(submission, ass1)

		submission.assignment = ass2

		try {
			mustBeLinked(submission, ass1)
			fail("expected exception")
		} catch {
			case e: ItemNotFoundException =>
		}
	}

	@Test def linkedFileAttachmentToSubmission() {

		val attachment = new FileAttachment

		val submission1 = Fixtures.submissionWithId("0000001", id = "submission1")
		val submission2 = Fixtures.submissionWithId("0000002", id = "submission2")

		val sv1 = new SavedFormValue
		sv1.submission = submission1

		val sv2 = new SavedFormValue
		sv2.submission = submission2

		attachment.submissionValue = sv1

		mustBeLinked(attachment, submission1)

		attachment.submissionValue = sv2

		try {
			mustBeLinked(attachment, submission1)
			fail("expected exception")
		} catch {
			case e: ItemNotFoundException =>
		}
	}

	@Test def linkedMarkingWorkflowToDepartment() {
		val markingWorkflow = Fixtures.seenSecondMarkingLegacyWorkflow("my workflow")
		markingWorkflow.department = dept

		mustBeLinked(markingWorkflow, dept)

		val dept2 = Fixtures.department("xx", "dept 2")
		dept2.id = "dept2"

		markingWorkflow.department = dept2

		try {
			mustBeLinked(markingWorkflow, dept)
			fail("expected exception")
		} catch {
			case e: ItemNotFoundException =>
		}
	}

	@Test def linkedFeedbackTemplateToDepartment() {
		val template = Fixtures.feedbackTemplate("my template")
		template.department = dept

		mustBeLinked(template, dept)

		val dept2 = Fixtures.department("xx", "dept 2")
		dept2.id = "dept2"

		template.department = dept2

		try {
			mustBeLinked(template, dept)
			fail("expected exception")
		} catch {
			case e: ItemNotFoundException =>
		}
	}

	@Test def mandatory() {
		val assignment = Fixtures.assignment("my assignment")
		mandatory(assignment) should be (assignment)

		try {
			mandatory(null)
			fail("expected exception")
		} catch {
			case e: ItemNotFoundException =>
		}

		mandatory(Some("yes")) should be ("yes")

		try {
			mandatory(None)
			fail("expected exception")
		} catch {
			case e: ItemNotFoundException =>
		}
	}

	@Test def notDeleted() {
		val assignment = Fixtures.assignment("my assignment")
		notDeleted(assignment)

		try {
			assignment.deleted = true
			notDeleted(assignment)
			fail("expected exception")
		} catch {
			case e: ItemNotFoundException =>
		}
	}

	@Test def checkOneOf() {
		val user = new User("custard")
		user.setIsLoggedIn(true)
		user.setFoundUser(true)
		val currentUser = new CurrentUser(user, user)

		val securityService = new SecurityService
		val roleService = mock[RoleService]
		roleService.getExplicitPermissionsFor(currentUser, mod1) returns Stream(
			PermissionDefinition(Permissions.Assignment.Create, Some(mod1), true)
		)
		securityService.roleService = roleService

		PermissionCheckAny(
			Seq(CheckablePermission(Permissions.Assignment.Create, mod1),
				CheckablePermission(Permissions.Assignment.Update, mod1))
		)

		permissionsAnyChecks should be (Map(
			Permissions.Assignment.Create -> Set(Some(mod1)),
			Permissions.Assignment.Update -> Set(Some(mod1))
		))

		try { withCurrentUser(currentUser) {
			new Binder(this, "OneFromTwoScoped", securityService)
		}} catch {
			case e: PermissionDeniedException => fail("One of the two scoped permissions should be enough")
		}

		clearAnyChecks()

		PermissionCheckAny(
			Seq(CheckablePermission(Permissions.Profiles.Search),
				CheckablePermission(Permissions.Assignment.Create, mod1))
		)

		permissionsAnyChecks should be (Map(
			Permissions.Profiles.Search -> Set(None),
			Permissions.Assignment.Create -> Set(Some(mod1))
		))

		roleService.getRolesFor(currentUser, null) returns Stream.empty
		try { withCurrentUser(currentUser) {
			new Binder(this, "OneFromOneScopedOneScopeless", securityService)
		}} catch {
			case e: PermissionDeniedException => fail("The matching scoped permission should be enough")
		}

		clearAnyChecks()

		PermissionCheckAny(
			Seq(CheckablePermission(Permissions.Profiles.Search),
				CheckablePermission(Permissions.Assignment.Read, mod1))
		)

		permissionsAnyChecks should be (Map(
			Permissions.Profiles.Search -> Set(None),
			Permissions.Assignment.Read -> Set(Some(mod1))
		))

		try { withCurrentUser(currentUser) {
			new Binder(this, "NoneFromOneScopedOneScopeless", securityService)
			fail ("Neither permission should match")
		}} catch {
			case e: Exception =>
		}

		clearAnyChecks()

		try { withCurrentUser(currentUser) {
			new Binder(this, "NoneFromEmpty", securityService)
			fail("No permissions to check, so should fail")
		}} catch {
			case e: Exception =>
		}
	}
}