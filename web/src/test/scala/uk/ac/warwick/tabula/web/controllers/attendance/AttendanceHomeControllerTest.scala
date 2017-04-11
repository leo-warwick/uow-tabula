package uk.ac.warwick.tabula.web.controllers.attendance

import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.attendance.{HomeCommand, HomeCommandState, HomeInformation}
import uk.ac.warwick.tabula.data.model.{Department, StudentRelationshipType, UserSettings}
import uk.ac.warwick.tabula.helpers.Tap.tap
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringService, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.{AcademicYear, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class AttendanceHomeControllerTest extends TestBase with Mockito{

	class Fixture{
		val user = new User("cusfal")
		var info = HomeInformation(
			hasProfile = false,
			viewPermissions = Seq(),
			managePermissions = Seq(),
			allRelationshipTypes = Seq(),
			relationshipTypesMap = Map()
		)

		val command = new HomeCommand(null) with Appliable[HomeInformation] with HomeCommandState with ModuleAndDepartmentServiceComponent
			with CourseAndRouteServiceComponent with ProfileServiceComponent with RelationshipServiceComponent with AttendanceMonitoringServiceComponent {

			var relationshipService: RelationshipService = _
			var profileService: ProfileService = _
			var moduleAndDepartmentService: ModuleAndDepartmentService = _
			var courseAndRouteService: CourseAndRouteService = _
			var attendanceMonitoringService: AttendanceMonitoringService = _

			def apply(): HomeInformation = info
		}
		val departmentCode = "xx"
		val relationshipType: StudentRelationshipType = smartMock[StudentRelationshipType]

		val controller = new AttendanceHomeController
		controller.userSettingsService = smartMock[UserSettingsService]
		controller.userSettingsService.getByUserId(user.getUserId) returns None
		controller.features = emptyFeatures
	}


	@Test
	def onlyProfileRedirectedToProfileView(){ new Fixture{
		info = HomeInformation(
			hasProfile = true,
			viewPermissions = Seq(),
			managePermissions = Seq(),
			allRelationshipTypes = Seq(),
			relationshipTypesMap = Map()
		)

		val mav: Mav = controller.home(command, None)
		mav.viewName should be("redirect:/attendance/profile")

	}}

	@Test
	def onlyOneViewPermissionRedirectedToViewDepartment(){ new Fixture{
		info = HomeInformation(
			hasProfile = false,
			viewPermissions = Seq(new Department().tap(_.code=departmentCode)),
			managePermissions = Seq(),
			allRelationshipTypes = Seq(),
			relationshipTypesMap = Map()
		)

		val mav: Mav = controller.home(command, Option(AcademicYear(2016)))
		mav.viewName should be(s"redirect:/attendance/view/$departmentCode/${AcademicYear(2016).startYear.toString}")

	}}

	@Test
	def onlyOneManagePermissionRedirectedToManageDepartment(){ new Fixture{
		info = HomeInformation(
			hasProfile = false,
			viewPermissions = Seq(),
			managePermissions = Seq(new Department().tap(_.code=departmentCode)),
			allRelationshipTypes = Seq(),
			relationshipTypesMap = Map()
		)

		val mav: Mav = controller.home(command, None)
		mav.viewName should be(s"redirect:/attendance/manage/$departmentCode/${AcademicYear(2016).startYear.toString}")

	}}

	@Test
	def noPermissions(){ withUser("cusfal") { new Fixture{
		info = HomeInformation(
			hasProfile = false,
			viewPermissions = Seq(),
			managePermissions = Seq(),
			allRelationshipTypes = Seq(),
			relationshipTypesMap = Map()
		)

		val mav: Mav = controller.home(command, None)
		mav.viewName should be("attendance/home")

	}}}

	@Test
	def viewPermissionsAndProfileShowHome(){ withUser("cusfal") { new Fixture{
		info = HomeInformation(
			hasProfile = true,
			viewPermissions = Seq(new Department().tap(_.code=departmentCode)),
			managePermissions = Seq(),
			allRelationshipTypes = Seq(),
			relationshipTypesMap = Map()
		)

		val mav: Mav = controller.home(command, None)
		mav.viewName should be("attendance/home")

	}}}

	@Test
	def managePermissionsAndProfileShowHome(){ withUser("cusfal") { new Fixture{
		info = HomeInformation(
			hasProfile = true,
			viewPermissions = Seq(),
			managePermissions = Seq(new Department().tap(_.code=departmentCode)),
			allRelationshipTypes = Seq(),
			relationshipTypesMap = Map()
		)

		val mav: Mav = controller.home(command, None)
		mav.viewName should be("attendance/home")

	}}}

	@Test
	def relationshipsAndProfileShowHome(){ withUser("cusfal") { new Fixture{
		info = HomeInformation(
			hasProfile = true,
			viewPermissions = Seq(),
			managePermissions = Seq(),
			allRelationshipTypes = Seq(),
			relationshipTypesMap = Map(relationshipType -> true)
		)

		val mav: Mav = controller.home(command, None)
		mav.viewName should be("attendance/home")

	}}}


}
