package uk.ac.warwick.tabula.commands.scheduling.imports

import java.sql.{Date, ResultSet}
import java.util.UUID

import org.apache.commons.lang3.text.WordUtils
import org.joda.time.{DateTime, LocalDate}
import org.springframework.beans.BeanWrapper
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Command, Unaudited}
import uk.ac.warwick.tabula.data.model.{Gender, Member, MemberProperties}
import uk.ac.warwick.tabula.data.{Daoisms, MemberDao}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.scheduling.{PropertyCopying, SitsStudentRow}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.scheduling.{MembershipInformation, MembershipMember}
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, UserLookupService}
import uk.ac.warwick.userlookup.User

import scala.language.implicitConversions
import scala.util.matching.Regex

abstract class ImportMemberCommand extends Command[Member] with Logging with Daoisms
  with MemberProperties with Unaudited with PropertyCopying {

  import ImportMemberHelpers._

  PermissionCheck(Permissions.ImportSystemData)

  var memberDao: MemberDao = Wire[MemberDao]
  var userLookup: UserLookupService = Wire[UserLookupService]
  var moduleAndDepartmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]

  // A couple of intermediate properties that will be transformed later
  var homeDepartmentCode: String = _

  var membershipLastUpdated: DateTime = _

  def this(mac: MembershipInformation, ssoUser: User, rs: Option[ResultSet], ssr: Option[SitsStudentRow] = None) = {
    this()

    implicit val resultSet = rs

    val member = mac.member
    this.membershipLastUpdated = member.modified

    this.universityId = oneOf(Option(member.universityId), ssr.flatMap(_.universityId), optString("university_id")).get

    // TAB-2014
    this.userId = oneOf(Option(member.usercode), ssoUser.getUserId.maybeText, ssr.flatMap(_.usercode), optString("user_code")).getOrElse(this.universityId)

    this.userType = member.userType

    this.title = oneOf(Option(member.title), ssr.flatMap(_.title), optString("title")) map {
      WordUtils.capitalizeFully(_).trim()
    } getOrElse ""

    def regexExceptionHandled(fieldNameToDisplay: String, fallbackField: String)(f: => String): String = {
      try {
        f
      } catch {
        case iae: IllegalArgumentException =>
          // Regex match error
          logger.error(s"Failed to match $fieldNameToDisplay for ${ssoUser.getUserId}")
          fallbackField
      }
    }

    this.firstName = oneOf(
      ssr.flatMap(_.preferredForename),
      optString("preferred_forename"),
      Option(member.preferredForenames),
      Option(ssoUser.getFirstName)
    ).map(s => regexExceptionHandled("firstName", ssoUser.getFirstName) {
      formatForename(s, ssoUser.getFirstName)
    }).getOrElse("")
    this.fullFirstName = oneOf(ssr.flatMap(_.fornames), optString("forenames"), Option(ssoUser.getFirstName))
      .map(s => regexExceptionHandled("firstName", ssoUser.getFirstName) {
        formatForename(s, ssoUser.getFirstName)
      }).getOrElse("")
    this.lastName = oneOf(ssr.flatMap(_.familyName), optString("family_name"), Option(member.preferredSurname), Option(ssoUser.getLastName))
      .map(s => regexExceptionHandled("lastName", ssoUser.getLastName) {
        formatSurname(s, ssoUser.getLastName)
      }).getOrElse("")

    this.email = oneOf(Option(member.email), ssr.flatMap(_.emailAddress), optString("email_address"), Option(ssoUser.getEmail)).orNull
    this.homeEmail = oneOf(Option(member.alternativeEmailAddress), ssr.flatMap(_.alternativeEmailAddress), optString("alternative_email_address")).orNull

    this.gender = oneOf(Option(member.gender), ssr.flatMap(_.gender).map(Gender.fromCode), optString("gender").map(Gender.fromCode)).orNull

    this.jobTitle = member.position
    this.phoneNumber = member.phoneNumber

    this.inUseFlag = getInUseFlag(oneOf(ssr.flatMap(_.inUseFlag), rs.map(_.getString("in_use_flag"))), member)
    this.groupName = member.targetGroup
    this.inactivationDate = member.endDate

    this.homeDepartmentCode = oneOf(Option(member.departmentCode), Option(ssoUser.getDepartmentCode)).orNull
    this.dateOfBirth = oneOf(Option(member.dateOfBirth), ssr.flatMap(_.dateOfBirth), optLocalDate("date_of_birth")).orNull
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

  private lazy val department = homeDepartmentCode.maybeText.flatMap(moduleAndDepartmentService.getDepartmentByCode)

  // We intentionally use a single pipe rather than a double pipe here - we want all statements to be evaluated
  protected def copyMemberProperties(commandBean: BeanWrapper, memberBean: BeanWrapper): Boolean =
    copyBasicProperties(basicMemberProperties, commandBean, memberBean) |
      copyObjectProperty("homeDepartment", homeDepartmentCode.safeLowercase, memberBean, department) |
      setTimetableHashIfMissing(memberBean)

}

object ImportMemberHelpers {

  implicit def opt[A](value: A): Option[A] = Option(value)

  /** Return the first Option that has a value, else None. */
  def oneOf[A](options: Option[A]*): Option[A] = options.flatten.headOption

  def optString(columnName: String)(implicit rs: Option[ResultSet]): Option[String] =
    rs.flatMap { rs =>
      if (hasColumn(rs, columnName)) Option(rs.getString(columnName))
      else None
    }

  def optLocalDate(columnName: String)(implicit rs: Option[ResultSet]): Option[LocalDate] =
    rs.flatMap { rs =>
      if (hasColumn(rs, columnName)) Option(rs.getDate(columnName)).map {
        new LocalDate(_)
      }
      else None
    }

  def optIntAsBoolean(columnName: String)(implicit rs: Option[ResultSet]): Option[Boolean] =
    rs.flatMap { rs =>
      if (hasColumn(rs, columnName)) Option(rs.getInt(columnName)).exists(_ != 0)
      else None
    }

  def getInteger(resultSet: ResultSet, column: String): Option[Int] = {
    val intValue = resultSet.getInt(column)
    if (resultSet.wasNull()) None else Some(intValue)
  }

  def hasColumn(rs: ResultSet, columnName: String): Boolean = {
    val metadata = rs.getMetaData
    val cols = for (col <- 1 to metadata.getColumnCount)
      yield columnName.toLowerCase == metadata.getColumnName(col).toLowerCase
    cols.exists(b => b)
  }

  def toLocalDate(date: Date): LocalDate = {
    if (date == null) {
      null
    } else {
      new LocalDate(date)
    }
  }

  def toAcademicYear(code: String): AcademicYear = {
    if (code == null || code == "") {
      null
    } else {
      AcademicYear.parse(code)
    }
  }

  def getInUseFlag(flag: Option[String], member: MembershipMember): String =
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
