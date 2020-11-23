package uk.ac.warwick.tabula.api.web.controllers

import javax.validation.Valid
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.permissions.GrantPermissionsCommand
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}

@Controller
@RequestMapping("/v1/permissions/module/{module}")
class ModulePermissionsApiController extends ApiController {
  validatesSelf[SelfValidating]

  @ModelAttribute("addSingleCommand")
  def addSingleCommandModel(@PathVariable module: Module): GrantPermissionsCommand.Command[Module] =
    GrantPermissionsCommand(mandatory(module))

  @RequestMapping(
    method = Array(PUT),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE),
    produces = Array("application/json")
  )
  def addRole(@Valid @ModelAttribute("addSingleCommand") command: GrantPermissionsCommand.Command[Module], errors: Errors): Mav = {
    if (errors.hasErrors) {
      Mav(new JSONErrorView(errors))
    } else {
      command.apply()
      Mav(new JSONView(Map(
        "success" -> true
      )))
    }
  }
}
