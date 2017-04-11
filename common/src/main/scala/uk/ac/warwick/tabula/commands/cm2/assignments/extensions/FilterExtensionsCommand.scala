package uk.ac.warwick.tabula.commands.cm2.assignments.extensions

import org.hibernate.criterion.Order
import org.hibernate.criterion.Order._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.ScalaRestriction
import uk.ac.warwick.tabula.data.ScalaRestriction._
import uk.ac.warwick.tabula.data.model.{Assignment, Department, Module}
import uk.ac.warwick.tabula.data.model.forms.ExtensionState
import uk.ac.warwick.tabula.helpers.coursework.ExtensionGraph
import uk.ac.warwick.tabula.permissions.Permissions

import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.Public

import scala.collection.JavaConverters._

case class FilterExtensionResults(
	extensions: Seq[ExtensionGraph],
	total: Int
)

object FilterExtensionsCommand {
	def apply(user: CurrentUser) =
		new FilterExtensionsCommandInternal(user)
			with Command[FilterExtensionResults]
			with AutowiringUserLookupComponent
			with AutowiringExtensionServiceComponent
			with AutowiringModuleAndDepartmentServiceComponent
			with AutowiringTermServiceComponent
			with ReadOnly with Unaudited with Public
}

class FilterExtensionsCommandInternal(val user: CurrentUser) extends CommandInternal[FilterExtensionResults]
	with FilterExtensionsState with TaskBenchmarking {

	this: UserLookupComponent with ExtensionServiceComponent with ModuleAndDepartmentServiceComponent
		with TermServiceComponent =>

	import FiltersExtensions._

	private def departmentsWithPermssion =
		moduleAndDepartmentService.departmentsWithPermission(user, Permissions.Department.ManageExtensionSettings)
	private def modulesWithPermssion =
		moduleAndDepartmentService.modulesWithPermission(user, Permissions.Module.Administer)
	private def modulesInDepartmentsWithPermission =
		moduleAndDepartmentService.modulesInDepartmentsWithPermission(user, Permissions.Department.ManageExtensionSettings)

	lazy val allModules: Seq[Module] = (modulesWithPermssion ++ modulesInDepartmentsWithPermission).toSeq.sortBy(_.code)
	lazy val allDepartments: Seq[Department] = (departmentsWithPermssion ++ allModules.map(_.adminDepartment)).toSeq.sortBy(_.fullName)

	def applyInternal(): FilterExtensionResults = {

		// on the off chance that someone has tried to hack extra departments or modules into the filter remove them
		departments = departments.asScala.filter(d => allDepartments.contains(d)).asJava
		modules = modules.asScala.filter(m => allModules.contains(m)).asJava

		// if no more specific module or department restrictions are specified we should filter based on the users permissions
		val defaultModuleRestrictions = inIfNotEmpty(
			"module.code", allModules.map(_.code),
			AliasPaths("module") : _*
		)

		val defaultDepartmentRestrictions = inIfNotEmpty(
			"department.code", allDepartments.map(_.code),
			AliasPaths("department") : _*
		)

		val restrictions: Seq[ScalaRestriction] = Seq(
			receivedRestriction,
			stateRestriction,
			assignmentRestriction,
			moduleRestriction.orElse(defaultModuleRestrictions),
			departmentRestriction.orElse(defaultDepartmentRestrictions)
		).flatten

		val totalResults = benchmarkTask("countStudentsByRestrictions") { extensionService.countFilteredExtensions(
			restrictions = restrictions
		)}

		val orders = if (sortOrder.isEmpty) defaultOrder else sortOrder

		val extensions = benchmarkTask("findStudentsByRestrictions") { extensionService.filterExtensions(
			restrictions,
			buildOrders(orders.asScala),
			extensionsPerPage,
			extensionsPerPage * (page-1)
		)}

		val graphs = extensions.map(e => ExtensionGraph(e, userLookup.getUserByUserId(e.usercode)))
		FilterExtensionResults(graphs, totalResults)
	}
}

trait FilterExtensionsState extends FiltersExtensions {

	self: TermServiceComponent =>

	var page = 1
	var extensionsPerPage = 50
	var defaultOrder: JList[Order] = Seq(desc("requestedOn")).asJava
	var sortOrder: JList[Order] = JArrayList()

	var times: JList[TimeFilter] = JArrayList()
	var states: JList[ExtensionState] = JArrayList()
	var assignments: JList[Assignment] = JArrayList()
	var modules: JList[Module] = JArrayList()
	var departments: JList[Department] = JArrayList()

	val user: CurrentUser
	val allModules: Seq[Module]
	val allDepartments: Seq[Department]
	lazy val allStates: Seq[ExtensionState with Product with Serializable] = ExtensionState.all
	lazy val allTimes: Seq[TimeFilter with Product with Serializable] = TimeFilter.all
}