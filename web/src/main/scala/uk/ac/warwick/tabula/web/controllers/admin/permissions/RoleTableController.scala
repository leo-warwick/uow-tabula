package uk.ac.warwick.tabula.web.controllers.admin.permissions

import uk.ac.warwick.tabula.web.controllers.DepartmentScopedController
import uk.ac.warwick.tabula.web.controllers.admin.AdminController
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable}
import uk.ac.warwick.tabula.roles.{SelectorBuiltInRoleDefinition, RoleDefinition}
import uk.ac.warwick.tabula.permissions.{SelectorPermission, Permissions, PermissionsSelector, Permission}
import uk.ac.warwick.tabula.data.model.{StudentRelationshipType, Department}
import uk.ac.warwick.tabula.helpers.ReflectionHelper
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent, RelationshipService}
import uk.ac.warwick.tabula.data.model.permissions.CustomRoleDefinition

@Controller
abstract class AbstractRoleTableController extends AdminController {

	var permissionsService: PermissionsService = Wire[PermissionsService]
	var relationshipService: RelationshipService = Wire[RelationshipService]

	type Response = Option[Boolean]
	val Allow: Response = Some(true)
	val Deny: Response = Some(false)
	val Partial: Response = None // allowed under certain circumstances

	type RolesTable = Seq[(Permission, Seq[(RoleDefinition, Response)])]

	private def parentDepartments(department: Department): Seq[Department] =
		if (mandatory(department).hasParent) department +: parentDepartments(department.parent)
		else Seq(department)

	private def baseName(roleDefinition: RoleDefinition): String = roleDefinition match {
		case custom: CustomRoleDefinition => baseName(custom.baseRoleDefinition)
		case _ => roleDefinition.getName.substring(0, 5)
	}

	protected def rolesTable(department: Option[Department]): RolesTable = {
		val builtInRoleDefinitions = ReflectionHelper.allBuiltInRoleDefinitions

		val allDepartments = department.toSeq.flatMap(parentDepartments)

		val relationshipTypes =
			if (allDepartments.isEmpty) relationshipService.allStudentRelationshipTypes.filter { _.defaultDisplay }
			else allDepartments.flatMap { _.displayedStudentRelationshipTypes }.distinct

		val selectorBuiltInRoleDefinitions =
			ReflectionHelper.allSelectorBuiltInRoleDefinitionNames.flatMap { name =>
				SelectorBuiltInRoleDefinition.of(name, PermissionsSelector.Any[StudentRelationshipType]) +:
					relationshipTypes.map { relationshipType =>
						SelectorBuiltInRoleDefinition.of(name, relationshipType)
					}
			}

		val customRoleDefinitions =
			allDepartments
				.flatMap { department => permissionsService.getCustomRoleDefinitionsFor(department) }
				.filterNot { _.replacesBaseDefinition }

		val allDefinitionsWithoutReplacements =
			(builtInRoleDefinitions ++ selectorBuiltInRoleDefinitions ++ customRoleDefinitions)
				.filter { _.isAssignable }
				.sortBy { _.allPermissions(Some(null)).size }
				.groupBy(baseName)
				.toSeq.sortBy { case (substr, defs) =>
					// Show selector ones first
					val selectorSort = defs.head match {
						case _: SelectorBuiltInRoleDefinition[_] => 0
						case _ => 1
					}

					(selectorSort, defs.map { _.allPermissions(Some(null)).size }.max, substr)
				}
				.flatMap { case (_, defs) => defs }

		val allDefinitions =
			allDefinitionsWithoutReplacements.map { definition =>
				(definition, allDepartments.flatMap { _.replacedRoleDefinitionFor(definition) }.headOption.getOrElse(definition))
			}

		def groupFn(p: Permission) = {
			val simpleName = Permissions.shortName(p.getClass)

			val parentName =
				if (simpleName.indexOf('.') == -1) ""
				else simpleName.substring(0, simpleName.lastIndexOf('.'))

			parentName
		}

		ReflectionHelper.allPermissions
			.filter { p => groupFn(p).hasText }
			.map { permission =>
				(permission, allDefinitions.map {
					case (actualDefinition, permsDefinition) => (actualDefinition, permsDefinition match {
						case definition: SelectorBuiltInRoleDefinition[StudentRelationshipType@unchecked] =>
							permission match {
								case _ if definition.mayGrant(permission) => Some(true)
								case selectorPermission: SelectorPermission[StudentRelationshipType@unchecked]
									if definition.allPermissions(Some(null)).exists { case (p, _) => p.getName == selectorPermission.getName} &&
										definition.selector <= selectorPermission.selector => None
								case _ => Some(false)
							}
						case definition => Some(definition.mayGrant(permission))
					})
				})
			}
			.filter { case (p, defs) => defs.exists { case (_, result) => result.isEmpty || result.exists { b => b } } }
			.sortBy { case (p, _) => groupFn(p) }
	}

}

@Controller
class RoleTableController extends AbstractRoleTableController {

	@RequestMapping(Array("/admin/roles"))
	def generic = Mav("admin/permissions/role_table", "rolesTable" -> rolesTable(None))

}

@Controller
class DepartmentRoleTableController extends AbstractRoleTableController
	with DepartmentScopedController with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	override val departmentPermission: Permission = Permissions.Department.ArrangeRoutesAndModules

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@RequestMapping(Array("/admin/department/{department}/roles")) def forDepartment(@PathVariable department: Department) =
		Mav("admin/permissions/role_table", "rolesTable" -> rolesTable(Some(department)))

}
