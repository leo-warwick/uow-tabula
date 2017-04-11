package uk.ac.warwick.tabula.commands.profiles

import org.hibernate.validator.constraints.NotEmpty
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Command, Description, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.{Member, MemberUserType}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.elasticsearch.AutowiringProfileQueryServiceComponent

abstract class AbstractSearchProfilesCommand(val user: CurrentUser, firstUserType: MemberUserType, otherUserTypes: MemberUserType*)
	extends Command[Seq[Member]]
		with ReadOnly
		with Unaudited
		with AbstractSearchProfilesCommandState
		with AutowiringProfileServiceComponent
		with AutowiringSecurityServiceComponent
		with AutowiringModuleAndDepartmentServiceComponent
 		with AutowiringProfileQueryServiceComponent {

	import AbstractSearchProfilesCommand._

	final val userTypes: Set[MemberUserType] = Set(firstUserType) ++ otherUserTypes

	def validQuery: Boolean =
		(query.trim().length >= MinimumQueryLength) &&
		query.split("""\s+""").exists({_.length >= MinimumTermLength})

	private def singletonByUserType(option: Option[Member]) = option match {
		case Some(member) => Seq(member) filter {userTypes contains _.userType}
		case _ => Seq()
	}

	private def canRead(member: Member) = securityService.can(user, Permissions.Profiles.Read.Core, member)

	def usercodeMatches: Seq[Member] =
		if (!isMaybeUsercode(query)) Seq()
		else profileService.getAllMembersWithUserId(query).filter {m => userTypes.contains(m.userType)}.filter(canRead)

	def universityIdMatches: Seq[Member] =
		if (!isMaybeUniversityId(query)) Seq()
		else singletonByUserType(profileService.getMemberByUniversityId(query)) filter canRead

	override def describe(d: Description): Unit = d.property("query" -> query)

}

object AbstractSearchProfilesCommand {

	/** The minimum length of the whole query */
	val MinimumQueryLength = 3

	/** The minimum length of at least one term in the query, avoids searches for "m m m" getting through */
	val MinimumTermLength = 2

	private val UniversityIdPattern = """^\d{7,}$"""

	private val UsercodePattern = """^[A-Za-z0-9]{5,}$"""

	def isMaybeUniversityId(query: String): Boolean = query.trim matches UniversityIdPattern

	def isMaybeUsercode(query: String): Boolean = query.trim matches UsercodePattern

}

trait AbstractSearchProfilesCommandState {
	@NotEmpty(message = "{NotEmpty.profiles.searchQuery}")
	var query: String = _

	var searchAllDepts: Boolean = _
}
