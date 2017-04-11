package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.feedback.GenerateGradesFromMarkCommand
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.commands.coursework.assignments.{AdminAddMarksCommand, PostExtractValidation}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.{CurrentUser, PermissionDeniedException}
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.coursework.web.Routes
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import uk.ac.warwick.tabula.services.coursework.docconversion.MarkItem
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.services.AssessmentMembershipService
import uk.ac.warwick.tabula.services.FeedbackService

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marks"))
class OldAddMarksController extends OldCourseworkController {

	@Autowired var feedbackService: FeedbackService = _
	@Autowired var assignmentMembershipService: AssessmentMembershipService = _

	type AdminAddMarksCommand = Appliable[Seq[Feedback]] with PostExtractValidation

	@ModelAttribute("adminAddMarksCommand") def command(@PathVariable module: Module, @PathVariable assignment: Assignment, user: CurrentUser): AdminAddMarksCommand =
		AdminAddMarksCommand(mandatory(module), mandatory(assignment), user, GenerateGradesFromMarkCommand(mandatory(module), mandatory(assignment)))

	// Add the common breadcrumbs to the model.
	def crumbed(mav: Mav, module: Module): Mav = mav.crumbs(Breadcrumbs.Department(module.adminDepartment), Breadcrumbs.Module(module))

	@RequestMapping(method = Array(HEAD, GET))
	def viewMarkUploadForm(
			@PathVariable module: Module,
			@PathVariable assignment: Assignment,
			@ModelAttribute("adminAddMarksCommand") cmd: AdminAddMarksCommand, errors: Errors
	): Mav = {

		if(assignment.hasWorkflow) {
			logger.error(s"Can't add marks to an assignment with a workflow - ${assignment.id}")
			throw new PermissionDeniedException(user, Permissions.AssignmentFeedback.Manage, assignment)
		}

		// All mark uploads are keyed on uniID so ignore others
		val members = assignmentMembershipService.determineMembershipUsers(assignment).filter(_.getWarwickId != null)

		val marksToDisplay = members.map { member =>
			val feedback = feedbackService.getStudentFeedback(assignment, member.getUserId)
			noteMarkItem(member, feedback)
		}.sortBy(_.studentIdentifier)

		crumbed(Mav(s"$urlPrefix/admin/assignments/marks/marksform",
			"marksToDisplay" -> marksToDisplay,
			"isGradeValidation" -> module.adminDepartment.assignmentGradeValidation
		), module)

	}

	private def noteMarkItem(member: User, feedback: Option[Feedback]) = {

		val markItem = new MarkItem()
		markItem.universityId = member.getWarwickId
		markItem.user = member

		feedback match {
			case Some(f) =>
				markItem.actualMark = f.actualMark.map { _.toString }.getOrElse("")
				markItem.actualGrade = f.actualGrade.map { _.toString }.getOrElse("")
			case None =>
				markItem.actualMark = ""
				markItem.actualGrade = ""
		}

		markItem
	}

	@RequestMapping(method = Array(POST), params = Array("!confirm"))
	def confirmBatchUpload(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@ModelAttribute("adminAddMarksCommand") cmd: AdminAddMarksCommand, errors: Errors
	): Mav = {
		if (errors.hasErrors) viewMarkUploadForm(module, assignment, cmd, errors)
		else {
			bindAndValidate(module, cmd, errors)
			crumbed(Mav(s"$urlPrefix/admin/assignments/marks/markspreview"), module)
		}
	}

	@RequestMapping(method = Array(POST), params = Array("confirm=true"))
	def doUpload(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@ModelAttribute("adminAddMarksCommand") cmd: AdminAddMarksCommand, errors: Errors
	): Mav = {
		bindAndValidate(module, cmd, errors)
		cmd.apply()
		Redirect(Routes.admin.module(module))
	}

	private def bindAndValidate(module: Module, cmd: AdminAddMarksCommand, errors: Errors) {
		cmd.postExtractValidation(errors)
	}

}