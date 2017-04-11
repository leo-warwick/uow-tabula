package uk.ac.warwick.tabula.web.views

import org.springframework.beans.factory.annotation.Autowired
import freemarker.template.utility.DeepUnwrap
import freemarker.template.TemplateModel
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.RequestInfo
import freemarker.template.TemplateMethodModelEx
import uk.ac.warwick.tabula.permissions.{ScopelessPermission, Permissions, PermissionsTarget, SelectorPermission}
import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

/**
 * Freemarker directive to show the contents of the tag
 */
class PermissionFunction extends TemplateMethodModelEx with Logging {

	@Autowired var securityService: SecurityService = _

	override def exec(args: JList[_]): Object = {

		val currentUser = RequestInfo.fromThread.get.user

		val arguments = args.asScala.toSeq.map { model => DeepUnwrap.unwrap(model.asInstanceOf[TemplateModel]) }

		arguments match {
			case Seq(actionName: String, item: PermissionsTarget) =>
				securityService.can(currentUser, Permissions.of(actionName), item): JBoolean
			case Seq(actionName: String, item: PermissionsTarget, selector: StudentRelationshipType) => // FIXME hard-coded
				securityService.can(currentUser, SelectorPermission.of(actionName, selector), item): JBoolean
			case Seq(actionName: String) =>
				val perm = Permissions.of(actionName)
				if (perm.isInstanceOf[ScopelessPermission]){
					securityService.can(currentUser, perm.asInstanceOf[ScopelessPermission]): JBoolean
				} else throw new IllegalArgumentException("Not a scopeless permission")
			case _ => throw new IllegalArgumentException("Bad args")
		}
	}
}