package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.assignments.{DownloadAdminSubmissionsForPrintingCommand, DownloadMarkerSubmissionsForPrintingCommand, DownloadSubmissionsForPrintingCommandRequest}
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.system.RenderableFileView
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.userlookup.User

trait OldDownloadSubmissionsForPrintingController extends OldCourseworkController with AutowiringUserLookupComponent {

	type DownloadSubmissionsForPrintingCommand = Appliable[RenderableFile] with DownloadSubmissionsForPrintingCommandRequest

	@RequestMapping
	def pdfCheck(@ModelAttribute("command") cmd: DownloadSubmissionsForPrintingCommand, @PathVariable module: Module): Mav = {
		Mav(new JSONView(Map(
			"submissionsWithNonPDFs" -> cmd.submissions.filter(
				_.allAttachments.exists(!_.name.endsWith(DownloadAdminSubmissionsForPrintingCommand.pdfExtension))
			).map(submission =>
				Map(
					"submission" -> submission.id,
					"universityId" -> submission.studentIdentifier,
					"name" -> (if (module.adminDepartment.showStudentName) userLookup.getUserByUserId(submission.usercode).getFullName else ""),
					"nonPDFFiles" -> submission.allAttachments.filter(
						!_.name.endsWith(DownloadAdminSubmissionsForPrintingCommand.pdfExtension)
					).map(_.name)
				)
			)
		))).noLayout()
	}

	@RequestMapping(params = Array("download"))
	def download(@ModelAttribute("command") cmd: DownloadSubmissionsForPrintingCommand): Mav = {
		Mav(new RenderableFileView(cmd.apply()))
	}

}
@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/submissions.pdf"))
class OldDownloadAdminSubmissionsForPrintingController extends OldDownloadSubmissionsForPrintingController {

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		DownloadAdminSubmissionsForPrintingCommand(module, assignment)

}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marker/{marker}/submissions.pdf"))
class OldDownloadMarkerSubmissionsForPrintingController extends OldDownloadSubmissionsForPrintingController {

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment, @PathVariable marker: User) =
		DownloadMarkerSubmissionsForPrintingCommand(module, assignment, marker, user)

}