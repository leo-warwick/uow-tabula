package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.beans.{BeanWrapper, BeanWrapperImpl}
import uk.ac.warwick.tabula.commands.{Description, Unaudited}
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.scheduling.MembershipInformation
import uk.ac.warwick.userlookup.User

class ImportOtherMemberCommand(member: MembershipInformation, ssoUser: User)
	extends ImportMemberCommand(member, ssoUser, None)
	with Logging with Daoisms with Unaudited {

	def applyInternal(): Member = transactional() {
		val memberExisting = memberDao.getByUniversityIdStaleOrFresh(universityId)
		if (memberExisting.collect { case m @ (_: StudentMember | _: StaffMember) => m }.nonEmpty) {
			// Don't override existing students or staff
			return memberExisting.get
		}

		logger.debug("Importing other member " + universityId + " into " + memberExisting)

		val (isTransient, member) = memberExisting match {
			case Some(m) => (false, m)
			case _ if this.userType == MemberUserType.Applicant => (true, new ApplicantMember(universityId))
			case _ => (true, new OtherMember(universityId))
		}

		val commandBean = new BeanWrapperImpl(this)
		val memberBean = new BeanWrapperImpl(member)

		val hasChanged = copyMemberProperties(commandBean, memberBean)

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + member)

			member.lastUpdatedDate = DateTime.now
			memberDao.saveOrUpdate(member)
		}

		member
	}

	override def describe(d: Description): Unit = d.property("universityId" -> universityId).property("category" -> this.userType)

	def phoneNumberPermissions = Nil

}