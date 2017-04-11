package uk.ac.warwick.tabula.web.controllers.exams.exams.admin

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.coursework.feedback.CheckSitsUploadCommand
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController

import scala.collection.JavaConverters._

@Controller
@RequestMapping(Array("/exams/exams/admin/module/{module}/{academicYear}/exams/{exam}/feedback/{feedback}/check-sits"))
class CheckExamSitsUploadController extends ExamsController with AutowiringProfileServiceComponent {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable exam: Exam, @PathVariable feedback: ExamFeedback) =
		CheckSitsUploadCommand(mandatory(feedback))

	@RequestMapping
	def page(
		@ModelAttribute("command") cmd: Appliable[CheckSitsUploadCommand.Result],
		errors: Errors,
		@PathVariable feedback: ExamFeedback
	): Mav = {
		val sprCodes = feedback.universityId.flatMap(uid => profileService.getMemberByUniversityId(uid)).flatMap {
			case s: StudentMember => Some(s.freshStudentCourseDetails.map(_.sprCode))
			case _ => None
		}.getOrElse(Seq())
		if (errors.hasErrors) {
			Mav("exams/exams/admin/check_sits")
		} else {
			Mav("exams/exams/admin/check_sits",
				"result" -> cmd.apply(),
				"assessmentGroupPairs" -> feedback.assessmentGroups.asScala.map(assessGroup => (assessGroup.occurrence, assessGroup.assessmentComponent.sequence)),
				"sprCodes" -> sprCodes
			)
		}
	}

}
