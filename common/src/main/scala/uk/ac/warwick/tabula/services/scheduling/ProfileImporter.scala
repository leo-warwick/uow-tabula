package uk.ac.warwick.tabula.services.scheduling

import java.sql.{ResultSet, Types}

import javax.sql.DataSource
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter, ISODateTimeFormat}
import org.joda.time.{DateTime, LocalDate}
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.scheduling.imports._
import uk.ac.warwick.tabula.commands.{Command, TaskBenchmarking, Unaudited}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.CourseType.{PGR, PGT}
import uk.ac.warwick.tabula.data.model.MemberUserType._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringMemberDaoComponent, Daoisms}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.scheduling.{ImportCommandFactory, SitsStudentRow}
import uk.ac.warwick.tabula.sandbox.{MapResultSet, SandboxData}
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.termdates.AcademicYearPeriod

import scala.collection.immutable.IndexedSeq
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters._
import scala.util.Try

object MembershipInformation {
  def apply(member: Member): MembershipInformation = MembershipInformation(MembershipMember(member), None)
}
case class MembershipInformation(member: MembershipMember, sitsApplicantInfo: Option[SitsApplicantInfo] = None)

trait ProfileImporter {

  import ProfileImporter._

  def getMemberDetails(
    memberInfo: Seq[MembershipInformation],
    users: Map[UniversityId, User],
    importCommandFactory: ImportCommandFactory
  ): Seq[ImportMemberCommand]

  def membershipInfoByDepartment(department: Department): Seq[MembershipInformation]

  def membershipInfoForIndividual(universityId: String): Option[MembershipInformation]

  def sitsStudentRows(universityIds: Seq[String]): Seq[SitsStudentRow]

  def getUniversityIdsPresentInMembership(universityIds: Set[String]): Set[String]

  def applicantExistsInSits(universityId: String): Boolean

  def applicantsExistingInSits(universityIds: Set[String]): Set[String]
}

@Profile(Array("dev", "test", "production"))
@Service
class ProfileImporterImpl extends ProfileImporter with Logging with SitsAcademicYearAware with AutowiringMemberDaoComponent with AutowiringReasonableAdjustmentsImporterComponent with TaskBenchmarking {

  import ProfileImporter._

  var sits: DataSource = Wire[DataSource]("sitsDataSource")

  var fim: DataSource = Wire[DataSource]("fimDataSource")

  private val SQLServerMaxParameterCount: Int = 2000

  lazy val membershipByDepartmentQuery = new MembershipByDepartmentQuery(fim)
  lazy val membershipByUniversityIdQuery = new MembershipByUniversityIdQuery(fim)

  lazy val applicantQuery = new ApplicantQuery(sits)

  lazy val applicantByUniversityIdQuery = new ApplicantByUniversityIdQuery(sits)

  lazy val studentInformationQuery: StudentInformationQuery = new StudentInformationQuery(sits)

  override def sitsStudentRows(universityIds: Seq[String]): Seq[SitsStudentRow] =
    // Due to the way our query works, it's faster to do this as multiple queries rather than individually
    universityIds.sorted.flatMap { universityId =>
      studentInformationQuery.executeByNamedParam(Map("universityId" -> universityId).asJava).asScala
    }

  lazy val membershipUniversityIdPresenceQuery: MembershipUniversityIdPresenceQuery = new MembershipUniversityIdPresenceQuery(fim)

  def getUniversityIdsPresentInMembership(universityIds: Set[String]): Set[String] = {
    universityIds.toSeq.grouped(SQLServerMaxParameterCount).flatMap(ids =>
      membershipUniversityIdPresenceQuery.executeByNamedParam(Map("universityIds" -> ids.asJava).asJava).asScala.toSet
    ).toSet
  }

