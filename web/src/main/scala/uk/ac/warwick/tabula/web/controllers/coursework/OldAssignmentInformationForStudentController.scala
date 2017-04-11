package uk.ac.warwick.tabula.web.controllers.coursework

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.{Assignment, Member, Module}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.{StudentMemberSubmissionAndFeedbackCommandState, StudentSubmissionAndFeedbackCommand}
import uk.ac.warwick.tabula.commands.coursework.StudentSubmissionAndFeedbackCommand._
import uk.ac.warwick.tabula.web.Mav

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/module/{module}/{assignment}/{studentMember}"))
class OldAssignmentInformationForStudentController extends OldCourseworkController {

	type StudentSubmissionAndFeedbackCommand = Appliable[StudentSubmissionInformation] with StudentMemberSubmissionAndFeedbackCommandState

	@ModelAttribute("command")
	def command(@PathVariable module: Module,
							@PathVariable assignment: Assignment,
							@PathVariable studentMember: Member): StudentSubmissionAndFeedbackCommand =
		StudentSubmissionAndFeedbackCommand(module, assignment, studentMember, user)

	@RequestMapping
	def assignmentGadgetInStudentProfile(@ModelAttribute("command") command: StudentSubmissionAndFeedbackCommand): Mav = {
		val info = command.apply()

		Mav(
			s"$urlPrefix/submit/assignment",
			"feedback" -> info.feedback,
			"submission" -> info.submission,
			"justSubmitted" -> false,
			"canSubmit" -> info.canSubmit,
			"canReSubmit" -> info.canReSubmit,
			"hasExtension" -> info.extension.isDefined,
			"hasActiveExtension" -> info.extension.exists(_.approved), // active = has been approved
			"extension" -> info.extension,
			"isExtended" -> info.isExtended,
			"extensionRequested" -> info.extensionRequested,
			"isSelf" -> (user.universityId == command.studentMember.universityId))
			.withTitle(command.module.name + " (" + command.module.code.toUpperCase + ")" + " - " + command.assignment.name)
	}

}