package uk.ac.warwick.tabula.services.scheduling

import java.sql.{ResultSet, Types}
import javax.sql.DataSource

import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.JBigDecimal
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.commands.scheduling.imports.ImportModuleRegistrationsCommand
import uk.ac.warwick.tabula.data.model.MemberUserType.Student
import uk.ac.warwick.tabula.data.model.{Module, StudentCourseDetails}
import uk.ac.warwick.tabula.data.{MemberDaoImpl, StudentCourseDetailsDao}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.sandbox.SandboxData
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.services.scheduling.ModuleRegistrationImporter.{AutoUploadedConfirmedModuleRegistrationsQuery, ConfirmedModuleRegistrationsQuery, UnconfirmedModuleRegistrationsQuery}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import scala.collection.immutable.{HashMap, Iterable}
import scala.util.Try

/**
 * Import module registration data from SITS.
 *
 */
trait ModuleRegistrationImporter {
	def getModuleRegistrationDetails(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]): Seq[ImportModuleRegistrationsCommand]
}

trait AbstractModuleRegistrationImporter extends ModuleRegistrationImporter with Logging {

	var studentCourseDetailsDao: StudentCourseDetailsDao = Wire[StudentCourseDetailsDao]
	var moduleAndDepartmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]

	protected def applyForRows(rows: Seq[ModuleRegistrationRow]): Iterable[ImportModuleRegistrationsCommand] = {
		val tabulaModules: Set[Module] = rows.groupBy(_.sitsModuleCode).flatMap { case (sitsModuleCode, moduleRows) =>
			moduleAndDepartmentService.getModuleBySitsCode(sitsModuleCode) match {
				case None =>
					logger.warn(s"No stem module for $sitsModuleCode found in Tabula for SCJ: ${moduleRows.map(_.scjCode).distinct.mkString(", ")}")
					None
				case Some(module) => Some(module)
			}
		}.toSet
		val tabulaModuleCodes = tabulaModules.map(_.code)
		val rowsBySCD: Map[StudentCourseDetails, Seq[ModuleRegistrationRow]] = rows.groupBy(_.scjCode).map { case (scjCode, scjRows) =>
			studentCourseDetailsDao.getByScjCode(scjCode).getOrElse {
				logger.error("Can't record module registration - could not find a StudentCourseDetails for " + scjCode)
				null
			} -> scjRows.filter(row => {
				val moduleCode = Module.stripCats(row.sitsModuleCode)
				moduleCode.isDefined && tabulaModuleCodes.contains(moduleCode.get.toLowerCase)
			})
		}
		rowsBySCD.filterKeys(_ != null).map { case (scd, scdRows) => new ImportModuleRegistrationsCommand(scd, scdRows, tabulaModules) }
	}
}

@Profile(Array("dev", "test", "production"))
@Service
class ModuleRegistrationImporterImpl extends AbstractModuleRegistrationImporter with TaskBenchmarking {

	var sits: DataSource = Wire[DataSource]("sitsDataSource")

	lazy val queries = Seq(
		new UnconfirmedModuleRegistrationsQuery(sits),
		new ConfirmedModuleRegistrationsQuery(sits),
		new AutoUploadedConfirmedModuleRegistrationsQuery(sits)
	)

	def getModuleRegistrationDetails(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]): Seq[ImportModuleRegistrationsCommand] = {
		benchmarkTask("Fetch module registrations") {
			val rows = membersAndCategories.filter { _.member.userType == Student }.flatMap { mac =>
				val universityId = mac.member.universityId
				val params = HashMap(("universityId", universityId))
				queries.flatMap { query =>
					query.executeByNamedParam(params.asJava).asScala
				}.distinct
			}.seq
			applyForRows(rows).toSeq
		}
	}
}

@Profile(Array("sandbox")) @Service
class SandboxModuleRegistrationImporter extends AbstractModuleRegistrationImporter {
	var memberDao: MemberDaoImpl = Wire.auto[MemberDaoImpl]