  def getMemberDetails(memberInfo: Seq[MembershipInformation], users: Map[UniversityId, User], importCommandFactory: ImportCommandFactory): Seq[ImportMemberCommand] = {
    memberInfo.groupBy(_.member.userType).flatMap { case (userType, members) => benchmarkTask(s"Get member info for $userType") {
      userType match {
        case Staff | Emeritus => members.map { info =>
          val ssoUser = users(info.member.universityId)
          new ImportStaffMemberCommand(info, ssoUser)
        }

        case Student =>
          val universityIds = members.map(_.member.universityId).distinct

          val allStudentSitsRows = benchmarkTask(s"Get student rows for ${members.map(_.member.universityId).size} university IDs") {
            sitsStudentRows(universityIds)
              .groupBy(_.universityId.get)
          }

          val allStudentsReasonableAdjustments = benchmarkTask(s"Get reasonable adjustments for ${members.map(_.member.universityId).size} university IDs") {
            Await.result(reasonableAdjustmentsImporter.getReasonableAdjustments(universityIds), Duration.Inf)
          }

          members
            .filter { info => allStudentSitsRows.contains(info.member.universityId) }
            .map { info =>
              val universityId = info.member.universityId
              val ssoUser = users(universityId)

              val sitsRows = allStudentSitsRows.getOrElse(universityId, Seq.empty)

              ImportStudentRowCommand(
                info,
                ssoUser,
                sitsRows,
                allStudentsReasonableAdjustments.get(universityId).flatten,
                importCommandFactory
              )
            }

        case Applicant | Other => members.map { info =>
          val ssoUser = users(info.member.universityId)
          new ImportOtherMemberCommand(info, ssoUser)
        }

        case _ => Seq.empty
      }
    }}.toSeq
  }

  def membershipInfoByDepartment(department: Department): Seq[MembershipInformation] = {
    val fimMembers: Seq[MembershipInformation] =
      if (department.code == applicantDepartmentCode) {
        // Magic student recruitment department - get membership information directly from SITS for applicants
        val members = applicantQuery.execute().asScala.toSeq
        val universityIds = members.map { case (membershipInfo, _) => membershipInfo.universityId }.toSet

        // Filter out people in UOW_CURRENT_MEMBERS to avoid double import
        val universityIdsInMembership = getUniversityIdsPresentInMembership(universityIds)

        members
          .filterNot { case (m, _) => universityIdsInMembership.contains(m.universityId) }
          .map { case (m, a) => MembershipInformation(m, Some(a)) }
      } else {
        membershipByDepartmentQuery.executeByNamedParam(Map("departmentCode" -> department.code.toUpperCase).asJava).asScala.toSeq.map { member =>
          MembershipInformation(member)
        }
      }

    val fimUniversityIds = fimMembers.map(_.member.universityId)
    val fimUserIds = fimMembers.map(_.member.usercode)
    val tabulaActiveMembers = transactional(readOnly = true) {
      memberDao.getActiveMembersByDepartment(department)
        .filterNot(m => fimUniversityIds.contains(m.universityId))
        .filterNot(m => fimUserIds.contains(m.userId))
        .map(MembershipInformation.apply)
    }

    fimMembers ++ tabulaActiveMembers
  }

  def head(result: Any): Option[MembershipInformation] = result match {
    case result: List[MembershipMember@unchecked] => result.headOption.map(m => MembershipInformation(m))
    case _ => None
  }

  def membershipInfoForIndividual(universityId: String): Option[MembershipInformation] = {
    Option(membershipByUniversityIdQuery.executeByNamedParam(Map("universityIds" -> universityId).asJava).asScala.toList).flatMap(head)
  }

  def applicantExistsInSits(universityId: String): Boolean =
    Option(applicantByUniversityIdQuery.executeByNamedParam(
      Map("universityIds" -> universityId).asJava).asScala.toList.map{ case (membershipMember, _) => membershipMember}
    ).flatMap(head).nonEmpty

  def applicantsExistingInSits(universityIds: Set[String]): Set[String] =
    universityIds.grouped(Daoisms.MaxInClauseCountOracle).flatMap { ids =>
      Option(applicantByUniversityIdQuery.executeByNamedParam(Map("universityIds" -> ids.asJava).asJava)
        .asScala
        .map { case (m, _) => m.universityId }
      ).getOrElse(Seq.empty)
    }.toSet

}

@Profile(Array("sandbox"))
@Service
class SandboxProfileImporter extends ProfileImporter with AutowiringProfileServiceComponent with AutowiringReasonableAdjustmentsImporterComponent {
  def getMemberDetails(memberInfo: Seq[MembershipInformation], users: Map[String, User], importCommandFactory: ImportCommandFactory): Seq[ImportMemberCommand] =
    memberInfo map { info =>
      info.member.userType match {
        case Student => studentMemberDetails(importCommandFactory)(info)
        case _ => staffMemberDetails(info)
      }
    }

  private def sitsStudentRows(universityId: String): Seq[SitsStudentRow] =
    profileService.getMemberByUniversityIdStaleOrFresh(universityId).toSeq.flatMap { member =>
      membershipInfoByDepartment(member.homeDepartment).find(_.member.universityId == universityId).toSeq.flatMap { mac =>
        sitsStudentRows(mac)
      }
    }

