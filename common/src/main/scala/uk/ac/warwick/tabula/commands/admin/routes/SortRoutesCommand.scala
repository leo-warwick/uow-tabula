package uk.ac.warwick.tabula.commands.admin.routes

import scala.collection.JavaConverters._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.mutable

object SortRoutesCommand {
	def apply(department: Department) =
		new SortRoutesCommandInternal(department)
			with ComposableCommand[Unit]
			with SortRoutesCommandGrouping
			with SortRoutesCommandPermissions
			with SortRoutesCommandDescription
			with SortRoutesCommandValidation
			with AutowiringModuleAndDepartmentServiceComponent
}

/** Arranges routes between a department and its child departments. */
class SortRoutesCommandInternal(val department: Department) extends CommandInternal[Unit] with SortRoutesCommandState with SortRoutesCommandGrouping {
	self: ModuleAndDepartmentServiceComponent =>

	def applyInternal(): Unit = transactional() {
		for ((dept, routes) <- mapping.asScala) {
			dept.routes.clear()
			dept.routes.addAll(routes)
			for (m <- routes.asScala) m.adminDepartment = dept
			moduleAndDepartmentService.saveOrUpdate(dept)
		}
	}
}

trait SortRoutesCommandGrouping {
	self: SortRoutesCommandState =>

	// Only called on initial form view
	def populate() {
		for (dept <- (departments))
			mapping.put(dept, JArrayList(dept.routes))
	}

	// Sort all the lists of routes by code.
	def sort() {
		// Because sortBy is not an in-place sort, we have to replace the lists entirely.
		// Alternative is Collections.sort or math.Sorting but these would be more code.
		for ((dept, routes) <- mapping.asScala) {
			mapping.put(dept, JArrayList(routes.asScala.toList.filter(validRoute).sorted))
		}
	}
}

trait SortRoutesCommandValidation extends SelfValidating {
	self: SortRoutesCommandState =>

	def validate(errors: Errors) {
		val mappingMap = mapping.asScala
		val currentRoutes = departments.map(_.routes.asScala).flatten.toList
		val newRoutes = mappingMap.values.map(_.asScala).flatten.toList.filter(validRoute)

		/* These next errors shouldn't really be possible from the UI unless there's a bug. */

		// Disallow submitting unrelated Departments
		if (!mappingMap.keys.forall( d => departments.contains(d) )) {
			errors.reject("sortRoutes.departments.invalid")
		}

		// Disallow referencing any Routes from other departments.
		// Also disallow removing routes from the ones we started with.
		if (newRoutes.sorted != currentRoutes.sorted) {
			errors.reject("sortRoutes.routes.invalid")
		}
	}
}

trait SortRoutesCommandState extends GroupsObjects[Route, Department] {
	def department: Department
	def departments: List[Department] = (department :: department.children.asScala.toList)

	protected def validRoute(route: Route): Boolean = route.code != null

	// Purely for use by Freemarker as it can't access map values unless the key is a simple value.
	// Do not modify the returned value!
	def mappingByCode: mutable.Map[String, _root_.uk.ac.warwick.tabula.JavaImports.JList[Route]] = mapping.asScala.map {
		case (dept, routes) => (dept.code, routes)
	}
}

trait SortRoutesCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: SortRoutesCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.ArrangeRoutesAndModules, mandatory(department))
	}
}

trait SortRoutesCommandDescription extends Describable[Unit] {
	self: SortRoutesCommandState =>

	def describe(d: Description): Unit = d.department(department)
}