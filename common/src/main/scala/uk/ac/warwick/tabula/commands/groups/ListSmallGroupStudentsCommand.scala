package uk.ac.warwick.tabula.commands.groups

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, MemberOrUser, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.ProfileService

/**
 * Generates a list of students on small group, for displaying to
 * owners or tutors of that group.
 */
class ListSmallGroupStudentsCommand(val group: SmallGroup) extends Command[Seq[MemberOrUser]] with Unaudited with ReadOnly {

	var profileService: ProfileService = Wire[ProfileService]

	PermissionCheck(Permissions.SmallGroups.ReadMembership, group)

	override def applyInternal(): Seq[MemberOrUser] = {
		group.students.users.map { user =>
			val member = profileService.getMemberByUniversityId(user.getWarwickId)
			MemberOrUser(member, user)
		}.sortBy { s => (s.lastName,s.firstName) }
	}

}
