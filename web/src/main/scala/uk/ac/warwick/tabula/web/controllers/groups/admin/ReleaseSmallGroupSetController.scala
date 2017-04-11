package uk.ac.warwick.tabula.web.controllers.groups.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.commands.groups.admin.{ReleaseGroupSetCommandImpl, ReleaseSmallGroupSetCommand}
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.{ViewGroup, ViewModule, ViewSet}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController

import scala.collection.JavaConverters._

@RequestMapping(Array("/groups/admin/module/{module}/groups/{set}/release"))
@Controller
class ReleaseSmallGroupSetController extends GroupsController {

	@ModelAttribute("releaseGroupSetCommand") def getReleaseGroupSetCommand(@PathVariable set: SmallGroupSet): ReleaseSmallGroupSetCommand = {
		new ReleaseGroupSetCommandImpl(Seq(set), user.apparentUser)
	}

	@RequestMapping
	def form(@ModelAttribute("releaseGroupSetCommand") cmd: ReleaseSmallGroupSetCommand): Mav =
		Mav("groups/admin/groups/release").noLayoutIf(ajax)


	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("releaseGroupSetCommand") cmd: ReleaseSmallGroupSetCommand): Mav = {
		val updatedSet = cmd.apply() match {
			case releasedSet :: Nil => releasedSet.set
			case _ => throw new IllegalStateException("Received multiple updated sets from a single update operation!")
		}
		val groupSetItem = ViewSet(updatedSet, ViewGroup.fromGroups(updatedSet.groups.asScala.sorted), GroupsViewModel.Tutor)
		val moduleItem = ViewModule(updatedSet.module, Seq(groupSetItem), canManageGroups = true)
		Mav("groups/admin/groups/single_groupset",
			"groupsetItem" -> groupSetItem,
			"moduleItem" -> moduleItem,
			"notificationSentMessage" -> cmd.describeOutcome).noLayoutIf(ajax) // should be AJAX, otherwise you'll just get a terse success response.
	}
}
