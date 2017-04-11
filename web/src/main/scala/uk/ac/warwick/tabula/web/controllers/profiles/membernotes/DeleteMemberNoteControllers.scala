package uk.ac.warwick.tabula.web.controllers.profiles.membernotes

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.profiles.membernotes._
import uk.ac.warwick.tabula.data.model.{AbstractMemberNote, ExtenuatingCircumstances, Member, MemberNote}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}

abstract class AbstractDeleteMemberNoteController extends ProfilesController {

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: Appliable[AbstractMemberNote], errors: Errors): Mav = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			cmd.apply()
			Mav(new JSONView(Map("status" -> "successful")))
		}
	}

}

@Controller
@RequestMapping(value = Array("/profiles/{member}/note/{note}/delete"))
class DeleteMemberNoteController extends AbstractDeleteMemberNoteController {

	@ModelAttribute("command")
	def command(@PathVariable member: Member, @PathVariable note: MemberNote) =
		DeleteMemberNoteCommand(note, member)

}

@Controller
@RequestMapping(value = Array("/profiles/{member}/circumstances/{circumstances}/delete"))
class DeleteExtenuatingCircumstancesController extends AbstractDeleteMemberNoteController {

	@ModelAttribute("command")
	def command(@PathVariable member: Member, @PathVariable circumstances: ExtenuatingCircumstances) =
		DeleteExtenuatingCircumstancesCommand(circumstances, member)

}

@Controller
@RequestMapping(value = Array("/profiles/{member}/note/{note}/restore"))
class RestoreMemberNoteController extends AbstractDeleteMemberNoteController {

	showDeletedItems

	@ModelAttribute("command")
	def command(@PathVariable member: Member, @PathVariable note: MemberNote) =
		RestoreMemberNoteCommand(note, member)

}

@Controller
@RequestMapping(value = Array("/profiles/{member}/circumstances/{circumstances}/restore"))
class RestoreExtenuatingCircumstancesController extends AbstractDeleteMemberNoteController {

	showDeletedItems

	@ModelAttribute("command")
	def command(@PathVariable member: Member, @PathVariable circumstances: ExtenuatingCircumstances) =
		RestoreExtenuatingCircumstancesCommand(circumstances, member)

}

@Controller
@RequestMapping(value = Array("/profiles/{member}/note/{note}/purge"))
class PurgeMemberNoteController extends AbstractDeleteMemberNoteController {

	showDeletedItems

	@ModelAttribute("command")
	def command(@PathVariable member: Member, @PathVariable note: MemberNote) =
		PurgeMemberNoteCommand(note, member)

}

@Controller
@RequestMapping(value = Array("/profiles/{member}/circumstances/{circumstances}/purge"))
class PurgeExtenuatingCircumstancesController extends AbstractDeleteMemberNoteController {

	showDeletedItems

	@ModelAttribute("command")
	def command(@PathVariable member: Member, @PathVariable circumstances: ExtenuatingCircumstances) =
		PurgeExtenuatingCircumstancesCommand(circumstances, member)

}
