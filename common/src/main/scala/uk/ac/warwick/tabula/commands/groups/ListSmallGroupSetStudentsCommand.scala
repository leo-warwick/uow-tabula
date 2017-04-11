package uk.ac.warwick.tabula.commands.groups

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{ProfileServiceComponent, AutowiringProfileServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}

case class StudentsInformation (
	smallGroupSet: SmallGroupSet,
	members: Seq[MemberOrUser],
	userIsMember: Boolean,
  showTutors: Boolean
)

object ListSmallGroupSetStudentsCommand {
	def apply(smallGroupSet: SmallGroupSet, user: CurrentUser): ListSmallGroupSetStudentsCommandInternal with ComposableCommand[StudentsInformation] with AutowiringProfileServiceComponent with ListSmallGroupSetStudentsCommandPermissions with Unaudited with ReadOnly = {
		new ListSmallGroupSetStudentsCommandInternal(smallGroupSet, user)
			with ComposableCommand[StudentsInformation]
			with AutowiringProfileServiceComponent
			with ListSmallGroupSetStudentsCommandPermissions
			with Unaudited with ReadOnly
	}
}

class ListSmallGroupSetStudentsCommandInternal(val smallGroupSet: SmallGroupSet, val user: CurrentUser)
	extends CommandInternal[StudentsInformation] with ListSmallGroupSetStudentsCommandState  {
	self:ProfileServiceComponent =>

		override def applyInternal(): StudentsInformation = {
				val students = smallGroupSet.allStudents
				val userIsMember = students.exists(_.getWarwickId == user.universityId)
				val showTutors = smallGroupSet.studentsCanSeeTutorName

				val members = students.map { user =>
						val member = profileService.getMemberByUniversityId(user.getWarwickId)
						MemberOrUser(member, user)
				}

				StudentsInformation(smallGroupSet, members, userIsMember, showTutors)
			}
}

trait ListSmallGroupSetStudentsCommandState {
	val user: CurrentUser
	val smallGroupSet: SmallGroupSet
}

trait ListSmallGroupSetStudentsCommandPermissions extends RequiresPermissionsChecking {
	self:ListSmallGroupSetStudentsCommandState =>
		def permissionsCheck(p: PermissionsChecking) {
			p.PermissionCheck(Permissions.SmallGroups.ReadMembership, smallGroupSet)
		}
}