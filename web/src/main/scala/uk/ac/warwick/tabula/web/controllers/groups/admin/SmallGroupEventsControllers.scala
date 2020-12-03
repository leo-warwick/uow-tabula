package uk.ac.warwick.tabula.web.controllers.groups.admin

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.groups.admin.{ModifySmallGroupEventCommand, ModifySmallGroupEventCommandState, UpdateSmallGroupEventFromExternalSystemCommand}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.services.timetables.AutowiringScientiaTimetableFetchingServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController
import uk.ac.warwick.tabula.{AcademicPeriod, AcademicYear}

trait SmallGroupEventsController extends GroupsController {
  validatesSelf[SelfValidating]

  @ModelAttribute("allDays") def allDays: Seq[DayOfWeek with Product] = DayOfWeek.members

  case class NamedTerm(name: String, term: AcademicPeriod, weekRange: WeekRange)

  @ModelAttribute("allTerms") def allTerms(@PathVariable("smallGroupSet") set: SmallGroupSet): Seq[NamedTerm] = {
    val year = Option(set.academicYear).getOrElse(AcademicYear.now())
    year.weeks
      .map { case (weekNumber, week) =>
        (weekNumber, week.period)
      }
      .groupBy(_._2)
      .map { case (term, weekNumbersAndTerms) =>
        (term, WeekRange(weekNumbersAndTerms.keys.min, weekNumbersAndTerms.keys.max))
      }
      .toSeq
      .sortBy { case (_, weekRange) => weekRange.minWeek }
      .map { case (term, weekRange) => NamedTerm(term.periodType.toString, term, weekRange) }
  }

  @ModelAttribute("allDeliveryMethods")
  def allDeliveryMethods: Seq[EventDeliveryMethod] = EventDeliveryMethod.values

  @ModelAttribute("allOnlinePlatforms")
  def allOnlinePlatforms: Seq[OnlinePlatform] = OnlinePlatform.values
}

abstract class AbstractCreateSmallGroupEventController extends SmallGroupEventsController {

  type CreateSmallGroupEventCommand = Appliable[SmallGroupEvent] with ModifySmallGroupEventCommandState

  @ModelAttribute("createSmallGroupEventCommand") def cmd(
    @PathVariable module: Module,
    @PathVariable("smallGroupSet") set: SmallGroupSet,
    @PathVariable("smallGroup") group: SmallGroup
  ): CreateSmallGroupEventCommand =
    ModifySmallGroupEventCommand.create(module, set, group)

  protected def cancelUrl(set: SmallGroupSet): String

  @RequestMapping
  def form(@ModelAttribute("createSmallGroupEventCommand") cmd: CreateSmallGroupEventCommand): Mav = {
    Mav("groups/admin/groups/events/new", "cancelUrl" -> cancelUrl(cmd.set))
      .crumbs(Breadcrumbs.Department(cmd.set.department, cmd.academicYear), Breadcrumbs.ModuleForYear(cmd.module, cmd.academicYear))
  }

  protected def submit(cmd: CreateSmallGroupEventCommand, errors: Errors, route: String): Mav = {
    if (errors.hasErrors) form(cmd)
    else {
      cmd.apply()
      RedirectForce(route)
    }
  }

}

@RequestMapping(Array("/groups/admin/module/{module}/groups/new/{smallGroupSet}/events/{smallGroup}/new"))
@Controller
class CreateSmallGroupSetCreateEventController extends AbstractCreateSmallGroupEventController {

  override def cancelUrl(set: SmallGroupSet): String = Routes.admin.createAddEvents(set)

  @RequestMapping(method = Array(POST))
  def saveAndExit(@Valid @ModelAttribute("createSmallGroupEventCommand") cmd: CreateSmallGroupEventCommand, errors: Errors, @PathVariable("smallGroupSet") set: SmallGroupSet): Mav =
    submit(cmd, errors, Routes.admin.createAddEvents(set))

}

@RequestMapping(Array("/groups/admin/module/{module}/groups/edit/{smallGroupSet}/events/{smallGroup}/new"))
@Controller
class EditSmallGroupSetCreateEventController extends AbstractCreateSmallGroupEventController {

  override def cancelUrl(set: SmallGroupSet): String = Routes.admin.editAddEvents(set)

  @RequestMapping(method = Array(POST))
  def saveAndExit(@Valid @ModelAttribute("createSmallGroupEventCommand") cmd: CreateSmallGroupEventCommand, errors: Errors, @PathVariable("smallGroupSet") set: SmallGroupSet): Mav =
    submit(cmd, errors, Routes.admin.editAddEvents(set))

}

