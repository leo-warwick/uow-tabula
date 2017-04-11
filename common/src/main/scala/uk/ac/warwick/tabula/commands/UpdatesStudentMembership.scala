package uk.ac.warwick.tabula.commands

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.UniversityId
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.web.bind.AbstractPropertyEditor

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

trait UpdatesStudentMembership {
	self: HasAcademicYear with SpecifiesGroupType with UserLookupComponent with AssessmentMembershipServiceComponent =>

	// needs a module to determine the possible options from SITS
	def module: Module
	def existingGroups: Option[Seq[UpstreamAssessmentGroup]]
	def existingMembers: Option[UnspecifiedTypeUserGroup]

	/**
	 * Convert Spring-bound upstream group references to an AssessmentGroup buffer
	 */
	def updateAssessmentGroups()

	/** linked assessment groups, which are to be persisted with the assignment.
		*  They can be used to lookup SITS UpstreamAssessmentGroups on demand,
		*  when required to lookup users.
		*/
	var assessmentGroups: JList[AssessmentGroup] = JArrayList()

	// bind property for items to be added to members.includeUsers
	@transient var includeUsers: JList[String] = JArrayList()

	// bind property for items to either be removed from members.includeUsers or added to members.excludeUsers
	@transient var excludeUsers: JList[String] = JArrayList()

	// bind property for the big free-for-all textarea of usercodes/uniIDs to add.
	// These are first resolved to userIds and then added to includeUsers
	@transient var massAddUsers: String = _

	// retains the result of massAddUsers before it is cleared
	@transient var originalMassAddUsers: String = _

	/** bind property for changing assessment groups */
	@transient var upstreamGroups: JList[UpstreamGroup] = JArrayList()

	/**
	 * If copying from existing object, this must be a DEEP COPY
	 * with changes copied back to the original UserGroup, don't pass
	 * the same UserGroup around because it'll just cause Hibernate
	 * problems. This copy should be transient.
	 *
	 * Changes to members are done via includeUsers and excludeUsers, since
	 * it is difficult to bind additions and removals directly to a collection
	 * with Spring binding.
	 */
	var members: UnspecifiedTypeUserGroup = {
		val ug =
			if (updateStudentMembershipGroupIsUniversityIds) UserGroup.ofUniversityIds
		  else UserGroup.ofUsercodes

		ug.userLookup = userLookup
		ug
	}


	// parse massAddUsers into a collection of individual tokens
	def massAddUsersEntries: Seq[String] =
		if (massAddUsers == null) Nil
		else massAddUsers
			.split("(\\s|[^A-Za-z\\d\\-_\\.])+")
			.map(_.trim)
			.filterNot(_.isEmpty)

	def afterBind() {
		updateMembership()
		updateAssessmentGroups()
	}

	private def bufferUserFromUserId(userId: String, buffer: ListBuffer[User]) {
		val user = userLookup.getUserByUserId(userId)
		if (user.isFoundUser) {
			buffer += user
		}
	}

	private def bufferValidUser(userString: String, buffer: ListBuffer[User]) {
		if (UniversityId.isValid(userString)) {
			val user = userLookup.getUserByWarwickUniId(userString)
			if (user.isFoundUser) {
				buffer += user
			} else {
				bufferUserFromUserId(userString, buffer)
			}
		} else {
			bufferUserFromUserId(userString, buffer)
		}
	}

