package uk.ac.warwick.tabula.web.controllers.groups.admin

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups.admin.{CopySmallGroupEventCommand, CopySmallGroupEventCommandState}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.web.Mav

import javax.validation.Valid


abstract class AbstractCopySmallGroupEventController extends SmallGroupEventsController {

  type CopySmallGroupEventCommand = Appliable[SmallGroupEvent] with CopySmallGroupEventCommandState

  @ModelAttribute("copySmallGroupEventCommand") def cmd(
    @PathVariable module: Module,
    @PathVariable("smallGroupSet") set: SmallGroupSet,
    @PathVariable("smallGroupEvent") event: SmallGroupEvent,
  ): CopySmallGroupEventCommand =
    CopySmallGroupEventCommand.copy(module, event)

  protected def cancelUrl(set: SmallGroupSet): String

  @RequestMapping
  def form(@ModelAttribute("copySmallGroupEventCommand") cmd: CopySmallGroupEventCommand): Mav = {
    Mav("groups/admin/groups/events/copy", "cancelUrl" -> cancelUrl(cmd.set))
      .crumbs(Breadcrumbs.Department(cmd.set.department, cmd.academicYear), Breadcrumbs.ModuleForYear(cmd.module, cmd.academicYear))
  }

  protected def submit(cmd: CopySmallGroupEventCommand, errors: Errors, route: String): Mav = {
    if (errors.hasErrors) form(cmd)
    else {
      cmd.apply()
      RedirectForce(route)
    }
  }

}


@RequestMapping(Array("/groups/admin/module/{module}/groups/new/{smallGroupSet}/events/{smallGroupEvent}/copy"))
@Controller
class CreateSmallGroupSetCopyEventController extends AbstractCopySmallGroupEventController {

  override def cancelUrl(set: SmallGroupSet): String = Routes.admin.createAddEvents(set)

  @RequestMapping(method = Array(POST))
  def saveAndExit(@Valid @ModelAttribute("copySmallGroupEventCommand") cmd: CopySmallGroupEventCommand, errors: Errors, @PathVariable("smallGroupEvent") event: SmallGroupEvent): Mav =
    submit(cmd, errors, Routes.admin.createAddEvents(event.groupSet))

}

@RequestMapping(Array("/groups/admin/module/{module}/groups/edit/{smallGroupSet}/events/{smallGroupEvent}/copy"))
@Controller
class EditSmallGroupSetCopyEventController extends AbstractCopySmallGroupEventController {

  override def cancelUrl(set: SmallGroupSet): String = Routes.admin.editAddEvents(set)

  @RequestMapping(method = Array(POST))
  def saveAndExit(@Valid @ModelAttribute("copySmallGroupEventCommand") cmd: CopySmallGroupEventCommand, errors: Errors, @PathVariable("smallGroupEvent") event: SmallGroupEvent): Mav =
    submit(cmd, errors, Routes.admin.editAddEvents(event.groupSet))

}
