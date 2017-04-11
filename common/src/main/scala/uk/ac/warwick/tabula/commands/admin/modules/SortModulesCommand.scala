package uk.ac.warwick.tabula.commands.admin.modules

import scala.collection.JavaConverters._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.mutable

object SortModulesCommand {
	def apply(department: Department) =
		new SortModulesCommandInternal(department)
				with ComposableCommand[Unit]
				with SortModulesCommandGrouping
				with SortModulesCommandPermissions
				with SortModulesCommandDescription
				with SortModulesCommandValidation
				with AutowiringModuleAndDepartmentServiceComponent
}

/** Arranges modules between a department and its child departments. */
class SortModulesCommandInternal(val department: Department) extends CommandInternal[Unit] with SortModulesCommandState with SortModulesCommandGrouping {
	self: ModuleAndDepartmentServiceComponent =>

	def applyInternal(): Unit = transactional() {
		for ((dept, modules) <- mapping.asScala) {
			dept.modules.clear()
			dept.modules.addAll(modules)
			for (m <- modules.asScala) m.adminDepartment = dept
			moduleAndDepartmentService.saveOrUpdate(dept)
		}
	}
}

trait SortModulesCommandGrouping {
	self: SortModulesCommandState =>

	// Only called on initial form view
	def populate() {
		for (dept <- (departments))
			mapping.put(dept, JArrayList(dept.modules))
	}

	// Sort all the lists of modules by code.
	def sort() {
		// Because sortBy is not an in-place sort, we have to replace the lists entirely.
		// Alternative is Collections.sort or math.Sorting but these would be more code.
		for ((dept, modules) <- mapping.asScala) {
			mapping.put(dept, JArrayList(modules.asScala.toList.filter(validModule).sorted))
		}
	}
}

trait SortModulesCommandValidation extends SelfValidating {
	self: SortModulesCommandState =>

	def validate(errors: Errors) {
		val mappingMap = mapping.asScala
		val currentModules = departments.map(_.modules.asScala).flatten.toList
		val newModules = mappingMap.values.map(_.asScala).flatten.toList.filter(validModule)

		/* These next errors shouldn't really be possible from the UI unless there's a bug. */

		// Disallow submitting unrelated Departments
		if (!mappingMap.keys.forall( d => departments.contains(d) )) {
			errors.reject("sortModules.departments.invalid")
		}

		// Disallow referencing any Modules from other departments.
		// Also disallow removing modules from the ones we started with.
		if (newModules.sorted != currentModules.sorted) {
			errors.reject("sortModules.modules.invalid")
		}
	}
}

trait SortModulesCommandState extends GroupsObjects[Module, Department] {
	def department: Department
	def departments: List[Department] = (department :: department.children.asScala.toList)

	protected def validModule(module: Module): Boolean = module.code != null

	// Purely for use by Freemarker as it can't access map values unless the key is a simple value.
	// Do not modify the returned value!
	def mappingByCode: mutable.Map[String, _root_.uk.ac.warwick.tabula.JavaImports.JList[Module]] = mapping.asScala.map {
		case (dept, modules) => (dept.code, modules)
	}
}

trait SortModulesCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: SortModulesCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.ArrangeRoutesAndModules, mandatory(department))
	}
}

trait SortModulesCommandDescription extends Describable[Unit] {
	self: SortModulesCommandState =>

	def describe(d: Description): Unit = d.department(department)
}