  private def sitsStudentRows(mac: MembershipInformation): Seq[SitsStudentRow] = {
    val member = mac.member

    val route = SandboxData.route(member.universityId.toLong)
    val yearOfStudy = ((member.universityId.toLong % 3) + 1).toInt

    (1 to yearOfStudy).map(thisYearOfStudy => {
      SitsStudentRow(new MapResultSet(Map(
        "university_id" -> member.universityId,
        "title" -> member.title,
        "preferred_forename" -> member.preferredForenames,
        "forenames" -> member.preferredForenames,
        "family_name" -> member.preferredSurname,
        "gender" -> member.gender.dbValue,
        "email_address" -> member.email,
        "user_code" -> member.usercode,
        "date_of_birth" -> member.dateOfBirth.toDateTimeAtStartOfDay(),
        "in_use_flag" -> "Active",
        "alternative_email_address" -> "%s%s@hotmail.com".format(member.preferredSurname.toLowerCase, member.preferredForenames.toLowerCase),
        "mobile_number" -> (if (member.universityId.toLong % 3 == 0) null else s"0700${member.universityId}"),
        "nationality" -> (if (member.universityId.toLong % 3 == 0) "Kittitian/Nevisian" else "British (ex. Channel Islands & Isle of Man)"),
        "second_nationality" -> (if (member.universityId.toLong % 3 == 0) "Syrian" else null),
        "tier4_visa_requirement" -> (if (member.universityId.toLong % 3 == 0) 1 else 0),
        "course_code" -> "%c%s-%s".format(route.courseType.courseCodeChars.head, member.departmentCode.toUpperCase, route.code.toUpperCase),
        "course_year_length" -> (if(route.courseType == PGT) "1" else "3"),
        "spr_code" -> "%s/1".format(member.universityId),
        "route_code" -> route.code.toUpperCase,
        "department_code" -> member.departmentCode.toUpperCase,
        "award_code" -> route.awardCode,
        "spr_status_code" -> "C",
        "scj_status_code" -> "C",
        "level_code" -> (if(route.courseType == PGT) "M1" else if(route.courseType == PGR) "M2" else  thisYearOfStudy.toString),
        "spr_tutor1" -> null,
        "spr_academic_year_start" -> (AcademicYear.now() - yearOfStudy + 1).toString,
        "scj_tutor1" -> null,
        "scj_transfer_reason_code" -> null,
        "scj_code" -> "%s/1".format(member.universityId),
        "begin_date" -> member.startDate.toDateTimeAtStartOfDay(),
        "end_date" -> member.endDate.toDateTimeAtStartOfDay(),
        "expected_end_date" -> member.endDate.toDateTimeAtStartOfDay(),
        "most_signif_indicator" -> "Y",
        "funding_source" -> null,
        "enrolment_status_code" -> "C",
        "study_block" -> thisYearOfStudy,
        "study_level" -> (if(route.courseType == PGT) "M1" else if(route.courseType == PGR) "M2" else  thisYearOfStudy.toString),
        "mode_of_attendance_code" -> (if (member.universityId.toLong % 5 == 0) "P" else "F"),
        "block_occurrence" -> (if (member.universityId.toLong % 5 == 0) "I" else "C"),
        "sce_academic_year" -> (AcademicYear.now() - (yearOfStudy - thisYearOfStudy)).toString,
        "sce_sequence_number" -> thisYearOfStudy,
        "sce_route_code" -> route.code.toUpperCase,
        "enrolment_department_code" -> member.departmentCode.toUpperCase,
        "mod_reg_status" -> "CON",
        "disability" -> (member.universityId.toLong % 100 match {
          case 1 => "G" // Learning difficulty
          case 4 => "F" // Mental health condition
          case 5 => "I" // Unclassified disability
          case n if n > 70 => "99" // Not known
          case _ => "A" // No disability
        }),
        "disabilityFunding" -> (member.universityId.toLong % 100 match {
          case 4 => "4" // In receipt
          case 5 => "5" // Not in receipt
          case 9 => "9" // Unknown
          case _ => null
        }),
        "special_exam_arrangements" -> (member.universityId.toLong % 100 match {
          case 1 | 4 | 5 => "Y"
          case n if n > 70 => "N"
          case _ => null
        }),
        "special_exam_arrangements_room_code" -> (member.universityId.toLong % 100 match {
          case 1 | 4 | 5 => "INDEPT"
          case _ => null
        }),
        "special_exam_arrangements_room_name" -> (member.universityId.toLong % 100 match {
          case 1 | 4 | 5 => "In Department - Reasonable Adjustment Room"
          case _ => null
        }),
        "special_exam_arrangements_extra_time" -> (member.universityId.toLong % 100 match {
          case 1 => "15"
          case 4 => "20"
          case 5 => "30"
          case _ => null
        }),
        "mst_type" -> "L",
        "sce_agreed_mark" -> new JBigDecimal((member.universityId ++ member.universityId).toCharArray.map(char =>
          char.toString.toInt * member.universityId.toCharArray.apply(0).toString.toInt * thisYearOfStudy
        ).sum % 100)
      )))
    })
  }

