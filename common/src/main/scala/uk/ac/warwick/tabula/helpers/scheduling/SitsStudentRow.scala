package uk.ac.warwick.tabula.helpers.scheduling

import java.sql.ResultSet

import org.joda.time.{Duration, LocalDate}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.commands.scheduling.imports.ImportMemberHelpers._
import uk.ac.warwick.tabula.data.model.{BasicStudentCourseProperties, BasicStudentCourseYearProperties, NamedLocation}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.util.Try

object SitsStudentRow {
  def apply(rs: ResultSet): SitsStudentRow = new SitsStudentRow(rs)

  /**
   * Order by (ascending)
   *
   * - SPR significance (least significant first)
   * - SPR code
   * - SCE sequence number
   */
  implicit val defaultOrdering: Ordering[SitsStudentRow] = Ordering.by { row =>
    (row.mostSignificant, row.sprCode, row.sceSequenceNumber)
  }
}

/**
  * Contains the data from the result set for the SITS query.
  */
class SitsStudentRow(resultSet: ResultSet)
  extends BasicStudentCourseProperties
    with BasicStudentCourseYearProperties
    with Logging
    with TaskBenchmarking {

  implicit val rs: Option[ResultSet] = Option(resultSet)

  val universityId: Option[String] = optString("university_id")
  val usercode: Option[String] = optString("user_code")
  val title: Option[String] = optString("title")
  val preferredForename: Option[String] = optString("preferred_forename")
  val fornames: Option[String] = optString("forenames")
  val familyName: Option[String] = optString("family_name")
  val gender: Option[String] = optString("gender")
  val emailAddress: Option[String] = optString("email_address")
  val dateOfBirth: Option[LocalDate] = optLocalDate("date_of_birth")
  val inUseFlag: Option[String] = optString("in_use_flag")
  val alternativeEmailAddress: Option[String] = optString("alternative_email_address")
  val disabilityCode: Option[String] = optString("disability")
  val disabilityFunding: Option[String] = optString("disabilityFunding")
  val deceased: Boolean = optString("mst_type") match {
    case Some("D") | Some("d") => true
    case _ => false
  }
  val nationality: Option[String] = optString("nationality")
  val secondNationality: Option[String] = optString("second_nationality")
  val tier4VisaRequirement: Option[Boolean] = optIntAsBoolean("tier4_visa_requirement")
  val mobileNumber: Option[String] = optString("mobile_number")

  // data from the result set which will be used by ImportStudentCourseCommand to create
  // StudentCourseDetails.  The data needs to be extracted in this command while the result set is accessible.

  var routeCode: String = resultSet.getString("route_code")
  var courseCode: String = resultSet.getString("course_code")
  var sprStatusCode: String = resultSet.getString("spr_status_code")
  var scjStatusCode: String = resultSet.getString("scj_status_code")
  var departmentCode: String = resultSet.getString("department_code")
  var awardCode: String = resultSet.getString("award_code")
  var sprStartAcademicYearString: String = resultSet.getString("spr_academic_year_start")

  // tutor data also needs some work before it can be persisted, so store it in local variables for now:
  //WMG uses a different table and column for their tutors
  var tutorUniId: String =
  if (departmentCode != null && departmentCode.toLowerCase == "wm") {
    Option(resultSet.getString("scj_tutor1")).map(_.substring(2)).orNull
  } else {
    resultSet.getString("spr_tutor1")
  }

  // this is the key and is not included in StudentCourseProperties, so just storing it in a var:
  var scjCode: String = resultSet.getString("scj_code")

  // now grab data from the result set into properties
  this.mostSignificant = resultSet.getString("most_signif_indicator") match {
    case "Y" | "y" => true
    case _ => false
  }

  this.sprCode = resultSet.getString("spr_code")
  this.beginDate = toLocalDate(resultSet.getDate("begin_date"))
  this.endDate = toLocalDate(resultSet.getDate("end_date"))
  this.expectedEndDate = toLocalDate(resultSet.getDate("expected_end_date"))
  this.courseYearLength = JInteger(Try(resultSet.getString("course_year_length").toInt).toOption)
  this.levelCode = resultSet.getString("level_code")
  this.reasonForTransferCode = resultSet.getString("scj_transfer_reason_code")
  this.specialExamArrangements = resultSet.getString("special_exam_arrangements") match {
    case "Y" | "y" => true
    case _ => false
  }
  this.specialExamArrangementsLocation = if (this.specialExamArrangements) {
    resultSet.getString("special_exam_arrangements_room_code") match {
      case null | "" => null
      case code => NamedLocation(resultSet.getString("special_exam_arrangements_room_name").maybeText.getOrElse(code))
    }
  } else null
  this.specialExamArrangementsExtraTime = if (this.specialExamArrangements) {
    Try(resultSet.getString("special_exam_arrangements_extra_time").toInt).toOption
      .filter(_ > 0)
      .map(Duration.standardMinutes(_))
      .orNull
  } else null
  this.specialExamArrangementsHourlyRestMinutes = if (this.specialExamArrangements) {
    Try(resultSet.getString("special_exam_arrangements_hourly_rest_minutes").toInt).toOption
      .filter(_ > 0)
      .map(Duration.standardMinutes(_))
      .orNull
  } else null

  // data from the result set which will be used by ImportStudentCourseYearCommand to create
  // StudentCourseYearDetails.  The data needs to be extracted in this command while the result set is accessible.

  var enrolmentDepartmentCode: String = resultSet.getString("enrolment_department_code")
  var enrolmentStatusCode: String = resultSet.getString("enrolment_status_code")
  var modeOfAttendanceCode: String = resultSet.getString("mode_of_attendance_code")
  var blockOccurrence: String = resultSet.getString("block_occurrence")
  var academicYearString: String = resultSet.getString("sce_academic_year")
  var moduleRegistrationStatusCode: String = resultSet.getString("mod_reg_status")

  var sceRouteCode: String = resultSet.getString("sce_route_code")

  this.yearOfStudy = resultSet.getInt("study_block")
  this.studyLevel = resultSet.getString("study_level")

  //this.fundingSource = rs.getString("funding_source")
  this.sceSequenceNumber = resultSet.getInt("sce_sequence_number")
  this.agreedMark = Try(resultSet.getBigDecimal("sce_agreed_mark")).toOption match {
    case Some(value) =>
      Option(value).map(_.setScale(1, java.math.RoundingMode.HALF_UP)).orNull
    case _ =>
      logger.error(s"Tried to import ${resultSet.getString("sce_agreed_mark")} as sce_agreed_mark for ${resultSet.getString("scj_code")}-$sceSequenceNumber but there was an error")
      null
  }
}
