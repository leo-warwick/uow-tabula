package uk.ac.warwick.tabula.web.controllers.groups.admin

import java.util.concurrent.TimeoutException

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.groups.admin.{EditSmallGroupEventsCommand, ImportSmallGroupEventsFromExternalSystemCommand, PopulateEditSmallGroupEventsSubCommands}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.util.Try

trait SyllabusPlusEventCountForModule {
	self: ModuleTimetableFetchingServiceComponent =>

	@ModelAttribute("syllabusPlusEventCount")
	def syllabusPlusEventCount(@PathVariable module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet): Int =
		Try(Await.result(timetableFetchingService.getTimetableForModule(module.code.toUpperCase, includeStudents = false), ImportSmallGroupEventsFromExternalSystemCommand.Timeout))
			.recover { case _: TimeoutException | _: TimetableEmptyException => EventList.empty }.get
			.events.count(ImportSmallGroupEventsFromExternalSystemCommand.isValidForYear(set.academicYear))
}

abstract class AbstractEditSmallGroupEventsController extends GroupsController
	with AutowiringScientiaTimetableFetchingServiceComponent
	with SyllabusPlusEventCountForModule {

	validatesSelf[SelfValidating]

	type EditSmallGroupEventsCommand = Appliable[SmallGroupSet] with PopulateEditSmallGroupEventsSubCommands

	@ModelAttribute("ManageSmallGroupsMappingParameters") def params = ManageSmallGroupsMappingParameters

	@ModelAttribute("command") def command(@PathVariable module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet): EditSmallGroupEventsCommand =
		EditSmallGroupEventsCommand(module, set)

	protected def renderPath: String

	protected def render(set: SmallGroupSet, model: Map[String, _] = Map()): Mav = {
		Mav(renderPath, model ++ Map("groups" -> set.groups.asScala.sorted)).crumbs(Breadcrumbs.Department(set.module.adminDepartment, set.academicYear), Breadcrumbs.ModuleForYear(set.module, set.academicYear))
	}

	@RequestMapping
	def form(
		@PathVariable("smallGroupSet") set: SmallGroupSet,
		@ModelAttribute("command") cmd: EditSmallGroupEventsCommand
	): Mav = render(set)

	protected def submit(cmd: EditSmallGroupEventsCommand, errors: Errors, set: SmallGroupSet, route: String): Mav = {
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			RedirectForce(route)
		}
	}

	@RequestMapping(method = Array(POST), params=Array("action=update"))
	def save(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = {
		if (!errors.hasErrors) {
			cmd.apply()
			cmd.populate()
		}

		render(set, Map("saved" -> true))
	}

	@RequestMapping(method = Array(POST), params=Array("action!=refresh", "action!=update"))
	def saveAndExit(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin(set.module.adminDepartment, set.academicYear))

}

@RequestMapping(Array("/groups/admin/module/{module}/groups/new/{smallGroupSet}/events"))
@Controller
class CreateSmallGroupSetAddEventsController extends AbstractEditSmallGroupEventsController {

	override val renderPath = "groups/admin/groups/newevents"

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndEditProperties, "action!=refresh", "action!=update"))
	def saveAndEditProperties(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.create(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddStudents, "action!=refresh", "action!=update"))
	def saveAndAddStudents(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.createAddStudents(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddGroups, "action!=refresh", "action!=update"))
	def saveAndAddGroups(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.createAddGroups(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAllocate, "action!=refresh", "action!=update"))
	def saveAndAddAllocate(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.createAllocate(set))

}

@RequestMapping(Array("/groups/admin/module/{module}/groups/edit/{smallGroupSet}/events"))
@Controller
class EditSmallGroupSetAddEventsController extends AbstractEditSmallGroupEventsController {

	override val renderPath = "groups/admin/groups/editevents"

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndEditProperties, "action!=refresh", "action!=update"))
	def saveAndEditProperties(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.edit(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddStudents, "action!=refresh", "action!=update"))
	def saveAndAddStudents(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.editAddStudents(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddGroups, "action!=refresh", "action!=update"))
	def saveAndAddGroups(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.editAddGroups(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAllocate, "action!=refresh", "action!=update"))
	def saveAndAddAllocate(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupEventsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.editAllocate(set))

}