  def studentMemberDetails(importCommandFactory: ImportCommandFactory)(mac: MembershipInformation): ImportStudentRowCommandInternal with Command[Member] with AutowiringProfileServiceComponent with Unaudited = {
    val member = mac.member
    val ssoUser = new User(member.usercode)
    ssoUser.setFoundUser(true)
    ssoUser.setVerified(true)
    ssoUser.setDepartment(SandboxData.Departments(member.departmentCode).name)
    ssoUser.setDepartmentCode(member.departmentCode)
    ssoUser.setEmail(member.email)
    ssoUser.setFirstName(member.preferredForenames)
    ssoUser.setLastName(member.preferredSurname)
    ssoUser.setStudent(true)
    ssoUser.setWarwickId(member.universityId)

    val rows = sitsStudentRows(mac)

    ImportStudentRowCommand(
      mac,
      ssoUser,
      rows,
      Await.result(reasonableAdjustmentsImporter.getReasonableAdjustments(member.universityId), Duration.Inf),
      importCommandFactory
    )
  }

  def staffMemberDetails(mac: MembershipInformation): ImportStaffMemberCommand = {
    val member = mac.member
    val ssoUser = new User(member.usercode)
    ssoUser.setFoundUser(true)
    ssoUser.setVerified(true)
    ssoUser.setDepartment(SandboxData.Departments(member.departmentCode).name)
    ssoUser.setDepartmentCode(member.departmentCode)
    ssoUser.setEmail(member.email)
    ssoUser.setFirstName(member.preferredForenames)
    ssoUser.setLastName(member.preferredSurname)
    ssoUser.setStaff(true)
    ssoUser.setWarwickId(member.universityId)

    new ImportStaffMemberCommand(mac, ssoUser)
  }

  def membershipInfoByDepartment(department: Department): Seq[MembershipInformation] = {
    SandboxData.Departments.get(department.code).map(dept =>
      studentsForDepartment(dept) ++ staffForDepartment(dept)
    ).getOrElse(Seq())
  }

  def staffForDepartment(department: SandboxData.Department): IndexedSeq[MembershipInformation] =
    (department.staffStartId to department.staffEndId).map { uniId =>
      val gender = if (uniId % 2 == 0) Gender.Male else Gender.Female
      val name = SandboxData.randomName(uniId, gender)
      val title = "Professor"
      val userType = MemberUserType.Staff
      val groupName = "Academic staff"

      MembershipInformation(
        MembershipMember(
          uniId.toString,
          department.code,
          "%s.%s@tabula-sandbox.warwick.ac.uk".format(name.givenName.substring(0, 1), name.familyName),
          groupName,
          title,
          name.givenName,
          name.familyName,
          groupName,
          DateTime.now.minusYears(40).toLocalDate.withDayOfYear((uniId % 364) + 1),
          department.code + "s" + uniId.toString.takeRight(3),
          DateTime.now.minusYears(10).toLocalDate,
          null,
          DateTime.now,
          null,
          gender,
          null,
          userType,
          JBoolean(Some(true))
        )
      )
    }

