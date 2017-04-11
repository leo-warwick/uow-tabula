package uk.ac.warwick.tabula.commands.exams.exams

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object EditExamCommand {
	def apply(exam: Exam) =
		new EditExamCommandInternal(exam)
			with ComposableCommand[Exam]
			with EditExamPermissions
			with EditExamCommandState
			with EditExamCommandDescription
			with PopulateEditExamCommand
			with ExamValidation
			with AutowiringAssessmentServiceComponent
			with AutowiringAssessmentMembershipServiceComponent
			with UpdatesStudentMembership
			with ModifiesExamMembership
			with HasAcademicYear
			with AutowiringUserLookupComponent
			with SpecifiesGroupType
}

class EditExamCommandInternal(override val exam: Exam)
	extends CommandInternal[Exam]
	with EditExamCommandState
	with UpdatesStudentMembership
	with ModifiesExamMembership {

	self: AssessmentServiceComponent with UserLookupComponent  with HasAcademicYear with SpecifiesGroupType
		with AssessmentMembershipServiceComponent =>

	override def applyInternal(): Exam = {

		this.copyTo(exam)

		assessmentService.save(exam)
		exam

	}
}

trait EditExamPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: EditExamCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Assignment.Update, exam)
	}
}

trait EditExamCommandState extends ExamState {

	self: AssessmentServiceComponent with UserLookupComponent with HasAcademicYear with SpecifiesGroupType
		with AssessmentMembershipServiceComponent =>

	def exam: Exam
	override def module: Module = exam.module
	override def academicYear: AcademicYear = exam.academicYear
}

trait EditExamCommandDescription extends Describable[Exam] {

	self: EditExamCommandState with AssessmentServiceComponent with UserLookupComponent with HasAcademicYear with SpecifiesGroupType
		with AssessmentMembershipServiceComponent =>

	def describe(d: Description) {
		d.exam(exam)
	}
}

trait PopulateEditExamCommand {

	self: EditExamCommandState with UpdatesStudentMembership =>

	name = exam.name
	assessmentGroups = exam.assessmentGroups
	markingWorkflow = exam.markingWorkflow
	massAddUsers = exam.members.users.map(_.getWarwickId).mkString("\n")

	def populateGroups(exam: Exam) {
		assessmentGroups = exam.assessmentGroups
		// TAB-4848 get all the groups that are linked even if they're marked not in use
		upstreamGroups.addAll(allUpstreamGroups.filter { ug =>
			assessmentGroups.asScala.exists(ag => ug.assessmentComponent == ag.assessmentComponent && ag.occurrence == ug.occurrence)
		}.asJavaCollection)
	}
}
