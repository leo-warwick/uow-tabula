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
import uk.ac.warwick.tabula.commands.scheduling.imports._
import uk.ac.warwick.tabula.commands.{Command, Unaudited}
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model.MemberUserType._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.scheduling.{ImportCommandFactory, SitsStudentRow}
import uk.ac.warwick.tabula.sandbox.{MapResultSet, SandboxData}
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileService}
import uk.ac.warwick.tabula.{AcademicYear, Features}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.immutable.IndexedSeq
import scala.util.Try
import uk.ac.warwick.tabula.JavaImports._

case class MembershipInformation(member: MembershipMember)

trait ProfileImporter {
	import ProfileImporter._

	var features: Features = Wire[Features]

	def getMemberDetails(memberInfo: Seq[MembershipInformation], users: Map[UniversityId, User], importCommandFactory: ImportCommandFactory)
		: Seq[ImportMemberCommand]
	def membershipInfoByDepartment(department: Department): Seq[MembershipInformation]
	def membershipInfoForIndividual(universityId: String): Option[MembershipInformation]
	def multipleStudentInformationQuery: MultipleStudentInformationQuery
}

@Profile(Array("dev", "test", "production"))
@Service
class ProfileImporterImpl extends ProfileImporter with Logging with SitsAcademicYearAware {
	import ProfileImporter._

	var sits: DataSource = Wire[DataSource]("sitsDataSource")

	var fim: DataSource = Wire[DataSource]("fimDataSource")

	lazy val membershipByDepartmentQuery = new MembershipByDepartmentQuery(fim)
	lazy val membershipByUniversityIdQuery = new MembershipByUniversityIdQuery(fim)

	lazy val applicantQuery = new ApplicantQuery(sits)

	def studentInformationQuery: StudentInformationQuery = new StudentInformationQuery(sits)

	def multipleStudentInformationQuery: MultipleStudentInformationQuery = new MultipleStudentInformationQuery(sits)

	def getMemberDetails(memberInfo: Seq[MembershipInformation], users: Map[UniversityId, User], importCommandFactory: ImportCommandFactory)
		: Seq[ImportMemberCommand] = {
		// TODO we could probably chunk this into 20 or 30 users at a time for the query, or even split by category and query all at once

		memberInfo.groupBy(_.member.userType).flatMap { case (userType, members) =>
			userType match {
				case Staff | Emeritus => members.map { info =>
					val ssoUser = users(info.member.universityId)
					new ImportStaffMemberCommand(info, ssoUser)
				}
				case Student =>	members.map { info =>
						val universityId = info.member.universityId
						val ssoUser = users(universityId)

						val sitsRows = studentInformationQuery.executeByNamedParam(
							Map("universityId" -> universityId)
						).toSeq
						ImportStudentRowCommand(
							info,
							ssoUser,
							sitsRows,
							importCommandFactory
						)
					}.seq
				case Applicant | Other => members.map { info =>
					val ssoUser = users(info.member.universityId)
					new ImportOtherMemberCommand(info, ssoUser)
				}
				case _ => Seq()
			}
		}.toSeq
	}

	def membershipInfoByDepartment(department: Department): Seq[MembershipInformation] =
		// Magic student recruitment department - get membership information directly from SITS for applicants
		if (department.code == applicantDepartmentCode) {
			val members = applicantQuery.execute().toSeq
			val universityIds = members.map { _.universityId }

			// Filter out people in UOW_CURRENT_MEMBERS to avoid double import
			val universityIdsInMembership =
				universityIds.grouped(Daoisms.MaxInClauseCount).flatMap { ids =>
					membershipByUniversityIdQuery.executeByNamedParam(Map("universityIds" -> ids.asJavaCollection)).asScala
						.map { _.universityId }
				}

			members.filterNot { m => universityIdsInMembership.contains(m.universityId) }.map { member => MembershipInformation(member) }
		} else {
			membershipByDepartmentQuery.executeByNamedParam(Map("departmentCode" -> department.code.toUpperCase)).toSeq map { member =>
				MembershipInformation(member)
			}
		}

	def membershipInfoForIndividual(universityId: String): Option[MembershipInformation] = {
		membershipByUniversityIdQuery.executeByNamedParam(Map("universityIds" -> universityId)).asScala.toList match {
			case Nil => None
			case mem: List[MembershipMember] => Some (
					MembershipInformation(
						mem.head
					)
				)
		}
	}
}

@Profile(Array("sandbox")) @Service
class SandboxProfileImporter extends ProfileImporter {
	var profileService: ProfileService = Wire[ProfileService]

	def getMemberDetails(memberInfo: Seq[MembershipInformation], users: Map[String, User], importCommandFactory: ImportCommandFactory): Seq[ImportMemberCommand] =
		memberInfo map { info => info.member.userType match {
			case Student => studentMemberDetails(importCommandFactory)(info)
			case _ => staffMemberDetails(info)
		}}

