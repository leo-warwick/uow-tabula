package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, ProfileService, StaffAssistantsHelpers}
import uk.ac.warwick.tabula.data.model.{Department, MemberUserType}
import uk.ac.warwick.tabula.roles._

class UserTypeAndDepartmentRoleProviderTest extends TestBase with Mockito {

	val provider = new UserTypeAndDepartmentRoleProvider

	val profileService: ProfileService with StaffAssistantsHelpers = mock[ProfileService with StaffAssistantsHelpers]
	val departmentService: ModuleAndDepartmentService = mock[ModuleAndDepartmentService]

	provider.profileService = profileService
	provider.departmentService.set(departmentService)

	val dept1: Department = Fixtures.department("cs")
	val dept2: Department = Fixtures.department("en")
	val dept3: Department = Fixtures.department("wm")

	@Test def student = withUser("cuscav") {
		val member = Fixtures.student(universityId="0123456", department=dept1, courseDepartment=dept3)
		member.profileService = profileService

		profileService.getAllMembersWithUserId("cuscav", true) returns (Seq(member))
		provider.getRolesFor(currentUser) should be (Seq(UniversityMemberRole(member), LoggedInRole(currentUser.apparentUser)))
	}

	@Test def staff = withUser("cuscav") {
		val member = Fixtures.staff(department=dept1)
		profileService.getAllMembersWithUserId("cuscav", true) returns (Seq(member))

		provider.getRolesFor(currentUser) should be (Seq(UniversityMemberRole(member), StaffRole(dept1), LoggedInRole(currentUser.apparentUser)))
	}

	@Test def emeritus = withUser("cuscav") {
		val member = Fixtures.member(MemberUserType.Emeritus, department=dept2)
		profileService.getAllMembersWithUserId("cuscav", true) returns (Seq(member))

		provider.getRolesFor(currentUser) should be (Seq(UniversityMemberRole(member), StaffRole(dept2), LoggedInRole(currentUser.apparentUser)))
	}

	@Test def other = withUser("cuscav") {
		val member = Fixtures.member(MemberUserType.Other, department=dept1)
		profileService.getAllMembersWithUserId("cuscav", true) returns (Seq(member))

		provider.getRolesFor(currentUser) should be (Seq(UniversityMemberRole(member), LoggedInRole(currentUser.apparentUser)))
	}

	@Test def multipleRolesMultipleDepartments = withUser("cuscav") {
		val member1 = Fixtures.student(universityId="0123456", department=dept1, courseDepartment=dept3)
		member1.profileService = profileService
		val member2 = Fixtures.staff(department=dept2)

		profileService.getAllMembersWithUserId("cuscav", true) returns (Seq(member1, member2))

		provider.getRolesFor(currentUser) should be (Seq(UniversityMemberRole(member1), UniversityMemberRole(member2), StaffRole(dept2), LoggedInRole(currentUser.apparentUser)))
	}

	@Test def fallbackToSSOStaff = withUser("cuscav") {
		profileService.getAllMembersWithUserId("cuscav", true) returns (Seq())
		departmentService.getDepartmentByCode("cs") returns (Some(dept1))

		currentUser.realUser.setDepartmentCode("CS")
		currentUser.realUser.setStaff(true)

		provider.getRolesFor(currentUser) should be (Seq(SSOStaffRole(dept1), LoggedInRole(currentUser.apparentUser)))
	}

	@Test def fallbackToSSOStudent = withUser("cuscav") {
		profileService.getAllMembersWithUserId("cuscav", true) returns (Seq())
		departmentService.getDepartmentByCode("en") returns (Some(dept2))

		currentUser.realUser.setDepartmentCode("EN")
		currentUser.realUser.setStudent(true)

		provider.getRolesFor(currentUser) should be (Seq(LoggedInRole(currentUser.apparentUser)))
	}

	@Test def fallbackToSSOOther = withUser("cuscav") {
		profileService.getAllMembersWithUserId("cuscav", true) returns (Seq())

		provider.getRolesFor(currentUser) should be (Seq(LoggedInRole(currentUser.apparentUser)))
	}

}