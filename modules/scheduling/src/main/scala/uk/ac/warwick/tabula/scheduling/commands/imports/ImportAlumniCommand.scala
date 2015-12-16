package uk.ac.warwick.tabula.scheduling.commands.imports

import java.sql.ResultSet

import org.joda.time.DateTime
import org.springframework.beans.{BeanWrapper, BeanWrapperImpl}
import uk.ac.warwick.tabula.commands.{Description, Unaudited}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{AlumniProperties, Member, OtherMember}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.scheduling.services.MembershipInformation
import uk.ac.warwick.userlookup.User

class ImportAlumniCommand(member: MembershipInformation, ssoUser: User, rs: ResultSet)
	extends ImportMemberCommand(member, ssoUser, Some(rs))
	with Logging with AlumniProperties with Unaudited {

	// any initialisation code specific to alumni (e.g. setting alumni properties) can go here

	def applyInternal(): Member = transactional() {
		val memberExisting = memberDao.getByUniversityIdStaleOrFresh(universityId)

		logger.debug("Importing alumni member " + universityId + " into " + memberExisting)

		val isTransient = !memberExisting.isDefined
		val member = memberExisting getOrElse(new OtherMember(universityId))

		val commandBean = new BeanWrapperImpl(this)
		val memberBean = new BeanWrapperImpl(member)

		// We intentionally use a single pipe rather than a double pipe here - we want both statements to be evaluated
		val hasChanged = copyMemberProperties(commandBean, memberBean) | copyAlumniProperties(commandBean, memberBean)

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + member)

			member.lastUpdatedDate = DateTime.now
			memberDao.saveOrUpdate(member)
		}

		member
	}

	private val basicAlumniProperties: Set[String] = Set()

	private def copyAlumniProperties(commandBean: BeanWrapper, memberBean: BeanWrapper) =
		copyBasicProperties(basicAlumniProperties, commandBean, memberBean)


	override def describe(d: Description) = d.property("universityId" -> universityId).property("category" -> "alumni")

	override def phoneNumberPermissions: Seq[Permission] = Nil
}