	def studentMemberDetails(importCommandFactory: ImportCommandFactory)(mac: MembershipInformation): ImportStudentRowCommandInternal with Command[Member] with AutowiringProfileServiceComponent with AutowiringTier4RequirementImporterComponent with AutowiringModeOfAttendanceImporterComponent with Unaudited = {
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

		val route = SandboxData.route(member.universityId.toLong)
		val yearOfStudy = ((member.universityId.toLong % 3) + 1).toInt

		val rows = (1 to yearOfStudy).map(thisYearOfStudy => {
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
				"alternative_email_address" -> null,
				"mobile_number" -> null,
				"nationality" -> "British (ex. Channel Islands & Isle of Man)",
				"course_code" -> "%c%s-%s".format(route.courseType.courseCodeChar, member.departmentCode.toUpperCase, route.code.toUpperCase),
				"course_year_length" -> "3",
				"spr_code" -> "%s/1".format(member.universityId),
				"route_code" -> route.code.toUpperCase,
				"department_code" -> member.departmentCode.toUpperCase,
				"award_code" -> (if (route.degreeType == DegreeType.Undergraduate) "BA" else "MA"),
				"spr_status_code" -> "C",
				"scj_status_code" -> "C",
				"level_code" -> thisYearOfStudy.toString,
				"spr_tutor1" -> null,
				"spr_academic_year_start" -> (AcademicYear.guessSITSAcademicYearByDate(DateTime.now) - yearOfStudy + 1).toString,
				"scj_tutor1" -> null,
				"scj_transfer_reason_code" -> null,
				"scj_code" -> "%s/1".format(member.universityId),
				"begin_date" -> member.startDate.toDateTimeAtStartOfDay(),
				"end_date" -> member.endDate.toDateTimeAtStartOfDay(),
				"expected_end_date" -> member.endDate.toDateTimeAtStartOfDay(),
				"most_signif_indicator" -> "Y",
				"funding_source" -> null,
				"enrolment_status_code" -> "C",
				"year_of_study" -> thisYearOfStudy,
				"mode_of_attendance_code" -> (if (member.universityId.toLong % 5 == 0) "P" else "F"),
				"sce_academic_year" -> (AcademicYear.guessSITSAcademicYearByDate(DateTime.now) - (yearOfStudy - thisYearOfStudy)).toString,
				"sce_sequence_number" -> thisYearOfStudy,
				"sce_route_code" -> route.code.toUpperCase,
				"enrolment_department_code" -> member.departmentCode.toUpperCase,
				"mod_reg_status" -> "CON",
				"disability" -> "A",
				"mst_type" -> "L",
				"sce_agreed_mark" -> new JBigDecimal((member.universityId ++ member.universityId).toCharArray.map(char =>
					char.toString.toInt * member.universityId.toCharArray.apply(0).toString.toInt * thisYearOfStudy
				).sum % 100)
			)))
		})

		ImportStudentRowCommand(
			mac,
			ssoUser,
			rows,
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
					userType
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
						else
							if (isPartTime) "Postgraduate (taught) PT" else "Postgraduate (taught) FT"
				}

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
						DateTime.now.minusYears(19).toLocalDate.withDayOfYear((uniId % 364) + 1),
						department.code + uniId.toString.takeRight(4),
						DateTime.now.minusYears(1).toLocalDate,
						DateTime.now.plusYears(2).toLocalDate,
						DateTime.now,
						null,
						gender,
						null,
						userType
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
					member.userType
				)
			)
		}

	def multipleStudentInformationQuery = throw new UnsupportedOperationException
}

object ProfileImporter extends Logging {
	var features: Features = Wire[Features]

	type UniversityId = String

	val applicantDepartmentCode: String = "sl"
	val sitsSchema: String = Wire.property("${schema.sits}")

	private def GetStudentInformation = f"""
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
			case when stu.stu_endd < sysdate then 'Inactive' else 'Active' end as in_use_flag,
			stu.stu_haem as alternative_email_address,
			stu.stu_cat3 as mobile_number,
			stu.stu_dsbc as disability,

			nat.nat_name as nationality,

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
			sce.sce_blok as year_of_study,
			sce.sce_moac as mode_of_attendance_code,
			sce.sce_ayrc as sce_academic_year,
			sce.sce_seq2 as sce_sequence_number,
			sce.sce_dptc as enrolment_department_code,
			sce.sce_udfj as sce_agreed_mark,
			sce.sce_rouc as sce_route_code,

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

			join $sitsSchema.srs_mst mst -- Master Student
				on stu.stu_code = mst.mst_adid
				and mst.mst_code =
					(
						select max(mst2.mst_code)
							from $sitsSchema.srs_mst mst2
							where mst.mst_adid = mst2.mst_adid
					)

			left outer join $sitsSchema.srs_crs crs -- Course
				on sce.sce_crsc = crs.crs_code

			left outer join $sitsSchema.srs_nat nat -- Nationality
				on stu.stu_natc = nat.nat_code

			left outer join $sitsSchema.srs_sta sts -- Status
				on spr.sts_code = sts.sta_code

			left outer join $sitsSchema.cam_ssn ssn -- module registration status
				on spr.spr_code = ssn.ssn_sprc
				and sce.sce_ayrc = ssn.ssn_ayrc

			left outer join $sitsSchema.ins_prs prs -- Personnel
				on spr.prs_code = prs.prs_code
		 """

