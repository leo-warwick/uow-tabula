package uk.ac.warwick.tabula.commands.cm2.marksmanagement

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{DegreeType, Department}
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._

object MarksOpenAndCloseDepartmentsCommand {
  type Command = Appliable[Seq[Department]] with PopulateOnForm

  def apply(): Command =
    new MarksOpenAndCloseDepartmentsCommandInternal
      with ComposableCommand[Seq[Department]]
      with AutowiringModuleAndDepartmentServiceComponent
      with MarksOpenAndCloseDepartmentsRequest
      with MarksOpenAndCloseDepartmentsCommandPermissions
      with MarksOpenAndCloseDepartmentsCommandDescription
      with AutowiringTransactionalComponent
}

abstract class MarksOpenAndCloseDepartmentsCommandInternal extends CommandInternal[Seq[Department]] {
  self: MarksOpenAndCloseDepartmentsRequest
    with ModuleAndDepartmentServiceComponent
    with TransactionalComponent =>

  override def applyInternal(): Seq[Department] = transactional() {
    departments.map { department =>
      val settings = state.asScala.get(department.code)
      settings.foreach { s =>
        val currentYear = AcademicYear.now()
        val previousYear = currentYear - 1

        department.uploadCourseworkMarksToSits = s.uploadCourseworkMarksToSits
        department.setUploadMarksToSitsForYear(previousYear, DegreeType.Undergraduate, canUpload = s.openForPreviousYearUG)
        department.setUploadMarksToSitsForYear(previousYear, DegreeType.Postgraduate, canUpload = s.openForPreviousYearPG)
        department.setUploadMarksToSitsForYear(currentYear, DegreeType.Undergraduate, canUpload = s.openForCurrentYearUG)
        department.setUploadMarksToSitsForYear(currentYear, DegreeType.Postgraduate, canUpload = s.openForCurrentYearPG)

        moduleAndDepartmentService.saveOrUpdate(department)
      }

      department
    }
  }

}

trait MarksOpenAndCloseDepartmentsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Marks.MarksManagement)
  }
}

trait MarksOpenAndCloseDepartmentsCommandDescription extends Describable[Seq[Department]] {
  override lazy val eventName: String = "MarksOpenAndCloseDepartments"

  override def describe(d: Description): Unit = {}
}

trait MarksOpenAndCloseDepartmentsRequest extends PopulateOnForm {
  self: ModuleAndDepartmentServiceComponent =>

  private def withSubDepartments(d: Department): Seq[Department] =
    Seq(d) ++ d.children.asScala.filterNot(_.isImportDepartment).toSeq.flatMap(withSubDepartments)

  lazy val departments: Seq[Department] =
    moduleAndDepartmentService.allRootDepartments.flatMap(withSubDepartments)
      .filter(_.modules.asScala.nonEmpty)

  var state: JMap[String, DepartmentMarkStateItem] =
    LazyMaps.create { _: String => new DepartmentMarkStateItem }.asJava

  override def populate(): Unit = {
    departments.foreach { department =>
      state.put(department.code, new DepartmentMarkStateItem(department))
    }
  }
}

class DepartmentMarkStateItem {
  def this(department: Department) = {
    this()
    this.uploadCourseworkMarksToSits = department.uploadCourseworkMarksToSits

    val currentYear = AcademicYear.now()
    val previousYear = currentYear - 1

    this.openForPreviousYearUG = department.canUploadMarksToSitsForYear(previousYear, DegreeType.Undergraduate)
    this.openForPreviousYearPG = department.canUploadMarksToSitsForYear(previousYear, DegreeType.Postgraduate)
    this.openForCurrentYearUG = department.canUploadMarksToSitsForYear(currentYear, DegreeType.Undergraduate)
    this.openForCurrentYearPG = department.canUploadMarksToSitsForYear(currentYear, DegreeType.Postgraduate)
  }

  var uploadCourseworkMarksToSits: Boolean = _
  var openForPreviousYearUG: Boolean = _
  var openForPreviousYearPG: Boolean = _
  var openForCurrentYearUG: Boolean = _
  var openForCurrentYearPG: Boolean = _
}
