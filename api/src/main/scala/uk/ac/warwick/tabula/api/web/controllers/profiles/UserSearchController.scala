package uk.ac.warwick.tabula.api.web.controllers.profiles

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.commands.ViewViewableCommand
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/v1/usersearch"))
class UserSearchController extends ApiController
  with GetUsersApi with AutowiringProfileServiceComponent {

  final override def onPreRequest(): Unit = {
    session.enableFilter(Member.ActiveOnlyFilter)
  }

  @RequestMapping(path = Array("/undergraduates"), method = Array(GET), produces = Array("application/json"))
  def undergraduates(
    @ModelAttribute("getCommand") command: ViewViewableCommand[PermissionsTarget],
    @RequestParam(required = false) level: String
  ): Mav = {
    getMav(
      Option(level) match {
        case Some("f") | Some("F") => profileService.findFinalistUndergraduateUsercodes()
        case Some(l) => profileService.findUndergraduatesUsercodesByLevel(l)
        case None => profileService.findUndergraduateUsercodes()
      }
    )
  }
}


trait GetUsersApi {

  self: ApiController with ProfileServiceComponent =>

  @ModelAttribute("getCommand")
  def getCommand(): ViewViewableCommand[PermissionsTarget] =
    new ViewViewableCommand(Permissions.Profiles.ViewSearchResults, PermissionsTarget.Global)

  def getMav(usercodes: Seq[String]): Mav = {
    Mav(new JSONView(Map(
      "success" -> true,
      "status" -> "ok",
      "usercodes" -> usercodes
    )))
  }
}
