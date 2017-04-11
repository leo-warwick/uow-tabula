package uk.ac.warwick.tabula.web.controllers.exams.exams.admin

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.assignments.{AdminAddMarksCommand, PostExtractValidation}
import uk.ac.warwick.tabula.commands.coursework.feedback.GenerateGradesFromMarkCommand
import uk.ac.warwick.tabula.services.coursework.docconversion.MarkItem
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.data.model.{MarkingWorkflow, Exam, Feedback, Module}
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.services.{UserLookupService, AssessmentMembershipService, FeedbackService}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(value = Array("/exams/exams/admin/module/{module}/{academicYear}/exams/{exam}/marks"))
class ExamsAddMarksController extends ExamsController {

	@Autowired var feedbackService: FeedbackService = _
	@Autowired var examMembershipService: AssessmentMembershipService = _
	@Autowired var userLookup: UserLookupService = _

	type AdminAddMarksCommand = Appliable[Seq[Feedback]] with PostExtractValidation

	@ModelAttribute("adminAddMarksCommand")
	def command(@PathVariable module: Module, @PathVariable exam: Exam, user: CurrentUser): AdminAddMarksCommand =
		AdminAddMarksCommand(mandatory(module), mandatory(exam), user, GenerateGradesFromMarkCommand(mandatory(module), mandatory(exam)))

	// Add the common breadcrumbs to the model.
	def crumbed(mav: Mav, module: Module, academicYear: AcademicYear): Mav = mav.crumbs(
		Breadcrumbs.Exams.Home,
		Breadcrumbs.Exams.Department(module.adminDepartment, academicYear),
		Breadcrumbs.Exams.Module(module, academicYear)
	)

	@RequestMapping(method = Array(HEAD, GET))
	def viewMarkUploadForm(
		@PathVariable module: Module,
		@PathVariable exam: Exam,
		@PathVariable academicYear: AcademicYear,
		@ModelAttribute("adminAddMarksCommand") cmd: AdminAddMarksCommand, errors: Errors
	): Mav = {
		val members = examMembershipService.determineMembershipUsersWithOrder(exam)

		val marksToDisplay = members.map { case (user, _) =>
			val feedback = feedbackService.getStudentFeedback(exam, user.getUserId)
			noteMarkItem(user, feedback)
		}

		val studentMarkerMap = members.map(m =>
			MarkingWorkflow.getMarkerFromAssessmentMap(userLookup, m._1.getWarwickId, exam.firstMarkerMap) match {
				case Some(markerId) => m._1.getWarwickId -> userLookup.getUserByUserId(markerId).getFullName
				case None => m._1.getWarwickId -> None
			}).toMap

		crumbed(Mav("exams/exams/admin/marks/marksform",
			"marksToDisplay" -> marksToDisplay,
			"seatNumberMap" -> members.map{case (user, seatNumber) => user.getUserId -> seatNumber}.toMap,
			"studentMarkerMap" -> studentMarkerMap,
			"isGradeValidation" -> module.adminDepartment.assignmentGradeValidation
		), module, academicYear)

	}

	private def noteMarkItem(member: User, feedback: Option[Feedback]) = {

		logger.debug("in noteMarkItem (logger.debug)")

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
		@PathVariable exam: Exam,
		@PathVariable academicYear: AcademicYear,
		@ModelAttribute("adminAddMarksCommand") cmd: AdminAddMarksCommand, errors: Errors
	): Mav = {
		if (errors.hasErrors) viewMarkUploadForm(module, exam, academicYear, cmd, errors)
		else {
			bindAndValidate(module, cmd, errors)
			crumbed(Mav("exams/exams/admin/marks/markspreview"), module, academicYear)
		}
	}

	@RequestMapping(method = Array(POST), params = Array("confirm=true"))
	def doUpload(
		@PathVariable module: Module,
		@PathVariable exam: Exam,
		@PathVariable academicYear: AcademicYear,
		@ModelAttribute("adminAddMarksCommand") cmd: AdminAddMarksCommand, errors: Errors
	): Mav = {
		bindAndValidate(module, cmd, errors)
		cmd.apply()
		Redirect(Routes.Exams.admin.module(module, academicYear))
	}

	private def bindAndValidate(module: Module, cmd: AdminAddMarksCommand, errors: Errors) {
		cmd.postExtractValidation(errors)
	}

}