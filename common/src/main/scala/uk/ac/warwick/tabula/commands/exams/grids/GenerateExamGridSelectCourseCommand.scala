package uk.ac.warwick.tabula.commands.exams.grids

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringStudentCourseYearDetailsDaoComponent, StudentCourseYearDetailsDaoComponent}
import uk.ac.warwick.tabula.helpers.StringUtils
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringCourseAndRouteServiceComponent, AutowiringLevelServiceComponent, CourseAndRouteServiceComponent, LevelServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.immutable.Range.Inclusive
import scala.jdk.CollectionConverters._

object GenerateExamGridSelectCourseCommand {
  def apply(department: Department, academicYear: AcademicYear, permitRoutesFromRootDepartment: Boolean = false) =
    new GenerateExamGridSelectCourseCommandInternal(department, academicYear, permitRoutesFromRootDepartment)
      with AutowiringCourseAndRouteServiceComponent
      with AutowiringStudentCourseYearDetailsDaoComponent
      with AutowiringLevelServiceComponent
      with AutowiringMitCircsSubmissionServiceComponent
      with ComposableCommand[Seq[ExamGridEntity]]
      with GenerateExamGridSelectCourseValidation
      with GenerateExamGridSelectCoursePermissions
      with GenerateExamGridSelectCourseCommandState
      with GenerateExamGridSelectCourseCommandRequest
      with ReadOnly with Unaudited
}

class GenerateExamGridSelectCourseCommandInternal(val department: Department, val academicYear: AcademicYear, val permitRoutesFromRootDepartment: Boolean)
  extends CommandInternal[Seq[ExamGridEntity]] with TaskBenchmarking {

  self: StudentCourseYearDetailsDaoComponent with GenerateExamGridSelectCourseCommandRequest with MitCircsSubmissionServiceComponent =>

  override def applyInternal(): Seq[ExamGridEntity] = {
    val scyds = benchmarkTask("findByCourseRoutesYear") {
      if (yearOfStudy != null) {
        studentCourseYearDetailsDao.findByCourseRoutesYear(academicYear, courses.asScala.toSeq, courseOccurrences.asScala.toSeq, routes.asScala.toSeq, yearOfStudy, includeTempWithdrawn, resitOnly, eagerLoad = true, disableFreshFilter = true, includePermWithdrawn = includePermWithdrawn)
      } else {
        studentCourseYearDetailsDao.findByCourseRoutesLevel(academicYear, courses.asScala.toSeq, courseOccurrences.asScala.toSeq, routes.asScala.toSeq, levelCode, includeTempWithdrawn, resitOnly, eagerLoad = true, disableFreshFilter = true, includePermWithdrawn = includePermWithdrawn)
      }.filter(scyd => department.includesMember(scyd.studentCourseDetails.student, Some(department)))
    }
    val sorted = benchmarkTask("sorting") {
      scyds.sortBy(_.studentCourseDetails.scjCode)
    }

    val mitCircsSubmissions = benchmarkTask("mitCircsSubmissions") {
      mitCircsSubmissionService.submissionsForStudents(sorted.map(_.studentCourseDetails.student))
    }

    benchmarkTask("toExamGridEntities") {
      sorted.map(scyd => {
        val student = scyd.studentCourseDetails.student
        val mitCircs = mitCircsSubmissions.getOrElse(student.universityId, Nil)
        student.toExamGridEntity(scyd, basedOnLevel = levelCode != null, mitCircs)
      })
    }
  }

}

trait GenerateExamGridSelectCourseValidation extends SelfValidating {

  self: GenerateExamGridSelectCourseCommandState with GenerateExamGridSelectCourseCommandRequest =>

