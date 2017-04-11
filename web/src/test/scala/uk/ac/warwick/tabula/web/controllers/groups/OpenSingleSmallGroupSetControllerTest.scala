package uk.ac.warwick.tabula.web.controllers.groups

import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupAllocationMethod, SmallGroupSet, SmallGroupSetSelfSignUpState}
import uk.ac.warwick.tabula.web.controllers.groups.admin.OpenSmallGroupSetController
import uk.ac.warwick.tabula.{Mockito, TestBase}

class OpenSingleSmallGroupSetControllerTest extends TestBase with Mockito {
	val controller = new OpenSmallGroupSetController
	val module = new Module
	val set = new SmallGroupSet
	set.allocationMethod = SmallGroupAllocationMethod.StudentSignUp


	@Test
	def createsCommand() {
		withUser("test") {
			val command = controller.getOpenGroupSetCommand(module, set, SmallGroupSetSelfSignUpState.Open)
			command.singleSetToOpen should be(set)
		}
	}

	@Test
	def showsForm() {
		val mockCommand = mock[Appliable[Seq[SmallGroupSet]]]
		controller.form(mockCommand).viewName should be("groups/admin/groups/open")
	}

	@Test
	def submitCallsApply() {
		val mockCommand = mock[Appliable[Seq[SmallGroupSet]]]
		controller.submit(mockCommand)
		verify(mockCommand, times(1)).apply()
	}

	@Test
	def submitShowsSuccessView() {
		val mockCommand = mock[Appliable[Seq[SmallGroupSet]]]
		controller.submit(mockCommand).viewName should be ("ajax_success")
	}

}
