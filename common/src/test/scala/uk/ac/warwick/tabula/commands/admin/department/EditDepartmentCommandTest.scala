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

class EditDepartmentCommandTest extends TestBase  with FunctionalContextTesting with Mockito {

	import EditDepartmentCommandTest.MinimalCommandContext

	trait CommandTestSupport extends EditDepartmentCommandState with ModuleAndDepartmentServiceComponent {
		val moduleAndDepartmentService: ModuleAndDepartmentService = mock[ModuleAndDepartmentService]

		moduleAndDepartmentService.getDepartmentByCode("in-pg") returns Some(Fixtures.department("in-pg", "IT Services Postgraduate"))
		moduleAndDepartmentService.getDepartmentByCode(isNotEq("in-pg")) returns None
	}

	trait Fixture {
		val parent: Department = Fixtures.department("in", "Information Technology Services")
		parent.shortName = "IT Services"
		parent.id = "in"
		parent.allowExtensionRequests = true
		parent.autoGroupDeregistration = false

		val ug: UserGroup = UserGroup.ofUsercodes
		ug.addUserId("cuslaj")

		val permissionsService: PermissionsService = mock[PermissionsService]
		permissionsService.ensureUserGroupFor(parent, ExtensionManagerRoleDefinition) returns ug
		parent.permissionsService = permissionsService

		val department: Department = Fixtures.department("in-ug", "IT Services Undergraduate")
		department.id = "in-ug"
		department.allowExtensionRequests = false
		department.autoGroupDeregistration = true
		department.filterRule = Department.UndergraduateFilterRule
		department.parent = parent

		val command = new EditDepartmentCommandInternal(department) with CommandTestSupport with EditDepartmentCommandValidation
	}

	@Test def init() { new Fixture {
		command.code should be("in-ug")
		command.fullName should be("IT Services Undergraduate")
		command.filterRule should be(Department.UndergraduateFilterRule)
	}}

	@Test def apply() { inContext[MinimalCommandContext] { new Fixture {
		command.code = "in-ugs"
		command.fullName = "IT Services Undergraduates"
		command.shortName = "ITS UG"
		command.filterRule = Department.InYearFilterRule(1)

		val dept: Department = command.applyInternal()
		dept.code should be ("in-ugs")
		dept.name should be ("ITS UG")
		dept.fullName should be ("IT Services Undergraduates")
		dept.filterRule should be (Department.InYearFilterRule(1))
		dept.parent should be (parent)
		dept.allowExtensionRequests should be (false)
		dept.autoGroupDeregistration should be (true)

		verify(command.moduleAndDepartmentService, times(1)).saveOrUpdate(dept)
	}}}

	@Test def validateNoErrors() { new Fixture {
		command.code = "in-ug"
		command.fullName = "Information Technology Services Undergraduate"
		command.filterRule = Department.UndergraduateFilterRule

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validateEmptyCode() { new Fixture {
		command.code = ""
		command.fullName = "Information Technology Services Undergraduate"
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
		command.fullName = "Information Technology Services Undergraduate"
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
		command.fullName = "Information Technology Services Undergraduate"
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
		command.fullName = "Information Technology Services Undergraduate"
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
		command.fullName = "Information Technology Services Postgraduate"
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
		command.fullName = "  "
		command.filterRule = Department.UndergraduateFilterRule

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("fullName")
		errors.getFieldError.getCodes should contain ("department.name.empty")
	}}

	@Test def validateNameDoesntStartWithParent() { new Fixture {
		command.code = "in-ug"
		command.fullName = "ITS Undergraduates"
		command.filterRule = Department.UndergraduateFilterRule

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("fullName")
		errors.getFieldError.getCodes should contain ("department.name.mustStartWithParent")
	}}

	@Test def validateNameTooLong() { new Fixture {
		command.code = "in-ug"
		command.fullName = "Information Technology Services Undergraduate Students Who Have Come To Study Computers And Programming And Things, What A Wonderful Sight This Is"
		command.filterRule = Department.UndergraduateFilterRule

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("fullName")
		errors.getFieldError.getCodes should contain ("department.name.tooLong")
	}}

	@Test def validateShortNameTooLong() { new Fixture {
		command.code = "in-ug"
		command.fullName = "Information Technology Services Undergraduates"
		command.shortName = "IT Services Undergraduate Students Who Have Come To Study Computers And Programming And Things, What A Wonderful Sight This Is"
		command.filterRule = Department.UndergraduateFilterRule

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("shortName")
		errors.getFieldError.getCodes should contain ("department.name.tooLong")
	}}

	@Test def validateEmptyFilterRule() { new Fixture {
		command.code = "in-ug"
		command.fullName = "Information Technology Services Undergraduates"
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
		command.fullName = "Information Technology Services Undergraduates"

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
		command.fullName = "Information Technology Services Undergraduates"

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

object EditDepartmentCommandTest {
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