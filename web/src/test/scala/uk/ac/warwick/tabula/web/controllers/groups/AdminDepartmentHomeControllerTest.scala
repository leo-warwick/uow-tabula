package uk.ac.warwick.tabula.web.controllers.groups

import org.joda.time.DateTime
import org.joda.time.base.BaseDateTime
import org.mockito.Mockito._
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups.admin.{AdminSmallGroupsHomeCommandState, AdminSmallGroupsHomeInformation}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.ViewSetWithProgress
import uk.ac.warwick.tabula.permissions.{Permission, PermissionsTarget}
import uk.ac.warwick.tabula.services.{SecurityService, TermService}
import uk.ac.warwick.tabula.web.controllers.groups.admin.GroupsAdminDepartmentController

import scala.collection.immutable.ListMap

class AdminDepartmentHomeControllerTest extends TestBase with Mockito{

	def createController: GroupsAdminDepartmentController = {
		val controller = new GroupsAdminDepartmentController() {
			override val termService: TermService = mock[TermService]
		}
		controller.securityService = smartMock[SecurityService]
		controller.features = emptyFeatures
		when(controller.securityService.can(any[CurrentUser], any[Permission], any[PermissionsTarget])).thenReturn{true}
		when(controller.termService.getAcademicWeeksForYear(any[BaseDateTime])).thenReturn(Nil)
		controller
	}

	@Test
	def reportsWhenNotAllGroupsetsAreReleased(){new SmallGroupFixture {
		withUser("test") {
			groupSet1.releasedToStudents = true
			groupSet1.releasedToTutors = false

			val cmd = new Appliable[AdminSmallGroupsHomeInformation] with AdminSmallGroupsHomeCommandState {
				val department: Department = Fixtures.department("in")
				def user = NoCurrentUser()
				def academicYear: AcademicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

				def apply(): AdminSmallGroupsHomeInformation = {
					AdminSmallGroupsHomeInformation(canAdminDepartment = false, Seq(groupSet1.module), Seq(ViewSetWithProgress(groupSet1, Nil, GroupsViewModel.Tutor, null, None, ListMap())), Nil)
				}
			}

			val mav = createController.adminDepartment(cmd, department, currentUser)
			mav.map.get("hasUnreleasedGroupsets") match{
				case Some(v: Boolean) => v should be {true}
				case _ => fail()
			}
		}}
	}

	@Test
	def reportsWhenAllGroupsetsAreReleased(){new SmallGroupFixture {
		withUser("test"){

			groupSet1.releasedToStudents = true
			groupSet1.releasedToTutors = true

			val cmd = new Appliable[AdminSmallGroupsHomeInformation] with AdminSmallGroupsHomeCommandState {
				val department: Department = Fixtures.department("in")
				def user = NoCurrentUser()
				def academicYear: AcademicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

				def apply(): AdminSmallGroupsHomeInformation = {
					AdminSmallGroupsHomeInformation(canAdminDepartment = false, Seq(groupSet1.module), Seq(ViewSetWithProgress(groupSet1, Nil, GroupsViewModel.Tutor, null, None, ListMap())), Nil)
				}
			}

			val mav = createController.adminDepartment(cmd, department, currentUser)
			mav.map.get("hasUnreleasedGroupsets") match {
				case Some(v: Boolean) => v should be {false}
				case _ => fail()
			}

		}}

	}

}
