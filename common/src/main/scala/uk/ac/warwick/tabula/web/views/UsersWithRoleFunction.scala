package uk.ac.warwick.tabula.web.views

import freemarker.template.TemplateMethodModelEx
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import org.springframework.beans.factory.annotation.Autowired
import freemarker.template.TemplateModel
import freemarker.template.TemplateModelException
import freemarker.template.utility.DeepUnwrap
import uk.ac.warwick.tabula.roles.RoleDefinition
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.UserLookupService
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.permissions.BuiltInRoleDefinitionUserType

/**
 * Returns a list of users with the current role, ordered by surname, firstname.
 *
 * Accepts exactly two arguments, the role definition and the scope
 */
class UsersWithRoleFunction extends TemplateMethodModelEx {

	@Autowired var permissionsService: PermissionsService = _
	@Autowired var userLookup: UserLookupService = _
	lazy val roleDefinitionConverter = new BuiltInRoleDefinitionUserType

	override def exec(args: java.util.List[_]): Object = {
		val arguments = args.asInstanceOf[java.util.List[TemplateModel]]

		if (arguments == null || args.size() != 2) throw new TemplateModelException("Invalid number of arguments")

		val roleName = DeepUnwrap.unwrap(arguments.get(0)).asInstanceOf[String]

		val roleDefinition =
			permissionsService.getCustomRoleDefinitionById(roleName).getOrElse {
				roleDefinitionConverter.convertToObject(roleName)
			}

		val scope = DeepUnwrap.unwrap(arguments.get(1)).asInstanceOf[PermissionsTarget]

		for {
			role <- permissionsService.getGrantedRole(scope, roleDefinition).toSeq
			user <- role.users.users
		} yield user
	}

}