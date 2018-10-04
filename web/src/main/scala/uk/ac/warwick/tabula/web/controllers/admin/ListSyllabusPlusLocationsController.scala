package uk.ac.warwick.tabula.web.controllers.admin

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.commands.admin.syllabusplus.{AddSyllabusPlusLocationCommand, DeleteSyllabusPlusLocationCommand, EditSyllabusPlusLocationCommand}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.SyllabusPlusLocation
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.AutowiringSyllabusPlusLocationServiceComponent
import uk.ac.warwick.tabula.services.timetables.ScientiaCentrallyManagedRooms
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/admin/scientia-rooms"))
class ListSyllabusPlusLocationsController extends AdminController
	with AutowiringSyllabusPlusLocationServiceComponent {

	@GetMapping
	def list(): Mav = {
		Mav("admin/scientia/list",
			"canManage" -> securityService.can(user, Permissions.ManageSyllabusPlusLocations),
			"locations" -> syllabusPlusLocationService.all()
		)
	}
}

@Controller
@RequestMapping(Array("/admin/scientia-rooms/populate"))
class PopulateSyllabusPlusLocationsController extends AdminController
with AutowiringSyllabusPlusLocationServiceComponent {
	@PostMapping
	def populate(): Mav = transactional() {
		if (syllabusPlusLocationService.all().nonEmpty) {
			throw new IllegalStateException("Refusing to populate a non-empty Syllabus+ locations table")
		}

		val locations = ScientiaCentrallyManagedRooms.CentrallyManagedRooms.map {
			case (upstreamName, location) =>
				val loc = new SyllabusPlusLocation
				loc.upstreamName = upstreamName
				loc.name = location.name
				loc.mapLocationId = location.locationId
				loc
		}

		locations.foreach(syllabusPlusLocationService.save)

		Redirect("/admin/scientia-rooms")
	}
}


@Controller
@RequestMapping(Array("/admin/scientia-rooms/new"))
class NewSyllabusPlusLocationController extends AdminController {
	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(): Appliable[SyllabusPlusLocation] with SelfValidating = AddSyllabusPlusLocationCommand()

	@GetMapping
	def form(): Mav = {
		Mav("admin/scientia/new")
	}

	@PostMapping
	def create(@Valid @ModelAttribute("command") command: Appliable[SyllabusPlusLocation] with SelfValidating, bindingResult: BindingResult): Mav = {
		if (bindingResult.hasErrors) {
			form()
		} else {
			command.apply()
			Redirect("/admin/scientia-rooms")
		}
	}
}

@Controller
@RequestMapping(Array("/admin/scientia-rooms/{location}"))
class ViewSyllabusPlusLocationController extends AdminController {
	@GetMapping
	def show(@PathVariable location: SyllabusPlusLocation): Mav = {
		Mav.empty()
	}
}

@Controller
@RequestMapping(Array("/admin/scientia-rooms/{location}/edit"))
class EditSyllabusPlusLocationController extends AdminController {
	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable location: SyllabusPlusLocation): Appliable[SyllabusPlusLocation] with SelfValidating = EditSyllabusPlusLocationCommand(location)

	@GetMapping
	def form(): Mav = {
		Mav("admin/scientia/edit")
	}

	@PostMapping
	def update(@Valid @ModelAttribute("command") command: Appliable[SyllabusPlusLocation] with SelfValidating, bindingResult: BindingResult): Mav = {
		if (bindingResult.hasErrors) {
			form()
		} else {
			command.apply()
			Redirect("/admin/scientia-rooms")
		}
	}
}

@Controller
@RequestMapping(Array("/admin/scientia-rooms/{location}/delete"))
class DeleteSyllabusPlusLocationController extends AdminController {
	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable location: SyllabusPlusLocation): Appliable[SyllabusPlusLocation] = DeleteSyllabusPlusLocationCommand(location)

	@GetMapping
	def form(@PathVariable location: SyllabusPlusLocation): Mav = {
		Mav("admin/scientia/delete")
	}

	@PostMapping
	def delete(@PathVariable location: SyllabusPlusLocation, @Valid @ModelAttribute("command") command: Appliable[SyllabusPlusLocation], bindingResult: BindingResult): Mav = {
		if (bindingResult.hasErrors) {
			form(location)
		} else {
			command.apply()
			Redirect("/admin/scientia-rooms")
		}
	}
}
