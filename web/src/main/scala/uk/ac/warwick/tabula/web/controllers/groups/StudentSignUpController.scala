package uk.ac.warwick.tabula.web.controllers.groups

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.services.AutowiringSmallGroupServiceComponent
import uk.ac.warwick.tabula.web.views.JSONView


@Controller
@RequestMapping(value = Array("/groups/module/{module_code}/groups/{set_id}/signup"))
class StudentSignUpController extends GroupsController {

  validatesSelf[AllocateSelfToGroupValidator]

  @ModelAttribute("command")
  def command(@PathVariable("set_id") groupSet: SmallGroupSet, user: CurrentUser): Appliable[SmallGroupSet] = {
    AllocateSelfToGroupCommand(user, groupSet)
  }

  @RequestMapping(method = Array(GET, HEAD))
  def get(): Mav = Redirect(Routes.home)

  @RequestMapping(method = Array(POST))
  def signUp(@Valid @ModelAttribute("command") command: Appliable[SmallGroupSet], errors: Errors): Mav = {
    if (errors.hasErrors) {
      Mav("groups/signup/problems", "action" -> "signup")
    } else {
      val set = command.apply()
      Redirect(Routes.homeForYear(set.academicYear))
    }
  }
}

@Controller
@RequestMapping(value = Array("/groups/module/{module_code}/groups/{set_id}/signuptimetableclashinfo"))
class StudentSignUpTimetableClashController extends GroupsController with AutowiringSmallGroupServiceComponent {

  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def ajaxClashTimetableInfo(@RequestParam group: SmallGroup, user: CurrentUser): Mav = {
    val clash = smallGroupService.doesTimetableClashesForStudent(group, user.apparentUser)
    Mav(new JSONView(Map("clash" -> clash)))
  }
}

@RequestMapping(value = Array("/groups/module/{module_code}/groups/{set_id}/leave"))
@Controller
class StudentUnSignUpController extends GroupsController {

  validatesSelf[DeallocateSelfFromGroupValidator]

  @ModelAttribute("command")
  def command(@PathVariable("set_id") groupSet: SmallGroupSet, user: CurrentUser): Appliable[SmallGroupSet] = {
    DeallocateSelfFromGroupCommand(user, groupSet)
  }

  @RequestMapping(method = Array(GET, HEAD))
  def get(): Mav = Redirect(Routes.home)

  @RequestMapping(method = Array(POST))
  def signUp(@Valid @ModelAttribute("command") command: Appliable[SmallGroupSet], errors: Errors): Mav = {
    if (errors.hasErrors) {
      Mav("groups/signup/problems", "action" -> "leave")
    } else {
      val set = command.apply()
      Redirect(Routes.homeForYear(set.academicYear))
    }
  }
}
