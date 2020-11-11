package uk.ac.warwick.tabula.commands.exams.grids

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{CoreRequiredModule, Department, Module, Route}
import uk.ac.warwick.tabula.data.{AutowiringStudentCourseYearDetailsDaoComponent, StudentCourseYearDetailsDaoComponent}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._

object GenerateExamGridSetCoreRequiredModulesCommand {
  def apply(department: Department, academicYear: AcademicYear) =
    new GenerateExamGridSetCoreRequiredModulesCommandInternal(department, academicYear)
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringStudentCourseYearDetailsDaoComponent
      with ComposableCommand[Map[Route, Seq[CoreRequiredModule]]]
      with PopulateGenerateExamGridSetCoreRequiredModulesCommand
      with GenerateExamGridSetCoreRequiredModulesCommandValidation
      with GenerateExamGridSetCoreRequiredModulesDescription
      with GenerateExamGridSetCoreRequiredModulesPermissions
      with GenerateExamGridSetCoreRequiredModulesCommandState
      with GenerateExamGridSetCoreRequiredModulesCommandRequest
      with GenerateExamGridSelectCourseCommandRequest
}

class GenerateExamGridSetCoreRequiredModulesCommandInternal(val department: Department, val academicYear: AcademicYear)
  extends CommandInternal[Map[Route, Seq[CoreRequiredModule]]] {

  self: GenerateExamGridSetCoreRequiredModulesCommandState with GenerateExamGridSetCoreRequiredModulesCommandRequest
    with GenerateExamGridSelectCourseCommandRequest with ModuleRegistrationServiceComponent =>

  override def applyInternal(): Map[Route, Seq[CoreRequiredModule]] = {
    allModules.map { case (route, _) =>
      val existing = existingCoreRequiredModules.getOrElse(route, Seq())
      val newModules = modules.asScala.getOrElse(route, JHashSet()).asScala
        .map(module => existing.find(_.module == module).getOrElse(
          new CoreRequiredModule(route, academicYear, yearOfStudy, module)
        ))
      val modulesToRemove = existing.diff(newModules.toSeq)
      modulesToRemove.foreach(moduleRegistrationService.delete)
      newModules.foreach(moduleRegistrationService.saveOrUpdate)
      route -> newModules.toSeq
    }
  }

}

trait PopulateGenerateExamGridSetCoreRequiredModulesCommand extends PopulateOnForm {

  self: GenerateExamGridSetCoreRequiredModulesCommandState with GenerateExamGridSetCoreRequiredModulesCommandRequest =>

  def populate(): Unit = {
    existingCoreRequiredModules.foreach { case (route, crModules) => modules.put(route, JHashSet(crModules.map(_.module).toSet)) }
  }

}

trait GenerateExamGridSetCoreRequiredModulesCommandValidation extends SelfValidating {

  self: GenerateExamGridSetCoreRequiredModulesCommandState with GenerateExamGridSetCoreRequiredModulesCommandRequest =>

  override def validate(errors: Errors): Unit = {
    allModules.foreach { case (route, routeModules) =>
      modules.asScala.get(route).foreach { crModules =>
        val invalidModules = crModules.asScala.filterNot(routeModules.contains)
        if (invalidModules.nonEmpty) {
          errors.rejectValue(s"modules[${route.code}]", "examGrid.coreRequiredModulesInvalid", Array(invalidModules.map(_.code).mkString(", ")), "")
        }
      }
    }
  }

}

trait GenerateExamGridSetCoreRequiredModulesPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: GenerateExamGridSetCoreRequiredModulesCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Department.ExamGrids, department)
  }

}

trait GenerateExamGridSetCoreRequiredModulesDescription extends Describable[Map[Route, Seq[CoreRequiredModule]]] {

  self: GenerateExamGridSetCoreRequiredModulesCommandState with GenerateExamGridSetCoreRequiredModulesCommandRequest
    with GenerateExamGridSelectCourseCommandRequest =>

  override lazy val eventName = "GenerateExamGridSetCoreRequiredModules"

  override def describe(d: Description): Unit = {
    d.department(department)
      .properties(
        "academicYear" -> academicYear.toString,
        "courses" -> courses.asScala.map(_.code),
        "yearOfStudy" -> yearOfStudy,
        "routeModules" -> modules.asScala.map { case (route, crModules) => route.code -> crModules.asScala.map(_.code) }
      )
  }
}

trait GenerateExamGridSetCoreRequiredModulesCommandState {

  self: ModuleAndDepartmentServiceComponent with GenerateExamGridSelectCourseCommandRequest
    with ModuleRegistrationServiceComponent with StudentCourseYearDetailsDaoComponent =>

  def department: Department

  def academicYear: AcademicYear

  private lazy val routesForDisplay: Seq[Route] = {
    if (routes.isEmpty) {
      studentCourseYearDetailsDao.findByCourseRoutesYear(academicYear, courses.asScala.toSeq, courseOccurrences.asScala.toSeq, routes.asScala.toSeq, studyYearByLevelOrBlock, includeTempWithdrawn, resitOnly = false, eagerLoad = true, disableFreshFilter = true)
        .filter(scyd => department.includesMember(scyd.studentCourseDetails.student, Some(department), Some(scyd.studentCourseDetails)))
        .map(scyd => Option(scyd.route).getOrElse(scyd.studentCourseDetails.currentRoute)).distinct
    } else {
      routes.asScala.toSeq.distinct
    }
  }
  lazy val allModules: Map[Route, Seq[Module]] = routesForDisplay.map(r =>
    r -> moduleAndDepartmentService.findByRouteYearAcademicYear(r, studyYearByLevelOrBlock, academicYear).sortBy(_.code)
  ).filter { case (_, modules) => modules.nonEmpty }.toMap
  lazy val existingCoreRequiredModules: Map[Route, Seq[CoreRequiredModule]] = routesForDisplay.map(r =>
    r -> moduleRegistrationService.findCoreRequiredModules(r, academicYear, studyYearByLevelOrBlock)
  ).toMap
}

trait GenerateExamGridSetCoreRequiredModulesCommandRequest {

  self: GenerateExamGridSetCoreRequiredModulesCommandState with GenerateExamGridSelectCourseCommandRequest =>

  var modules: JMap[Route, JSet[Module]] = LazyMaps.create { _: Route => JHashSet(): JSet[Module] }.asJava
}
