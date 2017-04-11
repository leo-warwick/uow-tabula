package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.commands.coursework.assignments.SubmissionAndFeedbackCommand
import uk.ac.warwick.tabula.web.controllers.MessageResolver

trait AssignmentStudentMessageResolver {
	def getMessageForStudent(student: SubmissionAndFeedbackCommand.Student, key: String, args: Object*): String
}

trait ReplacingAssignmentStudentMessageResolver extends AssignmentStudentMessageResolver {
	self: MessageResolver =>

	def getMessageForStudent(student: SubmissionAndFeedbackCommand.Student, key: String, args: Object*): String = {
		val studentId = student.user.getWarwickId

		val firstMarker =
			student.coursework.enhancedSubmission
				.flatMap { s => Option(s.submission) }
				.flatMap { _.firstMarker }
				.map { _.getWarwickId }
				.getOrElse("first marker")

		val secondMarker =
			student.coursework.enhancedSubmission
				.flatMap { s => Option(s.submission) }
				.flatMap { _.secondMarker }
				.map { _.getWarwickId }
				.getOrElse("second marker")

		getMessage(key, args)
			.replace("[STUDENT]", studentId)
			.replace("[FIRST_MARKER]", firstMarker)
			.replace("[SECOND_MARKER]", secondMarker)
	}
}
