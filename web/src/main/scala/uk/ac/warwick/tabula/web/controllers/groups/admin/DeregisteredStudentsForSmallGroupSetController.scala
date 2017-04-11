package uk.ac.warwick.tabula.web.controllers.groups.admin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.groups.admin.DeregisteredStudentsForSmallGroupSetCommand
import uk.ac.warwick.tabula.commands.groups.admin.DeregisteredStudentsForSmallGroupSetCommand.StudentNotInMembership
import uk.ac.warwick.tabula.commands.{Appliable, MemberOrUser, PopulateOnForm}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController

import scala.collection.JavaConverters._
import scala.collection.mutable

@Controller
@RequestMapping(Array("/groups/admin/module/{module}/groups/{smallGroupSet}/deregistered"))
class DeregisteredStudentsForSmallGroupSetController extends GroupsController with AutowiringProfileServiceComponent {

	type DeregisteredStudentsForSmallGroupSetCommand = Appliable[Seq[StudentNotInMembership]] with PopulateOnForm

	@ModelAttribute("command") def command(@PathVariable module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet): DeregisteredStudentsForSmallGroupSetCommand =
		DeregisteredStudentsForSmallGroupSetCommand(module, set)

	@ModelAttribute("students") def students(@PathVariable("smallGroupSet") set: SmallGroupSet): mutable.Buffer[StudentNotInMembership] =
		set.studentsNotInMembership.map { user =>
			val member = profileService.getMemberByUser(user, disableFilter = true)

			// Safe to do a .get here as studentsNotInMembership mandates that this is defined
			val group = set.groups.asScala.find { _.students.includesUser(user) }.get

			StudentNotInMembership(MemberOrUser(member, user), group)
		}

	@RequestMapping(method = Array(GET, HEAD))
	def form(
		@PathVariable("smallGroupSet") set: SmallGroupSet,
		@ModelAttribute("command") cmd: DeregisteredStudentsForSmallGroupSetCommand
	): Mav = {
		cmd.populate()
		renderForm(set, cmd)
	}

	private def renderForm(set: SmallGroupSet, cmd: DeregisteredStudentsForSmallGroupSetCommand) =
		Mav("groups/admin/groups/deregistered/form")
			.crumbs(
				Breadcrumbs.Department(set.module.adminDepartment, set.academicYear),
				Breadcrumbs.ModuleForYear(set.module, set.academicYear)
			)

	@RequestMapping(method = Array(POST))
	def save(
		@Valid @ModelAttribute("command") cmd: DeregisteredStudentsForSmallGroupSetCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = {
		if (errors.hasErrors) renderForm(set, cmd)
		else {
			val removed = cmd.apply()

			Mav("groups/admin/groups/deregistered/results",
				"removed" -> removed,
				"returnTo" -> Breadcrumbs.ModuleForYear(set.module, set.academicYear).url
			).crumbs(
				Breadcrumbs.Department(set.module.adminDepartment, set.academicYear),
				Breadcrumbs.ModuleForYear(set.module, set.academicYear)
			)
		}
	}

}
