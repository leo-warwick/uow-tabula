package uk.ac.warwick.tabula.scheduling.commands.imports

import java.sql.{Date, ResultSet}
import java.util.UUID

import org.apache.commons.lang3.text.WordUtils
import org.joda.time.{DateTime, LocalDate}
import org.springframework.beans.BeanWrapper
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Command, Unaudited}
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.model.{Gender, Member, MemberProperties}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.scheduling.helpers.PropertyCopying
import uk.ac.warwick.tabula.scheduling.services.{MembershipInformation, MembershipMember}
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.userlookup.User

import scala.language.implicitConversions
import scala.util.matching.Regex

abstract class ImportMemberCommand extends Command[Member] with Logging
with MemberProperties with Unaudited with PropertyCopying {
	import ImportMemberHelpers._

	PermissionCheck(Permissions.ImportSystemData)

	var memberDao = Wire[MemberDao]
	var userLookup = Wire[UserLookupService]

	// A couple of intermediate properties that will be transformed later
	var homeDepartmentCode: String = _

	var membershipLastUpdated: DateTime = _

	def this(mac: MembershipInformation, ssoUser: User, rs: Option[ResultSet]) {
		this()

		implicit val resultSet = rs

		val member = mac.member
		this.membershipLastUpdated = member.modified

		this.universityId = oneOf(Option(member.universityId), optString("university_id")).get

		// TAB-2014
		this.userId = oneOf(Option(member.usercode), ssoUser.getUserId.maybeText, optString("user_code")).get

		this.userType = member.userType

		this.title = oneOf(Option(member.title), optString("title")) map { WordUtils.capitalizeFully(_).trim() } getOrElse ""

		def regexExceptionHandled(fieldNameToDisplay: String, fallbackField: String)(f: => String): String = {
			try {	f	} catch {
				case iae: IllegalArgumentException =>
					// Regex match error
					logger.error(s"Failed to match $fieldNameToDisplay for ${ssoUser.getUserId}")
					fallbackField
			}
		}
		this.firstName = oneOf(
			optString("preferred_forename"),
			Option(member.preferredForenames),
			Option(ssoUser.getFirstName)
		).map(s => regexExceptionHandled("firstName", ssoUser.getFirstName){ formatForename(s, ssoUser.getFirstName) }).getOrElse("")
		this.fullFirstName = oneOf(optString("forenames"), Option(ssoUser.getFirstName))
			.map(s => regexExceptionHandled("firstName", ssoUser.getFirstName){ formatForename(s, ssoUser.getFirstName) }).getOrElse("")
		this.lastName = oneOf(optString("family_name"), Option(member.preferredSurname), Option(ssoUser.getLastName))
			.map(s => regexExceptionHandled("lastName", ssoUser.getLastName){ formatSurname(s, ssoUser.getLastName) }).getOrElse("")

		this.email = oneOf(Option(member.email), optString("email_address"), Option(ssoUser.getEmail)).orNull
		this.homeEmail = oneOf(Option(member.alternativeEmailAddress), optString("alternative_email_address")).orNull

		this.gender = oneOf(Option(member.gender), optString("gender") map Gender.fromCode).orNull

		this.jobTitle = member.position
		this.phoneNumber = member.phoneNumber

		this.inUseFlag = getInUseFlag(rs.map { _.getString("in_use_flag") }, member)
		this.groupName = member.targetGroup
		this.inactivationDate = member.endDate

		this.homeDepartmentCode = oneOf(Option(member.departmentCode), optString("home_department_code"), Option(ssoUser.getDepartmentCode)).orNull
		this.dateOfBirth = oneOf(Option(member.dateOfBirth), optLocalDate("date_of_birth")).orNull
	}

	private val basicMemberProperties = Set(
		// userType is included for new records, but hibernate does not in fact update it for existing records
		"userId", "firstName", "lastName", "email", "homeEmail", "title", "fullFirstName", "userType", "gender",
		"inUseFlag", "inactivationDate", "groupName", "dateOfBirth", "jobTitle", "phoneNumber"
	)

	private def setTimetableHashIfMissing(memberBean: BeanWrapper): Boolean = {
		val existingHash = memberBean.getPropertyValue("timetableHash").asInstanceOf[String]
		if (!existingHash.hasText) {
			memberBean.setPropertyValue("timetableHash", UUID.randomUUID.toString)
			true
		} else {
			false
		}
	}

	// We intentionally use a single pipe rather than a double pipe here - we want all statements to be evaluated
	protected def copyMemberProperties(commandBean: BeanWrapper, memberBean: BeanWrapper) =
		copyBasicProperties(basicMemberProperties, commandBean, memberBean) |
		copyObjectProperty("homeDepartment", homeDepartmentCode, memberBean, toDepartment(homeDepartmentCode)) |
		setTimetableHashIfMissing(memberBean)

}

