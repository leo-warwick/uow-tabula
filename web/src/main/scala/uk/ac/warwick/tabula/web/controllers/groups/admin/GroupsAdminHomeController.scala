package uk.ac.warwick.tabula.web.controllers.groups.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.PermissionDeniedException
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController
import scala.collection.JavaConverters._
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet


/**
 * Redirects to handle these otherwise nonexistent parent paths.
 */
@Controller
@RequestMapping(Array("/groups/admin", "/groups/admin/department", "/groups/admin/module"))
class GroupsAdminHomeController extends GroupsController {

	@RequestMapping(method=Array(GET, HEAD))
	def homeScreen(user: CurrentUser) = Redirect(Routes.home)

}
