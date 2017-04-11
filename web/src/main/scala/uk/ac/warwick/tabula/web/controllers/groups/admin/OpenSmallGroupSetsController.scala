package uk.ac.warwick.tabula.web.controllers.groups.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupAllocationMethod, SmallGroupSet, SmallGroupSetSelfSignUpState}
import uk.ac.warwick.tabula.commands.groups.admin.OpenSmallGroupSetCommand
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.services.AutowiringSmallGroupServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

@Controller
@RequestMapping(Array("/groups/admin/department/{department}/{academicYear}/groups/selfsignup/{action}"))
class OpenSmallGroupSetsController extends GroupsController with AutowiringSmallGroupServiceComponent {

	@ModelAttribute("setList") def newViewModelOpen(
		@PathVariable department: Department,
		@PathVariable action: SmallGroupSetSelfSignUpState
	): GroupsetListViewModel = {
		new GroupsetListViewModel((user, sets) => OpenSmallGroupSetCommand(department, sets, user, action), action)
	}

	@RequestMapping
	def form(
		@ModelAttribute("setList") model: GroupsetListViewModel,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		showFlash: Boolean = false
	): Mav = {
		val groupSets = smallGroupService.getSmallGroupSets(department, academicYear)
			.filter(groupset => groupset.allocationMethod == SmallGroupAllocationMethod.StudentSignUp)
		Mav("groups/admin/groups/bulk-open",
			"department" -> department,
			"groupSets" -> groupSets,
			"showFlash" -> showFlash,
			"setState" -> model.getName
		).crumbs(Breadcrumbs.Department(department, academicYear))
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("setList") model: GroupsetListViewModel, @PathVariable department: Department, @PathVariable academicYear: AcademicYear): Mav = {
		model.applyCommand(user.apparentUser)
		Redirect(Routes.admin.selfsignup(department, academicYear, model.getName), "batchOpenSuccess" -> true)
	}

	class GroupsetListViewModel(val createCommand: (User, Seq[SmallGroupSet]) => Appliable[Seq[SmallGroupSet]], var action: SmallGroupSetSelfSignUpState) {
		var checkedGroupsets: JList[SmallGroupSet] = JArrayList()

		def getName: String = action.name

		def applyCommand(user: User): Seq[SmallGroupSet] = {
			createCommand(user, checkedGroupsets.asScala).apply()
		}
	}

}