	/**
	 * Convert Spring-bound user lists into an explicit UserGroup
	 */
	private def updateMembership() {

		// buffers to hold the users we're adding/removing
		val usersToAdd = ListBuffer[User]()
		val usersToExclude = ListBuffer[User]()

		// parse items from textarea into usersToAdd list
		for (item <- massAddUsersEntries) {
			bufferValidUser(item, usersToAdd)
		}

		// now add the users from includeUsers
		for (includedUser <- includeUsers.asScala) {
			bufferValidUser(includedUser, usersToAdd)
		}

		// now get implicit membership list from upstream
		val upstreamMembers = existingGroups.map(assessmentMembershipService.determineMembershipUsers(_, existingMembers)).getOrElse(Seq())

		for (user <- usersToAdd.distinct) {
			if (members.excludes.contains(user)) {
				members.unexclude(user)
			}
			else if (!upstreamMembers.contains(user)) {
				// TAB-399 only add if not already a member of a linked UpstreamGroup
				members.add(user)
			}
		}

		// uninclude from previously-added users, or explicitly exclude as appropriate
		for (excludedUser <- excludeUsers.asScala) {
			bufferValidUser(excludedUser, usersToExclude)
		}

		for (user <- usersToExclude.distinct) {
			if (members.users contains user) {
				members.remove(user)
			}else{
				members.exclude(user)
			}
		}

		// clear these local properties, as we've "moved" the data into members
		originalMassAddUsers = massAddUsers
		massAddUsers = ""
		includeUsers = JArrayList()
		excludeUsers = JArrayList()
	}

	/**
	 * Build a seq of available upstream groups for assessment components on this module
	 */
	lazy val availableUpstreamGroups: Seq[UpstreamGroup] = {
		for {
			ua <- assessmentMembershipService.getAssessmentComponents(module)
			uag <- assessmentMembershipService.getUpstreamAssessmentGroups(ua, academicYear)
		} yield new UpstreamGroup(ua, uag)
	}

	/**
		* All upstream groups whether in use or not
		*/
	lazy val allUpstreamGroups: Seq[UpstreamGroup] = {
		for {
			ua <- assessmentMembershipService.getAssessmentComponents(module, inUseOnly = false)
			uag <- assessmentMembershipService.getUpstreamAssessmentGroups(ua, academicYear)
		} yield new UpstreamGroup(ua, uag)
	}


	/** get UAGs, populated with membership, from the currently stored assessmentGroups */
	def linkedUpstreamAssessmentGroups: Seq[UpstreamAssessmentGroup] =
		if (assessmentGroups == null) Seq()
		else assessmentGroups.asScala.flatMap { _.toUpstreamAssessmentGroup(academicYear) }

	/**
	 * Returns a sequence of MembershipItems
	 */
	def membershipInfo : AssessmentMembershipInfo = assessmentMembershipService.determineMembership(linkedUpstreamAssessmentGroups, Option(members))

}

/**
 * convenience classes
 */
class UpstreamGroup(val assessmentComponent: AssessmentComponent, val group: UpstreamAssessmentGroup) {
	val id: String = assessmentComponent.id + ";" + group.id

	val name: String = assessmentComponent.name
	val cats: Option[String] = assessmentComponent.cats
	val occurrence: String = group.occurrence
	val sequence: String = assessmentComponent.sequence
	val assessmentType: String = Option(assessmentComponent.assessmentType).map(_.value).orNull // TAB-1174 remove Option wrap when non-null is in place

	def isLinked(assessmentGroups: JList[AssessmentGroup]): Boolean = assessmentGroups.asScala.exists(ag =>
		ag.assessmentComponent.id == assessmentComponent.id && ag.occurrence == group.occurrence)

	override def toString: String = "assessmentComponent: " + assessmentComponent.id + ", occurrence: " + group.occurrence
}


class UpstreamGroupPropertyEditor extends AbstractPropertyEditor[UpstreamGroup] {
	var membershipService: AssessmentMembershipService = Wire.auto[AssessmentMembershipService]

	override def fromString(id: String): UpstreamGroup = {
		def explode = throw new IllegalArgumentException("No unique upstream group which matches id " + id + " is available")

		id.split(";") match {
			case Array(uaId: String, groupId: String) =>
				val ua = membershipService.getAssessmentComponent(uaId).getOrElse(explode)
				val uag = membershipService.getUpstreamAssessmentGroup(groupId).getOrElse(explode)
				new UpstreamGroup(ua, uag)
			case _ => explode
		}
	}

	override def toString(ug: UpstreamGroup): String = ug.id
}
trait SpecifiesGroupType{
	val updateStudentMembershipGroupIsUniversityIds: Boolean
}
