package uk.ac.warwick.tabula.web.controllers.profiles.relationships

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.ViewViewableCommand
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController

/**
 * Displays the students on a small group event of which you are a tutor.
 */
@Controller
@RequestMapping(value=Array("/profiles/groups/{smallGroup}/view"))
class ViewSmallGroupController extends ProfilesController with AutowiringProfileServiceComponent {

	@ModelAttribute
	def command(@PathVariable smallGroup: SmallGroup) =
		new ViewViewableCommand(Permissions.SmallGroups.Read, mandatory(smallGroup))

	@RequestMapping(method = Array(GET, HEAD))
	def show(@PathVariable smallGroup: SmallGroup): Mav = {
		val members = profileService.getAllMembersWithUniversityIds(smallGroup.students.users.map(_.getWarwickId))
		Mav("profiles/groups/view",
			"smallGroup" -> smallGroup,
			"tutees" -> members.sortBy { student =>  (student.lastName, student.firstName) }
		)
	}

}
