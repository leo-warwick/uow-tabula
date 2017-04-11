package uk.ac.warwick.tabula.web.controllers.groups.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.commands.groups.admin.ArchiveSmallGroupSetCommand
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController

@RequestMapping(Array("/groups/admin/module/{module}/groups/{set}/archive"))
@Controller
class ArchiveSmallGroupSetController extends GroupsController {

	@ModelAttribute("smallGroupSet") def set(@PathVariable set: SmallGroupSet): SmallGroupSet = set

	@ModelAttribute("archiveSmallGroupSetCommand") def cmd(@PathVariable module: Module, @PathVariable set: SmallGroupSet) =
		new ArchiveSmallGroupSetCommand(module, set)

	@RequestMapping
	def form(cmd: ArchiveSmallGroupSetCommand): Mav =
		Mav("groups/admin/groups/archive").noLayoutIf(ajax)

	@RequestMapping(method = Array(POST))
	def submit(cmd: ArchiveSmallGroupSetCommand): Mav = {
		cmd.apply()
		if (ajax)
			Mav("ajax_success").noLayout()
		else
			Redirect(Routes.home)
	}

}