abstract class AbstractEditSmallGroupEventController extends SmallGroupEventsController
  with AutowiringScientiaTimetableFetchingServiceComponent
  with SyllabusPlusEventCountForModule {

  type EditSmallGroupEventCommand = Appliable[SmallGroupEvent] with ModifySmallGroupEventCommandState

  @ModelAttribute("editSmallGroupEventCommand") def cmd(
    @PathVariable module: Module,
    @PathVariable("smallGroupSet") set: SmallGroupSet,
    @PathVariable("smallGroup") group: SmallGroup,
    @PathVariable("smallGroupEvent") event: SmallGroupEvent
  ): EditSmallGroupEventCommand =
    ModifySmallGroupEventCommand.edit(module, set, group, event)

  protected def cancelUrl(set: SmallGroupSet): String

  @RequestMapping
  def form(@ModelAttribute("editSmallGroupEventCommand") cmd: EditSmallGroupEventCommand): Mav = {
    Mav("groups/admin/groups/events/edit", "cancelUrl" -> cancelUrl(cmd.set))
      .crumbs(Breadcrumbs.Department(cmd.set.department, cmd.academicYear), Breadcrumbs.ModuleForYear(cmd.module, cmd.academicYear))
  }

  protected def submit(cmd: EditSmallGroupEventCommand, errors: Errors, route: String): Mav = {
    if (errors.hasErrors) form(cmd)
    else {
      cmd.apply()
      Redirect(route)
    }
  }

}

@RequestMapping(Array("/groups/admin/module/{module}/groups/new/{smallGroupSet}/events/{smallGroup}/edit/{smallGroupEvent}"))
@Controller
class CreateSmallGroupSetEditEventController extends AbstractEditSmallGroupEventController {

  override def cancelUrl(set: SmallGroupSet): String = Routes.admin.createAddEvents(set)

  @RequestMapping(method = Array(POST))
  def saveAndExit(@Valid @ModelAttribute("editSmallGroupEventCommand") cmd: EditSmallGroupEventCommand, errors: Errors, @PathVariable("smallGroupSet") set: SmallGroupSet): Mav =
    submit(cmd, errors, Routes.admin.createAddEvents(set))

  @ModelAttribute("is_edit_set") def isEditingSmallGroupSet = false

}

@RequestMapping(Array("/groups/admin/module/{module}/groups/edit/{smallGroupSet}/events/{smallGroup}/edit/{smallGroupEvent}"))
@Controller
class EditSmallGroupSetEditEventController extends AbstractEditSmallGroupEventController {

  override def cancelUrl(set: SmallGroupSet): String = Routes.admin.editAddEvents(set)

  @RequestMapping(method = Array(POST))
  def saveAndExit(@Valid @ModelAttribute("editSmallGroupEventCommand") cmd: EditSmallGroupEventCommand, errors: Errors, @PathVariable("smallGroupSet") set: SmallGroupSet): Mav =
    submit(cmd, errors, Routes.admin.editAddEvents(set))

  @ModelAttribute("is_edit_set") def isEditingSmallGroupSet = true

}

abstract class AbstractUpdateSmallGroupEventFromExternalSystemController extends GroupsController {

  validatesSelf[SelfValidating]

  type UpdateSmallGroupEventFromExternalSystemCommand = Appliable[SmallGroupEvent] with SelfValidating

  @ModelAttribute("command") def cmd(
    @PathVariable module: Module,
    @PathVariable("smallGroupSet") set: SmallGroupSet,
    @PathVariable("smallGroup") group: SmallGroup,
    @PathVariable("smallGroupEvent") event: SmallGroupEvent
  ): UpdateSmallGroupEventFromExternalSystemCommand =
    UpdateSmallGroupEventFromExternalSystemCommand(module, set, group, event)

  protected def render(event: SmallGroupEvent): Mav = {
    val set = event.group.groupSet

    Mav("groups/admin/groups/events/update", "cancelUrl" -> postSaveRoute(event))
      .crumbs(Breadcrumbs.Department(set.department, set.academicYear), Breadcrumbs.ModuleForYear(set.module, set.academicYear))
  }

  protected def postSaveRoute(event: SmallGroupEvent): String

  @RequestMapping
  def form(
    @PathVariable("smallGroupEvent") event: SmallGroupEvent,
    @ModelAttribute("command") cmd: UpdateSmallGroupEventFromExternalSystemCommand
  ): Mav = render(event)

  protected def submit(cmd: UpdateSmallGroupEventFromExternalSystemCommand, errors: Errors, event: SmallGroupEvent, route: String): Mav = {
    if (errors.hasErrors) {
      render(event)
    } else {
      cmd.apply()
      RedirectForce(route)
    }
  }

  @RequestMapping(method = Array(POST))
  def save(
    @Valid @ModelAttribute("command") cmd: UpdateSmallGroupEventFromExternalSystemCommand,
    errors: Errors,
    @PathVariable("smallGroupEvent") event: SmallGroupEvent
  ): Mav = submit(cmd, errors, event, postSaveRoute(event))

}

@RequestMapping(Array("/groups/admin/module/{module}/groups/new/{smallGroupSet}/events/{smallGroup}/edit/{smallGroupEvent}/import"))
@Controller
class CreateSmallGroupSetUpdateEventFromExternalSystemController extends AbstractUpdateSmallGroupEventFromExternalSystemController {
  override def postSaveRoute(event: SmallGroupEvent): String = Routes.admin.createEditEvent(event)
}

@RequestMapping(Array("/groups/admin/module/{module}/groups/edit/{smallGroupSet}/events/{smallGroup}/edit/{smallGroupEvent}/import"))
@Controller
class EditSmallGroupSetUpdateEventFromExternalSystemController extends AbstractUpdateSmallGroupEventFromExternalSystemController {
  override def postSaveRoute(event: SmallGroupEvent): String = Routes.admin.editEditEvent(event)
}
