package uk.ac.warwick.tabula.commands.profiles

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data.model.MemberUserType.{Staff, Student}
import uk.ac.warwick.tabula.data.model.{Department, Member}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.ModuleAndDepartmentServiceComponent
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object SearchProfilesCommand {
	def apply(currentMember: Member, user: CurrentUser) =
	new SearchProfilesCommandInternal(currentMember, user)
	with ComposableCommand[Seq[Member]]
	with Unaudited
	with SearchProfilesCommandPermissions
}

class SearchProfilesCommandInternal(val currentMember: Member, user: CurrentUser) extends AbstractSearchProfilesCommand(user, Student, Staff)
	with CommandInternal[Seq[Member]] {

	override def applyInternal(): Seq[Member] =
		if (validQuery) usercodeMatches ++ universityIdMatches ++ queryMatches
		else Seq()

	private def queryMatches = {
		val depts = (currentMember.affiliatedDepartments ++ moduleAndDepartmentService.departmentsWithPermission(user, Permissions.Profiles.ViewSearchResults)).distinct
		val deptsAndDescendantDepts = depts.flatMap(dept => {
			def children(d: Department): Set[Department] = {
				if (!d.hasChildren) Set(d)
				else Set(d) ++ d.children.asScala.toSet.flatMap(children)
			}
			Set(dept) ++ children(dept)
		})
		profileService.findMembersByQuery(query, deptsAndDescendantDepts, userTypes, searchAllDepts)
	}
}

trait SearchProfilesCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	override def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.Profiles.Search)
	}
}

