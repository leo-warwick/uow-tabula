package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.beans.{BeanWrapper, BeanWrapperImpl}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.scheduling.{ImportCommandFactory, PropertyCopying, SitsStudentRow}
import uk.ac.warwick.tabula.services.scheduling._
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.userlookup.User

object ImportStudentRowCommand {
	def apply(
		member: MembershipInformation,
		ssoUser: User,
		rows: Seq[SitsStudentRow],
		importCommandFactory: ImportCommandFactory
	): ImportStudentRowCommandInternal with Command[Member] with AutowiringProfileServiceComponent with AutowiringTier4RequirementImporterComponent with AutowiringModeOfAttendanceImporterComponent with Unaudited = {
		new ImportStudentRowCommandInternal(member, ssoUser, rows, importCommandFactory)
			with Command[Member]
			with AutowiringProfileServiceComponent
			with AutowiringTier4RequirementImporterComponent
			with AutowiringModeOfAttendanceImporterComponent
			with Unaudited
	}
}

/*
 * ImportStudentRowCommand takes an importCommandFactory which enables sub-commands to be created
  * while enabling testing without auto-wiring.
 */
class ImportStudentRowCommandInternal(
	val member: MembershipInformation,
	val ssoUser: User,
	val rows: Seq[SitsStudentRow],
	var importCommandFactory: ImportCommandFactory
) extends ImportMemberCommand(member, ssoUser, None, rows.headOption)
	with Describable[Member]
	with ImportStudentRowCommandState
	with PropertyCopying
	with Logging {

	self: ProfileServiceComponent with Tier4RequirementImporterComponent with ModeOfAttendanceImporterComponent =>

	val studentRow: Option[SitsStudentRow] = rows.headOption

	studentRow.foreach { row =>
		// these properties are from membership but may be overwritten by the SITS data (working theory)
		this.nationality = row.nationality.orNull
		this.mobileNumber = row.mobileNumber.orNull

		this.deceased = row.deceased
	}

	override def applyInternal(): Member = {
		transactional() {
			// set appropriate disability object iff a non-null code is retrieved - I <3 scala options
			studentRow.flatMap(_.disabilityCode).foreach(code => profileService.getDisability(code).foreach(this.disability = _))

			val memberExisting = memberDao.getByUniversityIdStaleOrFresh(universityId)

			logger.debug("Importing student member " + universityId + " into " + memberExisting)

			val (isTransient, member) = memberExisting.map(HibernateHelpers.initialiseAndUnproxy) match {
				case Some(m: StudentMember) => (false, m)
				case Some(m: OtherMember) =>
					// TAB-692 delete the existing member, then return a brand new one
					// TAB-2188
					logger.info(s"Deleting $m while importing $universityId")
					memberDao.delete(m)
					(true, new StudentMember(universityId))
				case Some(m: ApplicantMember) =>
					// TAB-692 delete the existing member, then return a brand new one
					// TAB-2188
					logger.info(s"Deleting $m while importing $universityId")
					memberDao.delete(m)
					(true, new StudentMember(universityId))
				case Some(m) => throw new IllegalStateException("Tried to convert " + m + " into a student!")
				case _ => (true, new StudentMember(universityId))
			}

			saveStudentDetails(isTransient, member)

			rows.groupBy(_.scjCode).foreach { case (_, rowsForCourse) =>
				val studentCourseDetails = importCommandFactory.createImportStudentCourseCommand(rowsForCourse, member).apply()
				member.attachStudentCourseDetails(studentCourseDetails)
			}


			member
		}
	}

	private def saveStudentDetails(isTransient: Boolean, member: StudentMember) {
		// There are multiple rows returned by the SQL per student; only import non-course details if we haven't already
		val commandBean = new BeanWrapperImpl(this)
		val memberBean = new BeanWrapperImpl(member)

		val tier4VisaRequirement = tier4RequirementImporter.hasTier4Requirement(universityId)

		// We intentionally use single pipes rather than double here - we want all statements to be evaluated
		val hasChanged = (copyMemberProperties(commandBean, memberBean)
			| copyStudentProperties(commandBean, memberBean)
			| (studentRow.isDefined && markAsSeenInSits(memberBean))
			|| (member.tier4VisaRequirement.booleanValue() != tier4VisaRequirement))

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + member)
			member.tier4VisaRequirement = tier4VisaRequirement
			member.lastUpdatedDate = DateTime.now
			memberDao.saveOrUpdate(member)
		}
	}

	private val basicStudentProperties = Set(
		"nationality", "mobileNumber", "disability", "deceased"
	)

	private def copyStudentProperties(commandBean: BeanWrapper, memberBean: BeanWrapper) =
		copyBasicProperties(basicStudentProperties, commandBean, memberBean)

	override def describe(d: Description): Unit = d.property("universityId" -> universityId).property("category" -> "student")
}

trait ImportStudentRowCommandState extends StudentProperties
