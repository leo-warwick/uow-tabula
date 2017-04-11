package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.feedback.MarksTemplateCommand._
import uk.ac.warwick.tabula.commands.coursework.feedback.{GenerateMarksTemplateCommand, GenerateOwnMarksTemplateCommand}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.services.AssessmentMembershipService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.ExcelView
import uk.ac.warwick.userlookup.User

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marks-template"))
class OldAssignmentMarksTemplateController extends OldCourseworkController {

	var assignmentMembershipService: AssessmentMembershipService = Wire[AssessmentMembershipService]

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		GenerateMarksTemplateCommand(
			mandatory(module),
			mandatory(assignment),
			assignmentMembershipService.determineMembershipUsers(assignment).map(_.getWarwickId)
		)

	@RequestMapping(method = Array(HEAD, GET))
	def generateMarksTemplate(@ModelAttribute("command") cmd: Appliable[XSSFWorkbook], @PathVariable assignment: Assignment): ExcelView = {
		new ExcelView(safeAssessmentName(assignment) + " marks.xlsx", cmd.apply())
	}
}


@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marker/{marker}/marks-template"))
class OldAssignmentMarkerMarksTemplateController extends OldCourseworkController {

	@ModelAttribute("command")
	def command(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@PathVariable marker: User
	) =
		GenerateOwnMarksTemplateCommand(
			mandatory(module),
			mandatory(assignment),
			assignment.getMarkersSubmissions(mandatory(marker)).flatMap(_.universityId)
		)

	@RequestMapping(method = Array(HEAD, GET))
	def generateMarksTemplate(@ModelAttribute("command") cmd: Appliable[XSSFWorkbook], @PathVariable assignment: Assignment): ExcelView = {
		new ExcelView(safeAssessmentName(assignment) + " marks.xlsx", cmd.apply())
	}
}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marker/marks-template"))
class OldCurrentAssignmentMarkerMarksTemplateController extends OldCourseworkController {

	@RequestMapping
	def redirect(@PathVariable assignment: Assignment, currentUser: CurrentUser): Mav = {
		Redirect(Routes.admin.assignment.markerFeedback.marksTemplate(assignment, currentUser.apparentUser))
	}
}
