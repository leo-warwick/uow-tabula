package uk.ac.warwick.tabula.commands.exams.grids

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}


object GenerateModuleExamGridCommand {
	def apply(department: Department, academicYear: AcademicYear) =
		new GenerateModuleExamGridCommandInternal(department, academicYear)
			with AutowiringCourseAndRouteServiceComponent
			with AutowiringStudentCourseYearDetailsDaoComponent
			with AutowiringAssessmentMembershipServiceComponent
			with AutowiringModuleRegistrationServiceComponent
			with ComposableCommand[ModuleExamGridResult]
			with GenerateModuleExamGridValidation
			with GenerateModuleExamGridPermissions
			with GenerateModuleExamGridCommandState
			with GenerateModuleExamGridCommandRequest
			with ReadOnly with Unaudited
}

case class ModuleExamGridResult(
	upstreamAssessmentGroupAndSequenceAndOccurrencesWithComponentName: Seq[(String, String)],
	gridStudentDetailRecords: Seq[ModuleGridDetailRecord]
)

case class ModuleGridDetailRecord(
	moduleRegistration: ModuleRegistration,
	componentInfo: Map[String, AssessmentComponentInfo],
	name: String,
	universityId: String,
	lastImportDate: Option[DateTime]
)


case class AssessmentComponentInfo(mark: BigDecimal, grade: String, isActualMark: Boolean, isActualGrade: Boolean, resitInfo: ResitComponentInfo)

case class ResitComponentInfo(resitMark: BigDecimal, resitGrade: String, isActualResitMark: Boolean, isActualResitGrade: Boolean)

class GenerateModuleExamGridCommandInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[ModuleExamGridResult] with TaskBenchmarking {

	self: StudentCourseYearDetailsDaoComponent with GenerateModuleExamGridCommandRequest with ModuleRegistrationServiceComponent with AssessmentMembershipServiceComponent =>


	override def applyInternal(): ModuleExamGridResult = {

		val upstreamAssessmentGroupAndSequenceAndOccurrencesWithComponentName = benchmarkTask("AssessmentComponentInfo") {
			assessmentMembershipService.getUpstreamAssessmentGroups(module, academicYear)
				.filterNot(p => p.assessmentGroup == AssessmentComponent.NoneAssessmentGroup)
				.map { ac =>
					(s"${ac.assessmentGroup}-${ac.sequence}-${ac.occurrence}", (assessmentMembershipService.getAssessmentComponent(ac) match {
						case Some(c) => c.name
						case _ => ""
					}))
				}
		}

		val records = moduleRegistrationService.getByModuleAndYear(module, academicYear)

		val gridStudentDetailRecords = benchmarkTask("GenerateMRComponents") {
			records.map { mr =>
				val componentInfo = mr.upstreamAssessmentGroupMembers.flatMap { uagm =>
					val aComponent = assessmentMembershipService.getAssessmentComponent(uagm.upstreamAssessmentGroup)
					aComponent.filter(_.inUse).map { comp =>
						val mark = uagm.agreedMark.getOrElse(uagm.actualMark.getOrElse(null))
						val grade = uagm.agreedGrade.getOrElse(uagm.actualGrade.getOrElse(null))
						val resitMark = uagm.resitAgreedMark.getOrElse(uagm.resitActualMark.getOrElse(null))
						val resitGrade = uagm.resitAgreedGrade.getOrElse(uagm.resitActualGrade.getOrElse(null))
						val resitInfo = ResitComponentInfo(resitMark, resitGrade, !uagm.resitAgreedMark.isDefined, !uagm.resitAgreedGrade.isDefined)
						s"${uagm.upstreamAssessmentGroup.assessmentGroup}-${uagm.upstreamAssessmentGroup.sequence}-${uagm.upstreamAssessmentGroup.occurrence}" -> AssessmentComponentInfo(mark, grade, !uagm.agreedMark.isDefined, !uagm.agreedGrade.isDefined, resitInfo)
					}
				}.sortBy(_._1).toMap
				val stu = mr.studentCourseDetails.student
				ModuleGridDetailRecord(mr, componentInfo, s"${stu.firstName} ${stu.lastName}", stu.universityId, Option(stu.lastImportDate))
			}
		}

		ModuleExamGridResult(upstreamAssessmentGroupAndSequenceAndOccurrencesWithComponentName, gridStudentDetailRecords)
	}
}

trait GenerateModuleExamGridValidation extends SelfValidating {

	self: GenerateModuleExamGridCommandState with GenerateModuleExamGridCommandRequest =>
	override def validate(errors: Errors): Unit = {
		if (module == null) {
			errors.reject("examModuleGrid.module.empty")
		}
	}
}

trait GenerateModuleExamGridPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: GenerateModuleExamGridCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.ExamGrids, department)
	}

}

trait GenerateModuleExamGridCommandState {

	self: CourseAndRouteServiceComponent =>

	def department: Department

	def academicYear: AcademicYear

}

trait GenerateModuleExamGridCommandRequest {
	var module: Module = _
}
