package uk.ac.warwick.tabula.commands.admin

import org.springframework.validation.BindException
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.{Appliable, Describable}
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.web.Cookie
import uk.ac.warwick.userlookup.User

class MasqueradeCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends MasqueradeCommandState with UserLookupComponent {
		val userLookup = new MockUserLookup
		userLookup.registerUsers("cusebr")
	}

	trait Fixture {
		val user = new CurrentUser(new User("cuscav"), new User("cuscav"))

		val command = new MasqueradeCommandInternal(user) with CommandTestSupport
	}

	@Test def set { new Fixture {
		command.usercode = "cusebr"

		val cookie: Option[Cookie] = command.applyInternal()
		cookie should be ('defined)
		cookie.map { cookie =>
			cookie.cookie.getName() should be (CurrentUser.masqueradeCookie)
			cookie.cookie.getValue() should be ("cusebr")
			cookie.cookie.getPath() should be ("/")
		}
	}}

	@Test def setInvalidUser { new Fixture {
		command.usercode = "undefined"

		val cookie: Option[Cookie] = command.applyInternal()
		cookie should be ('empty)
	}}

	@Test def remove { new Fixture {
		command.action = "remove"

		val cookie: Option[Cookie] = command.applyInternal()
		cookie should be ('defined)
		cookie.map { cookie =>
			cookie.cookie.getName() should be (CurrentUser.masqueradeCookie)
			cookie.cookie.getValue() should be (null) // removal
			cookie.cookie.getPath() should be ("/")
		}
	}}

	private trait ValidationFixture {
		val command = new MasqueradeCommandValidation with CommandTestSupport with ProfileServiceComponent with SecurityServiceComponent with ModuleAndDepartmentServiceComponent {
			val profileService: ProfileService = smartMock[ProfileService]
			val securityService: SecurityService = smartMock[SecurityService]
			val moduleAndDepartmentService: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]

			val ssoUser = new User("cuscav")
			val user = new CurrentUser(ssoUser, ssoUser)
		}
	}

	@Test def validateCan() { new ValidationFixture {
		command.usercode = "cusebr"

		val student: StudentMember = Fixtures.student()

		command.moduleAndDepartmentService.departmentsWithPermission(command.user, Permissions.Masquerade) returns (Set())
		command.profileService.getMemberByUser(command.userLookup.getUserByUserId("cusebr"), disableFilter = true, eagerLoad = false) returns (Some(student))
		command.securityService.can(command.user, Permissions.Masquerade, student) returns (true)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validateCant() { new ValidationFixture {
		command.usercode = "cusebr"

		val student: StudentMember = Fixtures.student()

		command.moduleAndDepartmentService.departmentsWithPermission(command.user, Permissions.Masquerade) returns (Set())
		command.profileService.getMemberByUser(command.userLookup.getUserByUserId("cusebr"), disableFilter = true, eagerLoad = false) returns (Some(student))
		command.securityService.can(command.user, Permissions.Masquerade, student) returns (false)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("usercode")
		errors.getFieldError.getCodes should contain ("masquerade.noPermission")
	}}

	@Test def validateCantBecauseSubDepartment() { new ValidationFixture {
		command.usercode = "cusebr"

		val parentDepartment: Department = Fixtures.department("in")
		val subDepartment: Department = Fixtures.department("in-ug")

		parentDepartment.children.add(subDepartment)
		subDepartment.parent = parentDepartment

		val student: StudentMember = Fixtures.student()
		student.homeDepartment = parentDepartment

		command.moduleAndDepartmentService.departmentsWithPermission(command.user, Permissions.Masquerade) returns (Set(subDepartment))
		command.profileService.getMemberByUser(command.userLookup.getUserByUserId("cusebr"), disableFilter = true, eagerLoad = false) returns (Some(student))
		command.securityService.can(command.user, Permissions.Masquerade, student) returns (false)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test
	def glueEverythingTogether() = withUser("cuscav") {
		val command = MasqueradeCommand(currentUser)

		command should be (anInstanceOf[Appliable[Option[Cookie]]])
		command should be (anInstanceOf[MasqueradeCommandState])
		command should be (anInstanceOf[Describable[Option[Cookie]]])
	}

}