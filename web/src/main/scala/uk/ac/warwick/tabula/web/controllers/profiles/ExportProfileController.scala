package uk.ac.warwick.tabula.web.controllers.profiles

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.reports.profiles.ProfileExportSingleCommand
import uk.ac.warwick.tabula.data.model.{Member, StudentMember}
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}

/**
	* This isn't exposed anywhere but is useful for acceptance testing
	*/
@Controller
@RequestMapping(Array("/profiles/view/{member}/{academicYear}.pdf"))
class ExportProfileController extends ProfilesController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(member: Member, academicYear: AcademicYear): ProfileExportSingleCommand.CommandType = member match {
		case student: StudentMember => ProfileExportSingleCommand(student, academicYear, user)
		case _ => throw new ItemNotFoundException
	}

	@RequestMapping(produces = Array("application/pdf"))
	def renderPdf(@Valid @ModelAttribute("command") command: ProfileExportSingleCommand.CommandType): RenderableFile =
		new RenderableAttachment(command.apply().head)

}
