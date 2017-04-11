package uk.ac.warwick.tabula.commands.exams.exams

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}

object DeleteExamCommand {
	def apply(exam: Exam) =
		new DeleteExamCommandInternal(exam)
			with ComposableCommand[Exam]
			with DeleteExamPermissions
			with DeleteExamCommandState
			with DeleteExamCommandDescription
			with DeleteExamValidation
			with AutowiringAssessmentServiceComponent
}

class DeleteExamCommandInternal(val exam: Exam)
	extends CommandInternal[Exam]
	with DeleteExamCommandState {

	self: AssessmentServiceComponent =>

	override def applyInternal(): Exam = {
		exam.markDeleted()
		assessmentService.save(exam)
		exam
	}
}

trait DeleteExamPermissions extends RequiresPermissionsChecking {

	self: DeleteExamCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Assignment.Delete, exam)
	}
}

trait DeleteExamCommandState  {

	var confirm = false
	def exam: Exam
}

trait DeleteExamCommandDescription extends Describable[Exam] {

	self: DeleteExamCommandState with AssessmentServiceComponent =>

	def describe(d: Description) {
		d.exam(exam)
	}
}

trait DeleteExamValidation extends SelfValidating {

	self: DeleteExamCommandState =>

	override def validate(errors: Errors) {
		if (!confirm) {
			errors.rejectValue("confirm", "exam.delete.confirm")
		} else validateCanDelete(errors)
	}
	def validateCanDelete(errors: Errors) {
		if (exam.deleted) {
			errors.reject("exam.delete.deleted")
		} else if (!exam.feedbacks.isEmpty) {
			errors.reject("exam.delete.marksAssociated")
		}
	}
}


