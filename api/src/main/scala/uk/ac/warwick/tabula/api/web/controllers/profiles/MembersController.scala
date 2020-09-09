package uk.ac.warwick.tabula.api.web.controllers.profiles

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{GetMapping, ModelAttribute, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.api.commands.profiles.{MemberSearchCommand, MemberSearchCommandInternal, MemberSearchCommandRequest}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.helpers.{APIFieldRestriction, MemberToJsonConverter}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Department, Member}
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{AutowiringScalaFreemarkerConfigurationComponent, JSONErrorView, JSONView}

import scala.jdk.CollectionConverters._

@Controller
@RequestMapping(Array("/v1/members"))
class MembersController extends ApiController with AutowiringProfileServiceComponent
  with AutowiringScalaFreemarkerConfigurationComponent
  with MemberToJsonConverter {

  validatesSelf[SelfValidating]

  type Command = Appliable[Seq[Member]] with MemberSearchCommandRequest with MemberSearchCommandInternal

  final override def onPreRequest(): Unit = {
    session.enableFilter(Member.ActiveOnlyFilter)
    session.enableFilter(Member.FreshOnlyFilter)
  }

  @ModelAttribute("command")
  def command(@RequestParam(name = "department", required = false) departments: JList[Department], user: CurrentUser): Command =
    MemberSearchCommand(Option(departments).map(_.asScala.toSeq).getOrElse(Seq[Department]()), user)

  @GetMapping(produces = Array("application/json"))
  def search(@Valid @ModelAttribute("command") command: Command, errors: Errors): Mav = {
    if (errors.hasErrors) {
      Mav(new JSONErrorView(errors))
    } else {
      val result = command.apply()
      val members = result.map(m => jsonMemberObject(m, APIFieldRestriction.restriction("member", command.fields)))

      Mav(new JSONView(Map(
        "success" -> true,
        "status" -> "ok",
        "members" -> members,
        "offset" -> command.offset,
        "limit" -> command.limit,
        "total" -> command.total
      )))
    }
  }

}
