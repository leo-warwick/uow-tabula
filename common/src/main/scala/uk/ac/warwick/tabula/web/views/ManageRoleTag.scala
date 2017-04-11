package uk.ac.warwick.tabula.web.views

import freemarker.template.TemplateDirectiveModel
import freemarker.template.TemplateDirectiveBody
import freemarker.template.TemplateModel
import freemarker.core.Environment
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.UserLookupService
import freemarker.template.utility.DeepUnwrap
import uk.ac.warwick.tabula.JavaImports._
import freemarker.ext.beans.BeansWrapper
import freemarker.template.TemplateException
import collection.JavaConversions._
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.data.model.permissions.PermissionUserType
import uk.ac.warwick.tabula.data.model.permissions.BuiltInRoleDefinitionUserType
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.roles.RoleDefinition
import uk.ac.warwick.tabula.permissions.Permission

class ManageRoleTag extends TemplateDirectiveModel {

	@Autowired var securityService: SecurityService = _
	@Autowired var permissionsService: PermissionsService = _

	lazy val permissionsConverter = new PermissionUserType
	lazy val roleDefinitionConverter = new BuiltInRoleDefinitionUserType

	override def execute(env: Environment,
		params: JMap[_, _],
		loopVars: Array[TemplateModel],
		body: TemplateDirectiveBody): Unit = {

		val wrapper = env.getObjectWrapper()

		val user = RequestInfo.fromThread.get.user

		val permission = unwrap(params.get("permission")).asInstanceOf[Permission]
		val roleDefinition = unwrap(params.get("roleDefinition")).asInstanceOf[RoleDefinition]
		val roleName = unwrap(params.get("roleName")).asInstanceOf[String]
		val permissionName = unwrap(params.get("permissionName")).asInstanceOf[String]
		val scope = unwrap(params.get("scope")).asInstanceOf[PermissionsTarget]

		if (body == null) {
			throw new TemplateException("ManageRoleTag: must have a body", env);
		}

		if (permissionName != null) {
			val perm =
				if (permission != null) permission
				else permissionsConverter.convertToObject(permissionName)
			val canDelegate = securityService.canDelegate(user, perm, scope) || user.god

			val deniedPermissions =
				if (canDelegate) Seq()
				else Seq(perm)

			env.getCurrentNamespace().put("can_delegate", wrapper.wrap(canDelegate))
			env.getCurrentNamespace().put("denied_permissions", wrapper.wrap(deniedPermissions))
		} else if (roleDefinition != null || roleName != null) {
			val definition =
				if (roleDefinition != null) roleDefinition
				else permissionsService.getCustomRoleDefinitionById(roleName).getOrElse {
					roleDefinitionConverter.convertToObject(roleName)
				}

			val allPermissions = definition.allPermissions(Option(scope)).keys
			val deniedPermissions = allPermissions.filterNot(securityService.canDelegate(user, _, scope))

			val canDelegate = deniedPermissions.isEmpty || user.god

			env.getCurrentNamespace().put("can_delegate", wrapper.wrap(canDelegate))
			env.getCurrentNamespace().put("denied_permissions", wrapper.wrap(deniedPermissions))
		} else {
			throw new TemplateException("ManageRoleTag: Either permissionName or roleName must be specified", env)
		}

		body.render(env.getOut())
	}

	def unwrap(obj: Any): AnyRef = {
		if (obj == null) null
		else DeepUnwrap.unwrap(obj.asInstanceOf[TemplateModel])
	}

}