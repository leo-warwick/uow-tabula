package uk.ac.warwick.tabula.web.controllers.profiles

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Appliable, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data.model.MemberUserType._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.commands.profiles._
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.web.Mav

@Controller class ProfilesHomeController extends ProfilesController with ChecksAgent {

	var departmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]

	@ModelAttribute("searchProfilesCommand") def searchProfilesCommand: SearchProfilesCommandInternal with ComposableCommand[Seq[Member]] with Unaudited with SearchProfilesCommandPermissions =
		restricted(SearchProfilesCommand(currentMember, user)).orNull

	@ModelAttribute("command")
	def createCommand(user: CurrentUser) = ProfilesHomeCommand(user, optionalCurrentMember)

	@RequestMapping(Array("/profiles")) def home(@ModelAttribute("command") cmd: Appliable[ProfilesHomeInformation]): Mav = {

		if (!user.isPGR && !isAgent(user.universityId) && optionalCurrentMember.exists(_.userType == Student)) {
			Redirect(Routes.Profile.identity(currentMember))
		} else {
			val info = cmd.apply()

			Mav("profiles/home/view",
				"relationshipTypesMap" -> info.relationshipTypesMap,
				"relationshipTypesMapById" -> info.relationshipTypesMap.map { case (k, v) => (k.id, v) },
				"universityId" -> user.universityId,
				"isPGR" -> user.isPGR,
				"smallGroups" -> info.smallGroups,
				"adminDepartments" -> info.adminDepartments,
				"searchDepartments" -> (departmentService.departmentsWithPermission(user, Permissions.Profiles.Search) ++ user.profile.map { _.homeDepartment }.toSet)
			)
		}
	}

	@RequestMapping(Array("/profiles/view")) def redirectHome() = Redirect(Routes.home)
}