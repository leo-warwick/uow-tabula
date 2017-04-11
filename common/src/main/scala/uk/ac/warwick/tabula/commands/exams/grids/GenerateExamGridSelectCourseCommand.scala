package uk.ac.warwick.tabula.commands.exams.grids

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringStudentCourseYearDetailsDaoComponent, StudentCourseYearDetailsDaoComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringCourseAndRouteServiceComponent, CourseAndRouteServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._
import scala.collection.immutable.Range.Inclusive

object GenerateExamGridSelectCourseCommand {
	def apply(department: Department, academicYear: AcademicYear) =
		new GenerateExamGridSelectCourseCommandInternal(department, academicYear)
			with AutowiringCourseAndRouteServiceComponent
			with AutowiringStudentCourseYearDetailsDaoComponent
			with ComposableCommand[Seq[ExamGridEntity]]
			with GenerateExamGridSelectCourseValidation
			with GenerateExamGridSelectCoursePermissions
			with GenerateExamGridSelectCourseCommandState
			with GenerateExamGridSelectCourseCommandRequest
			with ReadOnly with Unaudited
}

class GenerateExamGridSelectCourseCommandInternal(val department: Department, val academicYear: AcademicYear) 
	extends CommandInternal[Seq[ExamGridEntity]] with TaskBenchmarking {

	self: StudentCourseYearDetailsDaoComponent with GenerateExamGridSelectCourseCommandRequest =>

	override def applyInternal(): Seq[ExamGridEntity] = {
		val scyds = benchmarkTask("findByCourseRoutesYear") {
			studentCourseYearDetailsDao.findByCourseRoutesYear(academicYear, course, routes.asScala, yearOfStudy, includeTempWithdrawn, eagerLoad = true, disableFreshFilter = true)
				.filter(scyd => department.includesMember(scyd.studentCourseDetails.student, Some(department)))
		}
		val sorted = benchmarkTask("sorting") {
			scyds.sortBy(_.studentCourseDetails.scjCode)
		}
		benchmarkTask("toExamGridEntities") {
			sorted.map(scyd => scyd.studentCourseDetails.student.toExamGridEntity(scyd))
		}
	}

}

trait GenerateExamGridSelectCourseValidation extends SelfValidating {

	self: GenerateExamGridSelectCourseCommandState with GenerateExamGridSelectCourseCommandRequest =>

	override def validate(errors: Errors): Unit = {
		if (course == null) {
			errors.reject("examGrid.course.empty")
		} else if (!allCourses.contains(course)) {
			errors.reject("examGrid.course.invalid")
		}
		if (yearOfStudy == null) {
			errors.reject("examGrid.yearOfStudy.empty")
		} else if (!allYearsOfStudy.contains(yearOfStudy)) {
			errors.reject("examGrid.yearOfStudy.invalid", Array(FilterStudentsOrRelationships.MaxYearsOfStudy.toString), "")
		}
	}
}

trait GenerateExamGridSelectCoursePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: GenerateExamGridSelectCourseCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.ExamGrids, department)
	}

}

trait GenerateExamGridSelectCourseCommandState {

	self: CourseAndRouteServiceComponent =>

	def department: Department
	def academicYear: AcademicYear

	// Courses are always owned by the root department
	lazy val allCourses: List[Course] = department.rootDepartment.descendants.flatMap(d => courseAndRouteService.findCoursesInDepartment(d)).sortBy(_.code)
	lazy val allRoutes: List[Route] = department.descendants.flatMap(d => courseAndRouteService.findRoutesInDepartment(d)).sortBy(_.code)
	lazy val allYearsOfStudy: Inclusive = 1 to FilterStudentsOrRelationships.MaxYearsOfStudy
}

trait GenerateExamGridSelectCourseCommandRequest {
	var course: Course = _
	var routes: JList[Route] = JArrayList()
	var yearOfStudy: JInteger = _
	var includeTempWithdrawn: Boolean = false
}
