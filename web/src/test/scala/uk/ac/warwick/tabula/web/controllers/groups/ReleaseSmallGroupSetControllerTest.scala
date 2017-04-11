package uk.ac.warwick.tabula.web.controllers.groups

import org.mockito.Mockito.when
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.commands.groups.admin.{ReleaseSmallGroupSetCommand, ReleasedSmallGroupSet}
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.{ViewGroup, ViewModule, ViewSet}
import uk.ac.warwick.tabula.web.controllers.groups.admin.{ReleaseAllSmallGroupSetsController, ReleaseSmallGroupSetController}
import uk.ac.warwick.tabula.{AcademicYear, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

class ReleaseSmallGroupSetControllerTest extends TestBase with Mockito {

  @Test
  def createsCommandObject() {
    withUser("test") {
      val set = new SmallGroupSet
      val controller = new ReleaseSmallGroupSetController
      controller.getReleaseGroupSetCommand(set) should not be null
    }
  }

  @Test
  def showsForm() {
    withUser("test") {
      val controller = new ReleaseSmallGroupSetController
      val cmd = mock[ReleaseSmallGroupSetCommand]
      controller.form(cmd).viewName should be("groups/admin/groups/release")
      controller.form(cmd).map should be(Map())
    }
  }

  @Test
  def invokesCommand() {
    withUser("test") {
      val controller = new ReleaseSmallGroupSetController
			val cmd = mock[ReleaseSmallGroupSetCommand]
      when(cmd.apply()).thenReturn(Seq(ReleasedSmallGroupSet(new SmallGroupSet(), releasedToStudents = true, releasedToTutors = true)))

      controller.submit(cmd).viewName should be("groups/admin/groups/single_groupset")

      verify(cmd, times(1)).apply()
    }
  }

	@Test
	def returnsContextAsExpected() {
		withUser("test") {
			val controller = new ReleaseSmallGroupSetController
			val cmd = mock[ReleaseSmallGroupSetCommand]
			val set = new SmallGroupSet()
			set.module = new Module()
			cmd.apply() returns Seq(ReleasedSmallGroupSet(set, releasedToStudents = true, releasedToTutors = true))
			cmd.describeOutcome returns Some("hello")
			val context = controller.submit(cmd).map

			val expectedViewSet = new ViewSet(set, ViewGroup.fromGroups(set.groups.asScala), GroupsViewModel.Tutor)
			context("notificationSentMessage") should be(Some("hello"))
			context("groupsetItem") should be(expectedViewSet)
			context("moduleItem") should be(new ViewModule(set.module,Seq(expectedViewSet),true))
		}
	}


	@Test
  def moduleListViewModelConvertsModulesToGroupSets() {

		def newGroupSetFor2014 = new SmallGroupSet { academicYear = AcademicYear(2014)}
		def newGroupSetFor2015 = new SmallGroupSet { academicYear = AcademicYear(2015)}

    val mod1 = new Module()
    mod1.groupSets = Seq(newGroupSetFor2014, newGroupSetFor2014).asJava
    val mod2 = new Module()
    mod2.groupSets = Seq(newGroupSetFor2014, newGroupSetFor2014, newGroupSetFor2015).asJava

    val viewModel = new ReleaseAllSmallGroupSetsController().newViewModel(AcademicYear(2014))
    viewModel.checkedModules = JArrayList(mod1, mod2)

    viewModel.smallGroupSets().size should be(4) // ignore groups from previous years
  }

  @Test
  def batchControllerCreatesViewModel() {
    new ReleaseAllSmallGroupSetsController().newViewModel(AcademicYear(2014)) should not be null
  }

  @Test
  def batchControllerShowsFormWithDepartment() {
    val controller = new ReleaseAllSmallGroupSetsController()
    val model = controller.newViewModel(AcademicYear(2014))
    val department = new Department
    department.code = "xyz"
    val mav = controller.form(model, department, AcademicYear(2014))
    mav.toModel.get("department") should be(Some(department))
    mav.viewName should be("groups/admin/groups/bulk-release")
  }


  @Test
  def batchControllerInvokesCommand() {
    withUser("test") {
      val dept = new Department {
        code = "xx"
      }
			val year = AcademicYear(2014)
      val controller = new ReleaseAllSmallGroupSetsController()
      val command = mock[Appliable[Seq[ReleasedSmallGroupSet]]]
      val model = mock[controller.ModuleListViewModel]
      when(model.createCommand(any[User])).thenReturn(command)
      controller.submit(model, dept, year)
      verify(command, times(1)).apply()
    }
  }

  @Test
  def batchControllerReturnsNewFormWithFlashMessage(){
    withUser("test") {

      val controller = new ReleaseAllSmallGroupSetsController()
      val command = mock[Appliable[Seq[ReleasedSmallGroupSet]]]
      val model = mock[controller.ModuleListViewModel]
      val department = new Department
      department.code = "xyz"
			val year = AcademicYear(2014)
      when(model.createCommand(any[User])).thenReturn(command)

      val formView = controller.submit(model, department, year)

      formView.viewName should be("redirect:/groups/admin/department/xyz/2014/groups/release")
      formView.map.get("batchReleaseSuccess") should be(Some(true))
    }
  }
}
