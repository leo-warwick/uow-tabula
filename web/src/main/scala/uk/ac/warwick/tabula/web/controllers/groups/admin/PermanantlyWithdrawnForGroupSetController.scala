package uk.ac.warwick.tabula.web.controllers.groups.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, _}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.data.model.{Member, Module, StudentMember}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController
import uk.ac.warwick.tabula.web.views.JSONView

object PermanantlyWithdrawnForGroupSetCommand {
	def apply(module: Module, set: SmallGroupSet) =
		new PermanantlyWithdrawnForGroupSetCommandInternal(module, set)
			with AutowiringProfileServiceComponent
			with ComposableCommand[Seq[String]]
			with PermanantlyWithdrawnForGroupSetPermissions
			with PermanantlyWithdrawnForGroupSetCommandState
			with Unaudited with ReadOnly
}


class PermanantlyWithdrawnForGroupSetCommandInternal(val module: Module, val set: SmallGroupSet)
	extends CommandInternal[Seq[String]] {

	self: ProfileServiceComponent =>

	override def applyInternal(): Seq[String] = {
		val setUniversityIds = set.allStudents.map(_.getWarwickId)
		val studentMembers = profileService.getAllMembersWithUniversityIds(setUniversityIds).flatMap {
			case (student: StudentMember) => Option(student)
			case (member: Member) => None
		}
		val withdrawnStudentIds = studentMembers.filter(_.permanentlyWithdrawn).map(_.universityId)
		val missing = setUniversityIds.diff(studentMembers.map(_.universityId))
		withdrawnStudentIds ++ missing
	}

}

trait PermanantlyWithdrawnForGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: PermanantlyWithdrawnForGroupSetCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(set, module)
		p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
	}

}

trait PermanantlyWithdrawnForGroupSetCommandState {
	def module: Module
	def set: SmallGroupSet
}

@Controller
@RequestMapping(Array("/groups/admin/module/{module}/groups/{smallGroupSet}/withdrawn"))
class PermanantlyWithdrawnForGroupSetController extends GroupsController {

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet): Appliable[Seq[String]] =
		PermanantlyWithdrawnForGroupSetCommand(module, set)

	@RequestMapping
	def submit(@ModelAttribute("command") cmd: Appliable[Seq[String]]): Mav = {
		Mav(new JSONView(Map(
			"students" -> cmd.apply()
		)))
	}

}