	def getModuleRegistrationDetails(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]): Seq[ImportModuleRegistrationsCommand] =
		membersAndCategories flatMap { mac =>
			val universityId = mac.member.universityId
			val ssoUser = users(universityId)

			mac.member.userType match {
				case Student => studentModuleRegistrationDetails(universityId, ssoUser)
				case _ => Seq()
			}
		}

	def studentModuleRegistrationDetails(universityId: String, ssoUser: User): Iterable[ImportModuleRegistrationsCommand] = {
		val rows = (for {
			(_, d) <- SandboxData.Departments
			route <- d.routes.values.toSeq
			if (route.studentsStartId to route.studentsEndId).contains(universityId.toInt)
			moduleCode <- route.moduleCodes
		} yield {
			val mark = (universityId ++ universityId ++ moduleCode.substring(3)).toCharArray.map(char =>
				Try(char.toString.toInt).toOption.getOrElse(0) * universityId.toCharArray.apply(0).toString.toInt
			).sum % 100
			val grade = SandboxData.GradeBoundaries.find(gb => gb.marksCode == "TABULA-UG" && gb.minimumMark <= mark && gb.maximumMark >= mark).map(_.grade).getOrElse("F")
			new ModuleRegistrationRow(
				scjCode = "%s/1".format(universityId),
				sitsModuleCode = "%s-15".format(moduleCode.toUpperCase),
				cats = new JBigDecimal(15),
				assessmentGroup = "A",
				selectionStatusCode = (universityId.toInt + Try(moduleCode.substring(3).toInt).getOrElse(0)) % 2 match {
					case 0 => "C"
					case _ => "O"
				},
				occurrence = "A",
				academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now).toString,
				actualMark = Some(new JBigDecimal(mark)),
				actualGrade = grade,
				agreedMark = Some(new JBigDecimal(mark)),
				agreedGrade = grade
			)
		}).toSeq

		applyForRows(rows)
	}
}

object ModuleRegistrationImporter {
	val sitsSchema: String = Wire.property("${schema.sits}")

	// union 3 things -
	// 1. unconfirmed module registrations from the SMS table
	// 2. confirmed module registrations from the SMO table where there is a module registration status of confirmed
	// 3. confirmed module registrations from the SMO table where no status is recorded, i.e. where MRs have been imported
	//
	// the 3 queries should be mutually exclusive - 1st has SSN_MRGS != CON, 2nd has SSN_MRGS == CON and 3rd has no SSN.
	//
	// Although the 3 queries aren't unioned in SQL now, the column names still need to match.

	def UnconfirmedModuleRegistrations = s"""
			select scj_code, sms.mod_code, sms.sms_mcrd as credit, sms.sms_agrp as assess_group,
			sms.ses_code, -- e.g. C for core or O for option
			sms.ayr_code, sms_occl as occurrence,
			null as smr_actm, -- actual mark
			null as smr_actg, -- actual grade
			null as smr_agrm, -- agreed mark
			null as smr_agrg -- agreed grade
				from $sitsSchema.ins_stu stu -- student
					join $sitsSchema.ins_spr spr -- Student Programme Route, needed for SPR code
						on spr.spr_stuc = stu.stu_code

					join $sitsSchema.srs_scj scj -- Student Course Join, needed for SCJ code
						on scj.scj_sprc = spr.spr_code

					join $sitsSchema.cam_sms sms -- Student Module Selection (unconfirmed module choices)
						on sms.spr_code = spr.spr_code

					join $sitsSchema.cam_ssn ssn -- holds student module registration status
						on sms.spr_code = ssn.ssn_sprc and ssn.ssn_ayrc = sms.ayr_code and ssn.ssn_mrgs != 'CON' -- module choices not confirmed
				where stu.stu_code = :universityId"""

	// The check on SMO_RTSC excludes WMG cancelled modules or module registrations
	def ConfirmedModuleRegistrations = s"""
			select scj_code, smo.mod_code, smo.smo_mcrd as credit, smo.smo_agrp as assess_group,
			smo.ses_code, smo.ayr_code, smo.mav_occur as occurrence,
			smr_actm, -- actual overall module mark
			smr_actg, -- actual overall module grade
			smr_agrm, -- agreed overall module mark
			smr_agrg -- agreed overall module grade
				from $sitsSchema.ins_stu stu
					join $sitsSchema.ins_spr spr
						on spr.spr_stuc = stu.stu_code

					join $sitsSchema.srs_scj scj
						on scj.scj_sprc = spr.spr_code

					join $sitsSchema.cam_smo smo
						on smo.spr_code = spr.spr_code
						and (smo_rtsc is null or (smo_rtsc not like 'X%' and smo_rtsc != 'Z')) -- exclude WMG cancelled registrations

					left join $sitsSchema.ins_smr smr -- Student Module Result
						on smo.spr_code = smr.spr_code
						and smo.ayr_code = smr.ayr_code
						and smo.mod_code = smr.mod_code
						and smo.mav_occur = smr.mav_occur

					join $sitsSchema.cam_ssn ssn
						on smo.spr_code = ssn.ssn_sprc and ssn.ssn_ayrc = smo.ayr_code and ssn.ssn_mrgs = 'CON'
				where stu.stu_code = :universityId"""