  def studentsForDepartment(department: SandboxData.Department): Seq[MembershipInformation] =
    department.routes.values.flatMap { route =>
      (route.studentsStartId to route.studentsEndId).map { uniId =>
        val gender = if (uniId % 2 == 0) Gender.Male else Gender.Female
        val name = SandboxData.randomName(uniId, gender)
        val title = gender match {
          case Gender.Male => "Mr"
          case _ => "Miss"
        }
        // Every fifth student is part time
        val isPartTime = uniId % 5 == 0

        val userType = MemberUserType.Student
        val groupName = route.degreeType match {
          case DegreeType.Undergraduate => if (isPartTime) "Undergraduate - part-time" else "Undergraduate - full-time"
          case _ =>
            if (route.isResearch)
              if (isPartTime) "Postgraduate (research) PT" else "Postgraduate (research) FT"
            else if (isPartTime) "Postgraduate (taught) PT" else "Postgraduate (taught) FT"
        }

        val yearOfStudy = (uniId % 3) + 1
        val startDate = (AcademicYear.now() - (yearOfStudy - 1)).termOrVacation(AcademicYearPeriod.PeriodType.autumnTerm).firstDay
        val endDate = (AcademicYear.now() + (3 - yearOfStudy)).termOrVacation(AcademicYearPeriod.PeriodType.summerTerm).lastDay

        MembershipInformation(
          MembershipMember(
            uniId.toString,
            department.code,
            "%s.%s@tabula-sandbox.warwick.ac.uk".format(name.givenName.substring(0, 1), name.familyName),
            groupName,
            title,
            name.givenName,
            name.familyName,
            groupName,
            dateOfBirth = DateTime.now.minusYears(19).toLocalDate.withDayOfYear((uniId % 364) + 1),
            department.code + uniId.toString.takeRight(4),
            startDate,
            endDate,
            DateTime.now,
            null,
            gender,
            null,
            userType,
            null
          )
        )
      }
    }.toSeq

  def membershipInfoForIndividual(universityId: String): Option[MembershipInformation] =
    profileService.getMemberByUniversityIdStaleOrFresh(universityId).map { member =>
      MembershipInformation(
        MembershipMember(
          member.universityId,
          member.homeDepartment.code,
          member.email,
          member.groupName,
          member.title,
          member.firstName,
          member.lastName,
          member.jobTitle,
          member.dateOfBirth,
          member.userId,
          DateTime.now.minusYears(1).toLocalDate,
          DateTime.now.plusYears(2).toLocalDate,
          member.lastUpdatedDate,
          member.phoneNumber,
          member.gender,
          member.homeEmail,
          member.userType,
          member match {
            case staff: StaffMember => staff.teachingStaff
            case _ => null
          }
        )
      )
    }

  override def sitsStudentRows(universityIds: Seq[String]): Seq[SitsStudentRow] =
    universityIds.flatMap(sitsStudentRows)

  def getUniversityIdsPresentInMembership(universityIds: Set[String]): Set[String] = throw new UnsupportedOperationException

  def applicantExistsInSits(universityId: String): Boolean = throw new UnsupportedOperationException

  def applicantsExistingInSits(universityIds: Set[String]): Set[String] = throw new UnsupportedOperationException
}

object ProfileImporter extends Logging {

  import ImportMemberHelpers._

  type UniversityId = String

  val applicantDepartmentCode: String = "sl"
  val sitsSchema: String = Wire.property("${schema.sits}")

  /**
   * The length of time after a student's end date for which their account
   * retains full access rights (i.e. that of a student). We treat them as
   * inactive after this time, although they will probably still be able to
   * login as a recent leaver account for an additional year.
   */
  val accountInactivationGracePeriod: Int = 7 * 8 // 8 weeks

