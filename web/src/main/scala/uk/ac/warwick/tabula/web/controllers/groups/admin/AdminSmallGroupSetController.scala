package uk.ac.warwick.tabula.web.controllers.groups.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Appliable, ViewViewableCommand}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.services.groups.AutowiringSmallGroupSetWorkflowServiceComponent
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.{SetProgress, Tutor, ViewGroup, ViewSetWithProgress}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController

import scala.collection.JavaConverters._

@Controller
@RequestMapping(value=Array("/groups/admin/module/{module}/groups/{smallGroupSet}"))
class AdminSmallGroupSetController extends GroupsController with AutowiringSmallGroupSetWorkflowServiceComponent {

	hideDeletedItems

	@ModelAttribute("adminCommand") def command(@PathVariable module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet): ViewViewableCommand[SmallGroupSet] = {
		mustBeLinked(mandatory(set), mandatory(module))
		new ViewViewableCommand(Permissions.Module.ManageSmallGroups, set)
	}

	@RequestMapping
	def adminSingleSet(@ModelAttribute("adminCommand") cmd: Appliable[SmallGroupSet], user: CurrentUser): Mav = {
		val set = cmd.apply()

		val progress = smallGroupSetWorkflowService.progress(set)

		val setView = ViewSetWithProgress(
			set = set,
			groups = ViewGroup.fromGroups(set.groups.asScala.sorted),
			viewerRole = Tutor,
			progress = SetProgress(progress.percentage, progress.cssClass, progress.messageCode),
			nextStage = progress.nextStage,
			stages = progress.stages
		)

		val model = Map(
			"set" -> setView
		)

		if (ajax) Mav("groups/admin/module/single_set-noLayout", model).noLayout()
		else Mav("groups/admin/module/single_set", model).crumbs(Breadcrumbs.Department(set.module.adminDepartment, set.academicYear), Breadcrumbs.Module(set.module))
	}

}