object ImportMemberHelpers {

	implicit def opt[A](value: A): Option[A] = Option(value)

	/** Return the first Option that has a value, else None. */
	def oneOf[A](options: Option[A]*) = options.flatten.headOption

	def optString(columnName: String)(implicit rs: Option[ResultSet]): Option[String] =
		rs.flatMap { rs =>
			if (hasColumn(rs, columnName)) Option(rs.getString(columnName))
			else None
		}

	def optLocalDate(columnName: String)(implicit rs: Option[ResultSet]): Option[LocalDate] =
		rs.flatMap { rs =>
			if (hasColumn(rs, columnName)) Option(rs.getDate(columnName)).map { new LocalDate(_) }
			else None
		}

	def hasColumn(rs: ResultSet, columnName: String) = {
		val metadata = rs.getMetaData
		val cols = for (col <- 1 to metadata.getColumnCount)
			yield columnName.toLowerCase == metadata.getColumnName(col).toLowerCase
		cols.exists(b => b)
	}

	def toLocalDate(date: Date) = {
		if (date == null) {
			null
		} else {
			new LocalDate(date)
		}
	}

	def toAcademicYear(code: String) = {
		if (code == null || code == "") {
			null
		} else {
			AcademicYear.parse(code)
		}
	}

	def getInUseFlag(flag: Option[String], member: MembershipMember) =
		flag.getOrElse {
			val (startDate, endDate) = (member.startDate, member.endDate)
			if (startDate != null && startDate.toDateTimeAtStartOfDay.isAfter(DateTime.now))
				"Inactive - Starts " + startDate.toString("dd/MM/yyyy")
			else if (endDate != null && endDate.toDateTimeAtStartOfDay.isBefore(DateTime.now))
				"Inactive - Ended " + endDate.toString("dd/MM/yyyy")
			else "Active"
		}

	private val CapitaliseForenamePattern = """(?:(\p{Lu})(\p{L}*)([^\p{L}]?))""".r

	def formatForename(name: String, suggested: String = null): String = {
		if (name == null || name.equalsIgnoreCase(suggested)) {
			// Our suggested capitalisation from SSO was correct
			suggested
		} else {
			CapitaliseForenamePattern.replaceAllIn(name, { m: Regex.Match =>
				m.group(1).toUpperCase + m.group(2).toLowerCase + m.group(3)
			}).trim()
		}
	}

	private val CapitaliseSurnamePattern = """(?:((\p{Lu})(\p{L}*))([^\p{L}]?))""".r
	private val WholeWordGroup = 1
	private val FirstLetterGroup = 2
	private val RemainingLettersGroup = 3
	private val SeparatorGroup = 4

	def formatSurname(name: String, suggested: String = null): String = {
		if (name.equalsIgnoreCase(suggested)) {
			// Our suggested capitalisation from SSO was correct
			suggested
		} else {
			/*
			 * Conventions:
			 *
			 * von - do not capitalise de La - capitalise second particle O', Mc,
			 * Mac, M' - always capitalise
			 */

			CapitaliseSurnamePattern.replaceAllIn(name, { m: Regex.Match =>
				val wholeWord = m.group(WholeWordGroup).toUpperCase
				val first = m.group(FirstLetterGroup).toUpperCase
				val remainder = m.group(RemainingLettersGroup).toLowerCase
				val separator = m.group(SeparatorGroup)

				if (wholeWord.startsWith("MC") && wholeWord.length() > 2) {
					// Capitalise the first letter of the remainder
					first +
						remainder.substring(0, 1) +
						remainder.substring(1, 2).toUpperCase +
						remainder.substring(2) +
						separator
				} else if (wholeWord.startsWith("MAC") && wholeWord.length() > 3) {
					// Capitalise the first letter of the remainder
					first +
						remainder.substring(0, 2) +
						remainder.substring(2, 3).toUpperCase +
						remainder.substring(3) +
						separator
				} else if (wholeWord.equals("VON") || wholeWord.equals("D") || wholeWord.equals("DE") || wholeWord.equals("DI")) {
					// Special case - lowercase the first word
					first.toLowerCase + remainder + separator
				} else {
					first + remainder + separator
				}
			}).trim()
		}
	}
}