  private def GetStudentInformation =
    f"""
			select
			stu.stu_code as university_id,
			stu.stu_titl as title,
			stu.stu_fusd as preferred_forename,
			trim(stu.stu_fnm1 || ' ' || stu.stu_fnm2 || ' ' || stu.stu_fnm3) as forenames,
			stu.stu_surn as family_name,
			stu.stu_gend as gender,
			stu.stu_caem as email_address,
			stu.stu_udf3 as user_code,
			stu.stu_dob as date_of_birth,
			case when coalesce(stu.stu_endd, scj.scj_endd) + $accountInactivationGracePeriod < sysdate then 'Inactive' else 'Active' end as in_use_flag,
			stu.stu_haem as alternative_email_address,
			stu.stu_cat3 as mobile_number,
			stu.stu_dsbc as disability,
			stu.stu_dsba as disabilityFunding,

			nat.nat_name as nationality,
			nat2.nat_name as second_nationality,
      case
        when
          (nat.nat_edid is not null and nat.nat_edid = 0) or
          (nat2.nat_edid is not null and nat2.nat_edid = 0)
        then 0
        else 1
      end as tier4_visa_requirement,

			crs.crs_code as course_code,
			crs.crs_ylen as course_year_length,

			spr.spr_code as spr_code,
			spr.rou_code as route_code,
			spr.spr_dptc as department_code,
			spr.awd_code as award_code,
			spr.sts_code as spr_status_code,
			spr.spr_levc as level_code,
			prs.prs_udf1 as spr_tutor1,
			spr.spr_ayrs as spr_academic_year_start,

      spr.spr_udf4 as special_exam_arrangements,
      spr.spr_udf5 as special_exam_arrangements_room_code,
      rom.rom_name as special_exam_arrangements_room_name,
      spr.spr_udf6 as special_exam_arrangements_extra_time,

			scj.scj_code as scj_code,
			scj.scj_begd as begin_date,
			scj.scj_endd as end_date,
			scj.scj_eend as expected_end_date,
			scj.scj_udfa as most_signif_indicator,
			scj.scj_stac as scj_status_code,
			scj.scj_prsc as scj_tutor1,
			scj.scj_rftc as scj_transfer_reason_code,

			sce.sce_sfcc as funding_source,
			sce.sce_stac as enrolment_status_code,
			sce.sce_blok as study_block, -- formally year_of_study
			sce.sce_moac as mode_of_attendance_code,
			sce.sce_occl as block_occurrence,
			sce.sce_ayrc as sce_academic_year,
			sce.sce_seq2 as sce_sequence_number,
			sce.sce_dptc as enrolment_department_code,
			sce.sce_udfj as sce_agreed_mark,
			sce.sce_rouc as sce_route_code,

			cbo.cbo_levc as study_level,

			ssn.ssn_mrgs as mod_reg_status,

			mst.mst_type as mst_type -- D for deceased.  Other values are L (live record) and N (no MRE records)

		from $sitsSchema.ins_stu stu -- Student

			join $sitsSchema.ins_spr spr -- Student Programme Route
				on stu.stu_code = spr.spr_stuc

			join $sitsSchema.srs_scj scj -- Student Course Join
				on spr.spr_code = scj.scj_sprc

			join $sitsSchema.srs_sce sce -- Student Course Enrolment
				on scj.scj_code = sce.sce_scjc
				and sce.sce_seq2 = -- get the last course enrolment record for the course and year
					(
						select max(sce2.sce_seq2)
							from $sitsSchema.srs_sce sce2
								where sce.sce_scjc = sce2.sce_scjc
								and sce2.sce_ayrc = sce.sce_ayrc
					)

			left join $sitsSchema.srs_cbo cbo -- Course Block Occurrence
				on sce.SCE_CRSC = cbo.CBO_CRSC and sce.SCE_BLOK = cbo.CBO_BLOK and sce.SCE_OCCL = cbo.CBO_OCCL and sce.SCE_AYRC = cbo.CBO_AYRC

			join $sitsSchema.men_mre mre -- Master Related Entity
        on stu.stu_code = mre.mre_code and mre.mre_mrcc = 'STU'

      join $sitsSchema.srs_mst mst -- Master Student
        on mst.mst_code = mre.mre_mstc

			left outer join $sitsSchema.srs_crs crs -- Course
				on sce.sce_crsc = crs.crs_code

			left outer join $sitsSchema.srs_nat nat -- Nationality
				on stu.stu_natc = nat.nat_code

			left outer join $sitsSchema.srs_nat nat2 -- Nationality
				on stu.stu_nat1 = nat2.nat_code

			left outer join $sitsSchema.srs_sta sts -- Status
				on spr.sts_code = sts.sta_code

			left outer join $sitsSchema.cam_ssn ssn -- module registration status
				on spr.spr_code = ssn.ssn_sprc
				and sce.sce_ayrc = ssn.ssn_ayrc

			left outer join $sitsSchema.ins_prs prs -- Personnel
				on spr.prs_code = prs.prs_code

      left outer join $sitsSchema.ins_rom rom -- Room (for special exam arrangements)
        on spr.spr_udf5 = rom.rom_code
		 """

  def GetSingleStudentInformation: UniversityId = GetStudentInformation +
    f"""
			where stu.stu_code = :universityId
			order by stu.stu_code
		"""

  class StudentInformationQuery(ds: DataSource)
    extends MappingSqlQuery[SitsStudentRow](ds, GetSingleStudentInformation) {

    declareParameter(new SqlParameter("universityId", Types.VARCHAR))

    compile()

    override def mapRow(rs: ResultSet, rowNumber: Int) = SitsStudentRow(rs)
  }

