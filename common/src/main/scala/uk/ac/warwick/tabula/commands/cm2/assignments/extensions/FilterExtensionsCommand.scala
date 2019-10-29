package uk.ac.warwick.tabula.commands.cm2.assignments.extensions

import org.hibernate.criterion.Order
import org.hibernate.criterion.Order._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.ScalaRestriction
import uk.ac.warwick.tabula.data.ScalaRestriction._
import uk.ac.warwick.tabula.data.model.forms.ExtensionState
import uk.ac.warwick.tabula.data.model.{Assignment, Department, Module}
import uk.ac.warwick.tabula.helpers.coursework.ExtensionGraph
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, PermissionDeniedException}

import scala.collection.JavaConverters._

case class FilterExtensionResults(
  extensions: Seq[ExtensionGraph],
  total: Int
)

object FilterExtensionsCommand {
  def apply(academicYear: AcademicYear, user: CurrentUser) =
    new FilterExtensionsCommandInternal(academicYear, user)
      with Command[FilterExtensionResults]
      with AutowiringUserLookupComponent
      with AutowiringExtensionServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with ReadOnly with Unaudited with Public
}

class FilterExtensionsCommandInternal(val academicYear: AcademicYear, val user: CurrentUser) extends CommandInternal[FilterExtensionResults]
  with FilterExtensionsState with TaskBenchmarking {

  self: UserLookupComponent with ExtensionServiceComponent with ModuleAndDepartmentServiceComponent =>

  import FiltersExtensions._

  private def includeChildDepartments(department: Department): Set[Department] =
    Set(department) ++ department.children.asScala.flatMap(includeChildDepartments)

  private def departmentsWithPermssion: Set[Department] =
    moduleAndDepartmentService.departmentsWithPermission(user, Permissions.Extension.Read)
      .flatMap(includeChildDepartments)

  private def modulesWithPermssion: Set[Module] =
    moduleAndDepartmentService.modulesWithPermission(user, Permissions.Extension.Read)

  private def modulesInDepartmentsWithPermission: Set[Module] =
    departmentsWithPermssion.flatMap(_.modules.asScala)

  lazy val allModules: Seq[Module] = (modulesWithPermssion ++ modulesInDepartmentsWithPermission).toSeq.sortBy(_.code)
  lazy val allDepartments: Seq[Department] = (departmentsWithPermssion ++ allModules.map(_.adminDepartment)).toSeq.sortBy(_.fullName)

  def applyInternal(): FilterExtensionResults = {
    // permission to manage extensions are all scoped by dept or module - we can't do normal permissions checking as there could be no scope to check against
    if (allDepartments.isEmpty)
      throw PermissionDeniedException(user, Permissions.Extension.Read, null)

    // on the off chance that someone has tried to hack extra departments or modules into the filter remove them
    departments = departments.asScala.filter(d => allDepartments.contains(d)).asJava
    modules = modules.asScala.filter(m => allModules.contains(m)).asJava

    // if no more specific module or department restrictions are specified we should filter based on the users permissions
    val defaultModuleRestrictions = inIfNotEmpty(
      "module.code", allModules.map(_.code),
      AliasPaths("module"): _*
    )

    val defaultDepartmentRestrictions = inIfNotEmpty(
      "department.code", allDepartments.map(_.code),
      AliasPaths("department"): _*
    )

    val restrictions: Seq[ScalaRestriction] = Seq(
      academicYearRestriction,
      receivedRestriction,
      stateRestriction,
      assignmentRestriction,
      moduleRestriction.orElse(defaultModuleRestrictions),
      departmentRestriction.orElse(defaultDepartmentRestrictions)
    ).flatten

    val totalResults = benchmarkTask("countExtensionsByRestrictions") {
      extensionService.countFilteredExtensions(
        restrictions = restrictions
      )
    }

    val orders = if (sortOrder.isEmpty) defaultOrder else sortOrder

    val extensions = benchmarkTask("findExtensionsByRestrictions") {
      extensionService.filterExtensions(
        restrictions,
        buildOrders(orders.asScala.toSeq),
        extensionsPerPage,
        extensionsPerPage * (page - 1)
      )
    }

    val graphs = extensions.map(e => ExtensionGraph(e, userLookup.getUserByUserId(e.usercode)))
    FilterExtensionResults(graphs, totalResults)
  }
}

trait FilterExtensionsState extends FiltersExtensions {
  var page = 1
  var extensionsPerPage = 50
  var defaultOrder: JList[Order] = Seq(desc("requestedOn")).asJava
  var sortOrder: JList[Order] = JArrayList()

  var times: JList[TimeFilter] = JArrayList()
  var states: JList[ExtensionState] = JArrayList()
  var assignments: JList[Assignment] = JArrayList()
  var modules: JList[Module] = JArrayList()
  var departments: JList[Department] = JArrayList()

  def academicYear: AcademicYear

  def user: CurrentUser

  def allModules: Seq[Module]

  def allDepartments: Seq[Department]

  lazy val allStates: Seq[ExtensionState] = ExtensionState.all
  lazy val allTimes: Seq[TimeFilter] = TimeFilter.all
}