	// the left outer join to SSN excludes rows with a matching SSN since is only matching where SSN_SPRC is null
	// but that column has a non-null constraint
	def AutoUploadedConfirmedModuleRegistrations = s"""
			select scj_code, smo.mod_code, smo.smo_mcrd as credit, smo.smo_agrp as assess_group,
			smo.ses_code, smo.ayr_code, smo.mav_occur as occurrence, smr_actm, smr_actg, smr_agrm, smr_agrg
				from $sitsSchema.ins_stu stu
					join $sitsSchema.ins_spr spr
						on spr.spr_stuc = stu.stu_code

					join $sitsSchema.srs_scj scj
						on scj.scj_sprc = spr.spr_code

					join $sitsSchema.cam_smo smo
						on smo.spr_code = spr.spr_code
						and (smo_rtsc is null or (smo_rtsc not like 'X%' and smo_rtsc != 'Z'))

					left join $sitsSchema.ins_smr smr
						on smo.spr_code = smr.spr_code
						and smo.ayr_code = smr.ayr_code
						and smo.mod_code = smr.mod_code
						and smo.mav_occur = smr.mav_occur

					left outer join $sitsSchema.cam_ssn ssn
						on smo.spr_code = ssn.ssn_sprc and ssn.ssn_ayrc = smo.ayr_code
				where stu.stu_code = :universityId
					and ssn.ssn_sprc is null
			"""

	def mapResultSet(resultSet: ResultSet): ModuleRegistrationRow = {
		new ModuleRegistrationRow(
			resultSet.getString("scj_code"),
			resultSet.getString("mod_code"),
			resultSet.getBigDecimal("credit"),
			resultSet.getString("assess_group"),
			resultSet.getString("ses_code"),
			resultSet.getString("occurrence"),
			resultSet.getString("ayr_code"),
			Option(resultSet.getBigDecimal("smr_actm")),
			resultSet.getString("smr_actg"),
			Option(resultSet.getBigDecimal("smr_agrm")),
			resultSet.getString("smr_agrg")
		)
	}

	class UnconfirmedModuleRegistrationsQuery(ds: DataSource)
		extends MappingSqlQuery[ModuleRegistrationRow](ds, UnconfirmedModuleRegistrations) {
			declareParameter(new SqlParameter("universityId", Types.VARCHAR))
			compile()
			override def mapRow(resultSet: ResultSet, rowNumber: Int): ModuleRegistrationRow = mapResultSet(resultSet)
	}

	class ConfirmedModuleRegistrationsQuery(ds: DataSource)
		extends MappingSqlQuery[ModuleRegistrationRow](ds, ConfirmedModuleRegistrations) {
			declareParameter(new SqlParameter("universityId", Types.VARCHAR))
			compile()
			override def mapRow(resultSet: ResultSet, rowNumber: Int): ModuleRegistrationRow = mapResultSet(resultSet)
	}

	class AutoUploadedConfirmedModuleRegistrationsQuery(ds: DataSource)
		extends MappingSqlQuery[ModuleRegistrationRow](ds, AutoUploadedConfirmedModuleRegistrations) {
			declareParameter(new SqlParameter("universityId", Types.VARCHAR))
			compile()
			override def mapRow(resultSet: ResultSet, rowNumber: Int): ModuleRegistrationRow = mapResultSet(resultSet)
	}
}

// Full class rather than case class so it can be BeanWrapped
class ModuleRegistrationRow {

	var scjCode: String = _
	var sitsModuleCode: String = _
	var cats: JBigDecimal = _
	var assessmentGroup: String = _
	var selectionStatusCode: String = _
	var occurrence: String = _
	var academicYear: String = _
	var actualMark: Option[JBigDecimal] = _
	var actualGrade: String = _
	var agreedMark: Option[JBigDecimal] = _
	var agreedGrade: String = _

	def this(
		scjCode: String,
		sitsModuleCode: String,
		cats: JBigDecimal,
		assessmentGroup: String,
		selectionStatusCode: String,
		occurrence: String,
		academicYear: String,
		actualMark: Option[JBigDecimal],
		actualGrade: String,
		agreedMark: Option[JBigDecimal],
		agreedGrade: String
	) {
		this()
		this.scjCode = scjCode
		this.sitsModuleCode = sitsModuleCode
		this.cats = cats
		this.assessmentGroup = assessmentGroup
		this.selectionStatusCode = selectionStatusCode
		this.occurrence = occurrence
		this.academicYear = academicYear
		this.actualMark = actualMark
		this.actualGrade = actualGrade
		this.agreedMark = agreedMark
		this.agreedGrade = agreedGrade
	}

}