  val GetApplicantInformation =
    f"""
		select
			stu.stu_code as universityId,
			'SL' as deptCode,
			stu.stu_caem as mail,
			'Applicant' as targetGroup,
			stu.stu_titl as title,
			stu.stu_fusd as preferredFirstname,
			stu.stu_surn as preferredSurname,
			'Applicant' as jobTitle,
			to_char(stu.stu_dob, 'yyyy/mm/dd') as dateOfBirth,
			null as cn,
			to_char(stu.stu_begd, 'yyyy/mm/dd') as startDate,
			to_char(stu.stu_endd, 'yyyy/mm/dd') as endDate,
			stu.stu_updd as last_modification_date,
			null as telephoneNumber,
			stu.stu_gend as gender,
			stu.stu_haem as externalEmail,
			stu.stu_dsbc as disability,
			stu.stu_dsba as disabilityFunding,
			stu.stu_cat3 as mobile_number,
			nat.nat_name as nationality,
			nat2.nat_name as second_nationality,
	 		'N' as warwickTeachingStaff
		from $sitsSchema.ins_stu stu
			left outer join $sitsSchema.srs_nat nat on stu.stu_natc = nat.nat_code
			left outer join $sitsSchema.srs_nat nat2 on stu.stu_nat1 = nat2.nat_code
		where
			stu.stu_sta1 like '%%A' and -- applicant
			stu.stu_sta2 is null -- no student status
		"""

  val GetApplicantsByUniversityIdInformation = f"""$GetApplicantInformation and stu.stu_code in (:universityIds)"""

  class ApplicantQuery(ds: DataSource) extends MappingSqlQuery[(MembershipMember, SitsApplicantInfo)](ds, GetApplicantInformation) {

    val SqlDatePattern = "yyyy/MM/dd"
    val SqlDateTimeFormat: DateTimeFormatter = DateTimeFormat.forPattern(SqlDatePattern)

    compile()

    override def mapRow(rs: ResultSet, rowNumber: Int): (MembershipMember, SitsApplicantInfo) = toApplicantMember(rs, guessUsercode = false, SqlDateTimeFormat)
  }

  val GetMembershipByUniversityIdInformation =
    """
		select * from FIMSynchronizationService.dbo.UOW_Current_Accounts where warwickPrimary = 'Yes' and universityId in (:universityIds)
		"""

  class MembershipByUniversityIdQuery(ds: DataSource) extends MappingSqlQuery[MembershipMember](ds, GetMembershipByUniversityIdInformation) {
    declareParameter(new SqlParameter("universityIds", Types.VARCHAR))
    compile()

    override def mapRow(rs: ResultSet, rowNumber: Int): MembershipMember = membershipToMember(rs)
  }

  class ApplicantByUniversityIdQuery(ds: DataSource) extends MappingSqlQuery[(MembershipMember, SitsApplicantInfo)](ds, GetApplicantsByUniversityIdInformation) {
    declareParameter(new SqlParameter("universityIds", Types.VARCHAR))
    val SqlDatePattern = "yyyy/MM/dd"
    val SqlDateTimeFormat: DateTimeFormatter = DateTimeFormat.forPattern(SqlDatePattern)

    compile()

    override def mapRow(rs: ResultSet, rowNumber: Int): (MembershipMember, SitsApplicantInfo) = toApplicantMember(rs, guessUsercode = false, SqlDateTimeFormat)
  }

  val GetUniversityIdsPresentInMembership =
    """
		select universityId from FIMSynchronizationService.dbo.UOW_Current_Accounts where warwickPrimary = 'Yes' and universityId in (:universityIds) and (courseCode is null or courseCode != 'APPL')
	"""

  class MembershipUniversityIdPresenceQuery(ds: DataSource) extends MappingSqlQuery[String](ds, GetUniversityIdsPresentInMembership) {
    declareParameter(new SqlParameter("universityIds", Types.VARCHAR))
    compile()

    override def mapRow(rs: ResultSet, rowNumber: Int): String = rs.getString("universityId")
  }

  val GetMembershipByDepartmentInformation =
    """
		select * from FIMSynchronizationService.dbo.UOW_Current_Accounts where warwickPrimary = 'Yes' and deptCode = :departmentCode
		"""

  class MembershipByDepartmentQuery(ds: DataSource) extends MappingSqlQuery[MembershipMember](ds, GetMembershipByDepartmentInformation) {
    declareParameter(new SqlParameter("departmentCode", Types.VARCHAR))
    compile()

    override def mapRow(rs: ResultSet, rowNumber: Int): MembershipMember = membershipToMember(rs)
  }