	def GetSingleStudentInformation: UniversityId = GetStudentInformation + f"""
			where stu.stu_code = :universityId
			order by stu.stu_code
		"""

	def GetMultipleStudentsInformation: UniversityId = GetStudentInformation + f"""
			where stu.stu_code in (:universityIds)
			order by stu.stu_code
		"""

	class StudentInformationQuery(ds: DataSource)
		extends MappingSqlQuery[SitsStudentRow](ds, GetSingleStudentInformation) {

		var features: Features = Wire.auto[Features]

		declareParameter(new SqlParameter("universityId", Types.VARCHAR))

		compile()

		override def mapRow(rs: ResultSet, rowNumber: Int) = SitsStudentRow(rs)
	}

	class MultipleStudentInformationQuery(ds: DataSource)
		extends MappingSqlQuery[SitsStudentRow](ds, GetMultipleStudentsInformation) {

		var features: Features = Wire.auto[Features]

		declareParameter(new SqlParameter("universityIds", Types.VARCHAR))

		compile()

		override def mapRow(rs: ResultSet, rowNumber: Int) = SitsStudentRow(rs)
	}

	val GetApplicantInformation = f"""
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
			stu.stu_haem as externalEmail
		from $sitsSchema.ins_stu stu
		where
			stu.stu_sta1 like '%%A' and -- applicant
			stu.stu_sta2 is null and -- no student status
			stu.stu_udf3 is null -- no IT account
		"""

	class ApplicantQuery(ds: DataSource) extends MappingSqlQuery[MembershipMember](ds, GetApplicantInformation) {

		val SqlDatePattern = "yyyy/MM/dd"
		val SqlDateTimeFormat: DateTimeFormatter = DateTimeFormat.forPattern(SqlDatePattern)

		compile()
		override def mapRow(rs: ResultSet, rowNumber: Int): MembershipMember = membershipToMember(rs, guessUsercode = false, SqlDateTimeFormat)
	}

	val GetMembershipByUniversityIdInformation = """
		select * from FIMSynchronizationService.dbo.UOW_Current_Accounts where warwickPrimary = 'Yes' and universityId in (:universityIds)
		"""

	class MembershipByUniversityIdQuery(ds: DataSource) extends MappingSqlQuery[MembershipMember](ds, GetMembershipByUniversityIdInformation) {
		declareParameter(new SqlParameter("universityIds", Types.VARCHAR))
		compile()
		override def mapRow(rs: ResultSet, rowNumber: Int): MembershipMember = membershipToMember(rs)
	}

	val GetMembershipByDepartmentInformation = """
		select * from FIMSynchronizationService.dbo.UOW_Current_Accounts where warwickPrimary = 'Yes' and deptCode = :departmentCode
		"""

	class MembershipByDepartmentQuery(ds: DataSource) extends MappingSqlQuery[MembershipMember](ds, GetMembershipByDepartmentInformation) {
		declareParameter(new SqlParameter("departmentCode", Types.VARCHAR))
		compile()
		override def mapRow(rs: ResultSet, rowNumber: Int): MembershipMember = membershipToMember(rs)
	}

	private def membershipToMember(rs: ResultSet, guessUsercode: Boolean = true, dateTimeFormater: DateTimeFormatter = ISODateTimeFormat.dateHourMinuteSecondMillis()) =
		MembershipMember(
			universityId 						= rs.getString("universityId"),
			departmentCode					= rs.getString("deptCode"),
			email										= rs.getString("mail"),
			targetGroup							= rs.getString("targetGroup"),
			title										= rs.getString("title"),
			preferredForenames			= rs.getString("preferredFirstname"),
			preferredSurname				= rs.getString("preferredSurname"),
			position								= rs.getString("jobTitle"),
			dateOfBirth							= rs.getString("dateOfBirth").maybeText.map(dateTimeFormater.parseLocalDate).orNull,
			usercode								= rs.getString("cn").maybeText.getOrElse(if (guessUsercode) s"u${rs.getString("universityId")}" else null),
			startDate								= rs.getString("startDate").maybeText.flatMap(d => Try(dateTimeFormater.parseLocalDate(d)).toOption).orNull,
			endDate									= rs.getString("endDate").maybeText.map(dateTimeFormater.parseLocalDate).getOrElse(LocalDate.now.plusYears(100)),
			modified								= sqlDateToDateTime(rs.getDate("last_modification_date")),
			phoneNumber							= rs.getString("telephoneNumber"), // unpopulated in FIM
			gender									= Gender.fromCode(rs.getString("gender")),
			alternativeEmailAddress	= rs.getString("externalEmail"),
			userType								= MemberUserType.fromTargetGroup(rs.getString("targetGroup"))
		)

	private def sqlDateToDateTime(date: java.sql.Date): DateTime =
		(Option(date) map { new DateTime(_) }).orNull

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
	userType: MemberUserType
)


trait ProfileImporterComponent {
	def profileImporter: ProfileImporter
}

trait AutowiringProfileImporterComponent extends ProfileImporterComponent {
	var profileImporter: ProfileImporter = Wire[ProfileImporter]
}