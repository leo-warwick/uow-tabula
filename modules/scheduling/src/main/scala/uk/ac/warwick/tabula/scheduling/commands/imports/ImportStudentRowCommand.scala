package uk.ac.warwick.tabula.scheduling.commands.imports

import java.sql.ResultSet
import org.joda.time.DateTime
import org.springframework.beans.{BeanWrapper, BeanWrapperImpl}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Description, Unaudited}
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.{Member, OtherMember, StudentMember, StudentProperties}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.helpers.{ImportRowTracker, PropertyCopying}
import uk.ac.warwick.tabula.scheduling.services.{MembershipInformation, ModeOfAttendanceImporter}
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.userlookup.User


/*
 * ImportStudentRowCommand takes a number of other commands as argument which perform sub-tasks.
 * These need to be passed in, rather than newed up within the command, to enable testing
 * without auto-wiring.
 */
class ImportStudentRowCommand(val member: MembershipInformation,
		val ssoUser: User,
		val resultSet: ResultSet,
		val importRowTracker: ImportRowTracker,
		var importStudentCourseCommand: ImportStudentCourseCommand,
		var importTier4ForStudentCommand: ImportTier4ForStudentCommand)
	extends ImportMemberCommand(member, ssoUser, Some(resultSet))
	with Logging with Daoisms
	with StudentProperties with Unaudited with PropertyCopying {

	import ImportMemberHelpers._

	implicit val rs = resultSet

	var modeOfAttendanceImporter = Wire[ModeOfAttendanceImporter]
	var profileService = Wire[ProfileService]

	this.nationality = rs.getString("nationality")
	this.mobileNumber = rs.getString("mobile_number")

	override def applyInternal(): Member = {
		transactional() {
			val memberExisting = memberDao.getByUniversityIdStaleOrFresh(universityId)

			logger.debug("Importing member " + universityId + " into " + memberExisting)

			val (isTransient, member) = memberExisting match {
				case Some(member: StudentMember) => (false, member)
				case Some(member: OtherMember) => {
					// TAB-692 delete the existing member, then return a brand new one
					memberDao.delete(member)
					(true, new StudentMember(universityId))
				}
				case Some(member) => throw new IllegalStateException("Tried to convert " + member + " into a student!")
				case _ => (true, new StudentMember(universityId))
			}

			if (!importRowTracker.universityIdsSeen.contains(member.universityId)) {
				garnerStudentDetails(isTransient, member)
			}

			importStudentCourseCommand.stuMem = member
			val studentCourseDetails = importStudentCourseCommand.apply()

			member.attachStudentCourseDetails(studentCourseDetails)

			importRowTracker.universityIdsSeen.add(member.universityId)

			member
		}
	}

	private def garnerStudentDetails(isTransient: Boolean, member: StudentMember) {
		// There are multiple rows returned by the SQL per student; only import non-course details if we haven't already
		val commandBean = new BeanWrapperImpl(this)
		val memberBean = new BeanWrapperImpl(member)

		importTier4ForStudentCommand.student = member

		// We intentionally use single pipes rather than double here - we want all statements to be evaluated
		val hasChanged = (copyMemberProperties(commandBean, memberBean)
			| copyStudentProperties(commandBean, memberBean)
			| importTier4ForStudentCommand.apply
			| markAsSeenInSits(memberBean))

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + member)

			member.lastUpdatedDate = DateTime.now
			memberDao.saveOrUpdate(member)
		}
	}

	private val basicStudentProperties = Set(
		"nationality", "mobileNumber"
	)

	private def copyStudentProperties(commandBean: BeanWrapper, memberBean: BeanWrapper) =
		copyBasicProperties(basicStudentProperties, commandBean, memberBean)

	override def describe(d: Description) = d.property("universityId" -> universityId).property("category" -> "student")
}
