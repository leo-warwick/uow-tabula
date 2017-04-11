package uk.ac.warwick.tabula.web.controllers.groups

import uk.ac.warwick.tabula.{Features, TestBase}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel

class TutorHomeControllerTest extends TestBase {

	@Test def controllerShowsYourGroups() {
		val command = new Appliable[Map[Module, Map[SmallGroupSet, Seq[SmallGroup]]]] {
			def apply() = Map()
		}
		val controller = new GroupsTutorHomeController
		controller.features = Features.empty
		val mav = controller.listModules(command, None, null)

		mav.map("data") should be (GroupsViewModel.ViewModules(Nil, canManageDepartment=false))
	}

}