  override def validate(errors: Errors): Unit = {
    if (courses.isEmpty) {
      errors.reject("examGrid.course.empty")
    } else if (courses.asScala.exists(c => !allCourses.contains(c))) {
      errors.reject("examGrid.course.invalid")
    }
    if (yearOfStudy == null && levelCode == null) {
      errors.reject("examGrid.yearOfStudy.empty")
    } else if (yearOfStudy != null && !allYearsOfStudy.contains(yearOfStudy)) {
      errors.reject("examGrid.yearOfStudy.invalid", Array(FilterStudentsOrRelationships.MaxYearsOfStudy.toString), "")
    } else if (levelCode != null && !allLevels.map(_.code).contains(levelCode)) {
      errors.reject("examGrid.levelCode.invalid", Array(levelCode), "")
    }

  }
}

trait GenerateExamGridSelectCoursePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: GenerateExamGridSelectCourseCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Department.ExamGrids, department)
  }

}

trait GenerateExamGridSelectCourseCommandState {

  self: CourseAndRouteServiceComponent with LevelServiceComponent =>

  def department: Department

  def academicYear: AcademicYear

  def permitRoutesFromRootDepartment: Boolean

  // Courses are always owned by the root department
  lazy val allCourses: List[Course] = department.rootDepartment.descendants.flatMap(d => courseAndRouteService.findCoursesInDepartment(d)).filter(_.inUse).sortBy(_.code)
  lazy val allRoutes: List[Route] = {
    val descendantRoutes = department.descendants.flatMap(d => courseAndRouteService.findRoutesInDepartment(d))

    val rootDepartmentRoutes = if (descendantRoutes.isEmpty && permitRoutesFromRootDepartment) {
      courseAndRouteService.findRoutesInDepartment(department.rootDepartment)
    } else Nil

    (descendantRoutes ++ rootDepartmentRoutes).sortBy(_.code)
  }
  lazy val allYearsOfStudy: Inclusive = 1 to FilterStudentsOrRelationships.MaxYearsOfStudy
  lazy val allLevels: List[Level] = levelService.getAllLevels.toList.sortBy(_.code)(StringUtils.AlphaNumericStringOrdering)

  lazy val allCourseOccurrences: List[String] = courseAndRouteService.getOccurrencesForCourses(allCourses).sorted.toList
}

trait GenerateExamGridSelectCourseCommandRequest {
  var courses: JList[Course] = JArrayList()
  var courseOccurrences: JList[String] = JArrayList()
  var routes: JList[Route] = JArrayList()
  var yearOfStudy: JInteger = _
  var levelCode: String = _
  var courseYearsToShow: JSet[String] = JHashSet()
  var includeTempWithdrawn: Boolean = false
  var resitOnly: Boolean = false
  var includePermWithdrawn: Boolean = true

  def pgCourseIncluded: Boolean = courses.asScala.exists(_.courseType == CourseType.PGT)
  def ugCourseIncluded: Boolean = courses.asScala.exists(_.courseType == CourseType.UG)

  def isLevelGrid: Boolean = levelCode != null

  // true if none of the course filter parameters have been selected
  def isEmptyRequest: Boolean = courses.isEmpty &&
    courseOccurrences.isEmpty &&
    routes.isEmpty &&
    yearOfStudy == null &&
    levelCode == null

  // parses undergrad level codes into year of study as if the degree was being taken full time - otherwise returns 1 as other courses don't have multiple levels
  def studyYearByLevelOrBlock: JInteger = {
    JInteger(Option(yearOfStudy).map(_.toInt).orElse(Option(levelCode).map(Level.toYearOfStudy)))
  }

  def toMap: Map[String, Any] = Map(
    "courses" -> courses.asScala.map(_.code),
    "courseOccurrences" -> courseOccurrences.asScala,
    "routes" -> routes.asScala.map(_.code),
    "yearOfStudy" -> yearOfStudy,
    "levelCode" -> levelCode,
    "courseYearsToShow" -> courseYearsToShow,
    "includeTempWithdrawn" -> includeTempWithdrawn,
    "resitOnly" -> resitOnly,
    "includePermWithdrawn" -> includePermWithdrawn
  )
}