  private val usercodeOverrides: Map[UniversityId, String] =
    Wire.property("${membership.usercode_overrides}")
      .split(',')
      .filter(_.hasText)
      .map(_.split(":", 2))
      .map { case Array(universityId, usercode) => universityId -> usercode }
      .toMap

  private def membershipToMember(rs: ResultSet, guessUsercode: Boolean = true, dateTimeFormater: DateTimeFormatter = ISODateTimeFormat.dateHourMinuteSecondMillis()) =
    MembershipMember(
      universityId = rs.getString("universityId"),
      departmentCode = rs.getString("deptCode"),
      email = rs.getString("mail"),
      targetGroup = rs.getString("targetGroup"),
      title = rs.getString("title"),
      preferredForenames = rs.getString("preferredFirstname"),
      preferredSurname = rs.getString("preferredSurname"),
      position = rs.getString("jobTitle"),
      dateOfBirth = rs.getString("dateOfBirth").maybeText.map(dateTimeFormater.parseLocalDate).orNull,
      usercode = usercodeOverrides.getOrElse(rs.getString("universityId"), rs.getString("cn").maybeText.getOrElse(if (guessUsercode) s"u${rs.getString("universityId")}" else null)),
      startDate = rs.getString("startDate").maybeText.flatMap(d => Try(dateTimeFormater.parseLocalDate(d)).toOption).orNull,
      endDate = rs.getString("endDate").maybeText.map(dateTimeFormater.parseLocalDate).getOrElse(LocalDate.now.plusYears(100)),
      modified = sqlDateToDateTime(rs.getDate("last_modification_date")),
      phoneNumber = rs.getString("telephoneNumber"), // unpopulated in FIM
      gender = Gender.fromCode(rs.getString("gender")),
      alternativeEmailAddress = rs.getString("externalEmail"),
      userType = MemberUserType.fromTargetGroup(rs.getString("targetGroup")),
      teachingStaff = JBoolean(Option(rs.getString("warwickTeachingStaff")).map(_ == "Y"))
    )

  private def toApplicantMember(rs: ResultSet, guessUsercode: Boolean = true, dateTimeFormatter: DateTimeFormatter = ISODateTimeFormat.dateHourMinuteSecondMillis()) = {

    implicit val resultSet: Option[ResultSet] = Some(rs)

    val applicantInfo = SitsApplicantInfo(
      mobileNumber = optString("mobile_number").orNull,
      disability = optString("disability").orNull,
      disabilityFunding = optString("disabilityFunding").orNull,
      nationality = optString("nationality").orNull,
      secondNationality = optString("second_nationality").orNull
    )

    (membershipToMember(rs, guessUsercode, dateTimeFormatter), applicantInfo)
  }

  private def sqlDateToDateTime(date: java.sql.Date): DateTime =
    (Option(date) map {
      new DateTime(_)
    }).orNull

}

object MembershipMember {
  def apply(m: Member): MembershipMember = MembershipMember(
    universityId = m.universityId,
    departmentCode = m.homeDepartment.code,
    email = m.email,
    targetGroup = null,
    title = m.title,
    preferredForenames = m.firstName,
    preferredSurname = m.lastName,
    position = m.jobTitle,
    dateOfBirth = m.dateOfBirth,
    usercode = m.userId,
    startDate = null,
    endDate = null,
    modified = null,
    phoneNumber = null,
    gender = null,
    alternativeEmailAddress = null,
    userType = m.userType,
    teachingStaff = null
  )
}

case class MembershipMember(
  universityId: String = null,
  departmentCode: String = null,
  email: String = null,
  targetGroup: String = null,
  title: String = null,
  preferredForenames: String = null,
  preferredSurname: String = null,
  position: String = null,
  dateOfBirth: LocalDate = null,
  usercode: String = null,
  startDate: LocalDate = null,
  endDate: LocalDate = null,
  modified: DateTime = null,
  phoneNumber: String = null,
  gender: Gender = null,
  alternativeEmailAddress: String = null,
  userType: MemberUserType,
  teachingStaff: JBoolean
)

case class SitsApplicantInfo(
  mobileNumber: String = null,
  disability: String = null,
  disabilityFunding: String = null,
  nationality: String = null,
  secondNationality: String = null,
)


trait ProfileImporterComponent {
  def profileImporter: ProfileImporter
}

trait AutowiringProfileImporterComponent extends ProfileImporterComponent {
  var profileImporter: ProfileImporter = Wire[ProfileImporter]
}
