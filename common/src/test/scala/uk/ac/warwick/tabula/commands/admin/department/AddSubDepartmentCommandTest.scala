package uk.ac.warwick.tabula.commands.admin.department

import javax.sql.DataSource

import org.hibernate.{Session, SessionFactory}
import uk.ac.warwick.tabula.services.{RelationshipService, UserLookupService, ModuleAndDepartmentServiceComponent, ModuleAndDepartmentService}
import uk.ac.warwick.tabula.{FunctionalContextTesting, FunctionalContext, TestBase, Mockito, Fixtures}
import uk.ac.warwick.tabula.data.model.{UserGroup, Department}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import org.hamcrest.Matchers._
import uk.ac.warwick.tabula.roles.ExtensionManagerRoleDefinition

class AddSubDepartmentCommandTest extends TestBase  with FunctionalContextTesting with Mockito {

	import AddSubDepartmentCommandTest.MinimalCommandContext

	trait CommandTestSupport extends AddSubDepartmentCommandState with ModuleAndDepartmentServiceComponent {
		val moduleAndDepartmentService: ModuleAndDepartmentService = mock[ModuleAndDepartmentService]

		moduleAndDepartmentService.getDepartmentByCode("in-pg") returns Some(Fixtures.department("in-pg", "IT Services Postgraduate"))
		moduleAndDepartmentService.getDepartmentByCode(isNotEq("in-pg")) returns None
	}

	trait Fixture {

		val parent: Department = Fixtures.department("in", "IT Services")
		parent.id = "in-test"
		parent.allowExtensionRequests = true
		parent.autoGroupDeregistration = false

		val ug: UserGroup = UserGroup.ofUsercodes
		ug.addUserId("cuslaj")

		val permissionsService: PermissionsService = mock[PermissionsService]
		permissionsService.ensureUserGroupFor(parent, ExtensionManagerRoleDefinition) returns ug
		parent.permissionsService = permissionsService

		val command = new AddSubDepartmentCommandInternal(parent) with CommandTestSupport with AddSubDepartmentCommandValidation
	}


	@Test def init() { new Fixture {
		command.code should startWith("in-")
		command.name should startWith("IT Services ")
		command.filterRule should be(Department.AllMembersFilterRule)
	}}

	@Test def apply() { inContext[MinimalCommandContext] { new Fixture {
		command.code = "in-ug"
		command.name = "IT Services Undergraduate"
		command.filterRule = Department.UndergraduateFilterRule

		val dept: Department = command.applyInternal()
		dept.code should be ("in-ug")
		dept.name should be ("IT Services Undergraduate")
		dept.filterRule should be (Department.UndergraduateFilterRule)
		dept.parent should be (parent)
		dept.allowExtensionRequests should be (true)
		dept.autoGroupDeregistration should be (false)
		dept.extensionManagers.knownType.includedUserIds should contain("cuslaj")

		verify(command.moduleAndDepartmentService, times(2)).saveOrUpdate(dept)
	}}}

	@Test def validateNoErrors() { new Fixture {
		command.code = "in-ug"
		command.name = "IT Services Undergraduate"
		command.filterRule = Department.UndergraduateFilterRule

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validateEmptyCode() { new Fixture {
		command.code = ""
		command.name = "IT Services Undergraduate"
		command.filterRule = Department.UndergraduateFilterRule

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("code")
		errors.getFieldError.getCodes should contain ("department.code.empty")
	}}

	@Test def validateCodeDoesntStartWithParent() { new Fixture {
		command.code = "itservices-ug"
		command.name = "IT Services Undergraduate"
		command.filterRule = Department.UndergraduateFilterRule

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("code")
		errors.getFieldError.getCodes should contain ("department.code.mustStartWithParent")
	}}

	@Test def validateCodeTooLong() { new Fixture {
		command.code = "in-an-incredibly-long-code-this-is-silly-now"
		command.name = "IT Services Undergraduate"
		command.filterRule = Department.UndergraduateFilterRule

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("code")
		errors.getFieldError.getCodes should contain ("department.code.tooLong")
	}}

	@Test def validateCodeBadFormat() { new Fixture {
		command.code = "in-UG Students"
		command.name = "IT Services Undergraduate"
		command.filterRule = Department.UndergraduateFilterRule

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("code")
		errors.getFieldError.getCodes should contain ("department.code.badFormat")
	}}

	@Test def validateExistingCode() { new Fixture {
		command.code = "in-pg"
		command.name = "IT Services Postgraduate"
		command.filterRule = Department.PostgraduateFilterRule

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("code")
		errors.getFieldError.getCodes should contain ("department.code.exists")
	}}

	@Test def validateEmptyName() { new Fixture {
		command.code = "in-ug"
		command.name = "  "
		command.filterRule = Department.UndergraduateFilterRule

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("name")
		errors.getFieldError.getCodes should contain ("department.name.empty")
	}}

	// Remove this requirement TAB-2498
//	@Test def validateNameDoesntStartWithParent() { new Fixture {
//		command.code = "in-ug"
//		command.name = "ITS Undergraduates"
//		command.filterRule = Department.UndergraduateFilterRule
//
//		val errors = new BindException(command, "command")
//		command.validate(errors)
//
//		errors.hasErrors should be (true)
//		errors.getErrorCount should be (1)
//		errors.getFieldError.getField should be ("name")
//		errors.getFieldError.getCodes should contain ("department.name.mustStartWithParent")
//	}}

	@Test def validateNameTooLong() { new Fixture {
		command.code = "in-ug"
		command.name = "IT Services Undergraduate Students Who Have Come To Study Computers And Programming And Things, What A Wonderful Sight This Is"
		command.filterRule = Department.UndergraduateFilterRule

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("name")
		errors.getFieldError.getCodes should contain ("department.name.tooLong")
	}}

	@Test def validateEmptyFilterRule() { new Fixture {
		command.code = "in-ug"
		command.name = "IT Services Undergraduates"
		command.filterRule = null

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("filterRule")
		errors.getFieldError.getCodes should contain ("department.filterRule.empty")
	}}

	@Test def validateLessSpecificFilterRule() { new Fixture {
		command.code = "in-ug"
		command.name = "IT Services Undergraduates"

		parent.filterRule = Department.UndergraduateFilterRule
		command.filterRule = Department.AllMembersFilterRule

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("filterRule")
		errors.getFieldError.getCodes should contain ("department.filterRule.contradictory")
	}}

	@Test def validateContradictoryFilterRule() { new Fixture {
		command.code = "in-ug"
		command.name = "IT Services Undergraduates"

		parent.filterRule = Department.UndergraduateFilterRule
		command.filterRule = Department.PostgraduateFilterRule

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("filterRule")
		errors.getFieldError.getCodes should contain ("department.filterRule.contradictory")
	}}

}

object AddSubDepartmentCommandTest {
	class MinimalCommandContext extends FunctionalContext with Mockito {

		bean() {
			val permissionsService = mock[PermissionsService]
			permissionsService.ensureUserGroupFor(anArgThat(anything), anArgThat(anything))(anArgThat(anything)) returns UserGroup.ofUsercodes
			permissionsService
		}

		bean(){mock[UserLookupService]}
		bean(){mock[RelationshipService]}
		bean(){mock[ModuleAndDepartmentService]}
		bean(){
			val sessionFactory = smartMock[SessionFactory]
			val session = smartMock[Session]
			sessionFactory.getCurrentSession returns session
			sessionFactory.openSession() returns session
			sessionFactory
		}
		bean("dataSource"){mock[DataSource]}
	}
}