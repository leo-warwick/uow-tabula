package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.userlookup.User

/**
 * Wrapper that exposes properties from Member if available, with User
 * as a fallback. Useful where we get some User objects from a UserGroup
 * but want to look up profiles as preferred info where available.
 *
 * Currently only used in one place but feel free to expand this out to
 * something more generally usable.
 *
 * TODO maybe Member could directly implement the trait
 */
object MemberOrUser {
	def apply(member: Member): MemberOrUser = WrappedMember(member)
	def apply(user: User): MemberOrUser = WrappedUser(user)
	def apply(member: Option[Member], user: User): MemberOrUser = member map { WrappedMember } getOrElse WrappedUser(user)
}

sealed trait MemberOrUser{
	def isMember: Boolean
	def fullName: Option[String]
	def firstName: String
	def lastName: String
	def universityId: String
	def usercode: String
	def shortDepartment: String
	def email: String
	def asUser: User
	def asMember: Option[Member]

	override def hashCode: Int = universityId.hashCode

	override def equals(other: Any): Boolean = other match {
		case other: MemberOrUser => other.universityId == universityId
		case _ => false
	}
}

private case class WrappedUser(user: User) extends MemberOrUser {
	def isMember = false
	def fullName = Some(user.getFullName)
	def firstName: String = user.getFirstName
	def lastName: String = user.getLastName
	def universityId: String = user.getWarwickId
	def usercode: String = user.getUserId
	def shortDepartment: String = user.getShortDepartment
	def email: String = user.getEmail
	def asUser: User = user
	def asMember = None
}

private case class WrappedMember(member: Member) extends MemberOrUser {
	def isMember = true
	def fullName: Option[String] = member.fullName
	def firstName: String = member.firstName
	def lastName: String = member.lastName
	def universityId: String = member.universityId
	def usercode: String = member.userId
	def shortDepartment: String = member.homeDepartment.name
	def email: String = member.email
	def asUser: User = member.asSsoUser
	def asMember = Some(member)
}