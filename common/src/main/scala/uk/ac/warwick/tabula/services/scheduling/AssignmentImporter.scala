package uk.ac.warwick.tabula.services.scheduling

import java.sql.{ResultSet, Types}
import javax.sql.DataSource

import org.joda.time.DateTime
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.{MappingSqlQuery, MappingSqlQueryWithParameters}
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.{RowCallbackHandler, SqlParameter}
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.sandbox.SandboxData
import uk.ac.warwick.tabula.services.scheduling.AssignmentImporter.{AssessmentComponentQuery, GradeBoundaryQuery, UpstreamAssessmentGroupQuery}

import scala.collection.JavaConverters._

trait AssignmentImporter {
	/**
	 * Iterates through ALL module registration elements,
	 * passing each ModuleRegistration item to the given callback for it to process.
	 */
	def allMembers(callback: UpstreamModuleRegistration => Unit): Unit

	def getAllAssessmentGroups: Seq[UpstreamAssessmentGroup]

	def getAllAssessmentComponents: Seq[AssessmentComponent]

	def getAllGradeBoundaries: Seq[GradeBoundary]

	var yearsToImport: Seq[AcademicYear] = AcademicYear.guessSITSAcademicYearByDate(DateTime.now).yearsSurrounding(0, 1)
}

@Profile(Array("dev", "test", "production")) @Service
class AssignmentImporterImpl extends AssignmentImporter with InitializingBean {

	var sits: DataSource = Wire[DataSource]("sitsDataSource")

	var upstreamAssessmentGroupQuery: UpstreamAssessmentGroupQuery = _
	var assessmentComponentQuery: AssessmentComponentQuery = _
	var gradeBoundaryQuery: GradeBoundaryQuery = _
	var jdbc: NamedParameterJdbcTemplate = _

	override def afterPropertiesSet() {
		assessmentComponentQuery = new AssessmentComponentQuery(sits)
		upstreamAssessmentGroupQuery = new UpstreamAssessmentGroupQuery(sits)
		gradeBoundaryQuery = new GradeBoundaryQuery(sits)
		jdbc = new NamedParameterJdbcTemplate(sits)
	}

	def getAllAssessmentComponents: Seq[AssessmentComponent] = assessmentComponentQuery.executeByNamedParam(JMap(
		"academic_year_code" -> yearsToImportArray)).asScala

	private def yearsToImportArray = yearsToImport.map(_.toString).asJava: JList[String]

	// This will be quite a few thousand records, but not more than
	// 20k. Shouldn't cause any memory problems, so no point complicating
	// it by trying to stream or batch the data.
	def getAllAssessmentGroups: Seq[UpstreamAssessmentGroup] = upstreamAssessmentGroupQuery.executeByNamedParam(JMap(
		"academic_year_code" -> yearsToImportArray)).asScala

	/**
	 * Iterates through ALL module registration elements in ADS (that's many),
	 * passing each ModuleRegistration item to the given callback for it to process.
	 */
	def allMembers(callback: UpstreamModuleRegistration => Unit) {
		val params: JMap[String, Object] = JMap(
			"academic_year_code" -> yearsToImportArray)
		jdbc.query(AssignmentImporter.GetAllAssessmentGroupMembers, params, new RowCallbackHandler {
			override def processRow(rs: ResultSet) {
				callback(UpstreamModuleRegistration(
					year = rs.getString("academic_year_code"),
					sprCode = rs.getString("spr_code"),
					seatNumber = rs.getString("seat_number"),
					occurrence = rs.getString("mav_occurrence"),
					sequence = rs.getString("sequence"),
					moduleCode = rs.getString("module_code"),
					assessmentGroup = convertAssessmentGroupFromSITS(rs.getString("assessment_group")),
					actualMark = rs.getString("actual_mark"),
					actualGrade = rs.getString("actual_grade"),
					agreedMark = rs.getString("agreed_mark"),
					agreedGrade = rs.getString("agreed_grade")
				))

			}
		})
	}

	/** Convert incoming null assessment groups into the NONE value */
	private def convertAssessmentGroupFromSITS(string: String) =
		if (string == null) AssessmentComponent.NoneAssessmentGroup
		else string

	def getAllGradeBoundaries: Seq[GradeBoundary] = gradeBoundaryQuery.execute().asScala
}

@Profile(Array("sandbox")) @Service
class SandboxAssignmentImporter extends AssignmentImporter {

	def allMembers(callback: UpstreamModuleRegistration => Unit): Unit = {
		var moduleCodesToIds = Map[String, Seq[Range]]()

		for {
			(code, d) <- SandboxData.Departments
			route <- d.routes.values.toSeq
			moduleCode <- route.moduleCodes
		} {
			val range = route.studentsStartId to route.studentsEndId

			moduleCodesToIds = moduleCodesToIds + (
				moduleCode -> (moduleCodesToIds.getOrElse(moduleCode, Seq()) :+ range)
			)
		}

		for {
			(moduleCode, ranges) <- moduleCodesToIds
			range <- ranges
			uniId <- range
		} callback(
			UpstreamModuleRegistration(
				year = AcademicYear.guessSITSAcademicYearByDate(DateTime.now).toString,
				sprCode = "%d/1".format(uniId),
				seatNumber = "0",
				occurrence = "A",
				sequence = "A01",
				moduleCode = "%s-15".format(moduleCode.toUpperCase),
				assessmentGroup = "A",
				actualMark = "",
				actualGrade = "",
				agreedMark = "",
				agreedGrade = ""
			)
		)

	}

	def getAllAssessmentGroups: Seq[UpstreamAssessmentGroup] =
		for {
			(code, d) <- SandboxData.Departments.toSeq
			route <- d.routes.values.toSeq
			moduleCode <- route.moduleCodes
		} yield {
			val ag = new UpstreamAssessmentGroup()
			ag.moduleCode = "%s-15".format(moduleCode.toUpperCase)
			ag.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
			ag.assessmentGroup = "A"
			ag.occurrence = "A"
			ag.sequence = "A01"
			ag
		}

	def getAllAssessmentComponents: Seq[AssessmentComponent] =
		for {
			(code, d) <- SandboxData.Departments.toSeq
			route <- d.routes.values.toSeq
			moduleCode <- route.moduleCodes
		} yield {
			val a = new AssessmentComponent
			a.moduleCode = "%s-15".format(moduleCode.toUpperCase)
			a.sequence = "A01"
			a.name = "Coursework"
			a.assessmentGroup = "A"
			a.assessmentType = AssessmentType.Assignment
			a.inUse = true
			a.marksCode = "TABULA-UG"
			a
		}

	def getAllGradeBoundaries: Seq[GradeBoundary] = SandboxData.GradeBoundaries
}



object AssignmentImporter {
	var sitsSchema: String = Wire.property("${schema.sits}")
	var sqlStringCastFunction: String = "to_char"
	var dialectRegexpLike = "regexp_like"

	// Because we have a mismatch between nvarchar2 and chars in the text, we need to cast some results to chars in Oracle, but not in HSQL
	def castToString(orig: String): String =
		if (sqlStringCastFunction.hasText) s"$sqlStringCastFunction($orig)"
		else orig

	/** Get AssessmentComponents, and also some fake ones for linking to
		* the group of students with no selected assessment group.
		*
		* The actual assessment components come from CAM_MAB ("Module Assessment Body") which contains the
		* assessment components which make up modules.
		* This is unioned with module registrations (in SMS and SMO) where assessment group (SMS_AGRP and SMO_AGRP) is not
		* specified.
		*
		* SMS holds unconfirmed module registrations and is included to catch module registrations not approved yet.
		* SMO holds confirmed module registrations and is included to catch module registrations in departments which
		* upload module registrations after confirmation.
		*/
	def GetAssessmentsQuery = s"""
		select distinct
			sms.mod_code as module_code,
			'${AssessmentComponent.NoneAssessmentGroup}' as seq,
			'Students not registered for assessment' as name,
			'${AssessmentComponent.NoneAssessmentGroup}' as assessment_group,
			'X' as assessment_code,
			'Y' as in_use,
			null as marks_code
			from $sitsSchema.cam_sms sms
				join $sitsSchema.cam_ssn ssn -- SSN table holds module registration status
					on sms.spr_code = ssn.ssn_sprc and ssn.ssn_ayrc = sms.ayr_code and ssn.ssn_mrgs != 'CON' -- mrgs = "Module Registration Status"
			where
				sms.sms_agrp is null and -- assessment group, ie group of assessment components which together represent an assessment choice
				sms.ayr_code in (:academic_year_code)
	union all
		select distinct
			smo.mod_code as module_code,
			'${AssessmentComponent.NoneAssessmentGroup}' as seq,
			'Students not registered for assessment' as name,
			'${AssessmentComponent.NoneAssessmentGroup}' as assessment_group,
			'X' as assessment_code,
			'Y' as in_use,
			null as marks_code
			from $sitsSchema.cam_smo smo
				left outer join $sitsSchema.cam_ssn ssn
					on smo.spr_code = ssn.ssn_sprc and ssn.ssn_ayrc = smo.ayr_code
			where -- RTSC is used by WMG to indicate attendance status.  X = cancelled, Z = module cancelled
				(smo.smo_rtsc is null or (smo.smo_rtsc not like 'X%' and smo.smo_rtsc != 'Z')) and
				ssn.ssn_sprc is null and -- there is no module registration status row, so this SMO has been uploaded rather than created in SITS
				smo.smo_agrp is null and -- assessment group, ie group of assessment components which together represent an assessment choice
				smo.ayr_code in (:academic_year_code)
	union all
		select
			mab.map_code as module_code,
			${castToString("mab.mab_seq")} as seq,
			${castToString("mab.mab_name")} as name,
			${castToString("mab.mab_agrp")} as assessment_group,
			${castToString("mab.ast_code")} as assessment_code,
			${castToString("mab.mab_udf1")} as in_use,
			${castToString("mab.mks_code")} as marks_code
			from $sitsSchema.cam_mab mab -- Module Assessment Body, containing assessment components
				join $sitsSchema.cam_mav mav -- Module Availability which indicates which modules are avaiable in the year
					on mab.map_code = mav.mod_code and
						 mav.psl_code = 'Y' and -- "Period Slot" code - Y indicates year
						 mav.ayr_code in (:academic_year_code)
				join $sitsSchema.ins_mod mod
					on mav.mod_code = mod.mod_code
			where	mod.mod_iuse = 'Y' and -- in use
						mod.mot_code not in ('S-', 'D') and -- MOT = module type code - not suspended, discontinued?
						mab.mab_agrp is not null"""

	def GetAllAssessmentGroups = s"""
		select distinct
			mav.ayr_code as academic_year_code,
			mav.mod_code as module_code,
			'${AssessmentComponent.NoneAssessmentGroup}' as mav_occurrence,
			'${AssessmentComponent.NoneAssessmentGroup}' as assessment_group,
			'${AssessmentComponent.NoneAssessmentGroup}' as seq
			from $sitsSchema.cam_mab mab
				join $sitsSchema.cam_mav mav
					on mab.map_code = mav.mod_code
				join $sitsSchema.ins_mod mod
					on mav.mod_code = mod.mod_code
			where mod.mod_iuse = 'Y' and -- in use
						mod.mot_code not in ('S-', 'D') and -- MOT = module type code - not suspended, discontinued?
						mav.psl_code = 'Y' and -- period slot code of Y (year)
						mav.ayr_code in (:academic_year_code)
	union all
		select distinct
			mav.ayr_code as academic_year_code,
			mav.mod_code as module_code,
			${castToString("mav.mav_occur")} as mav_occurrence, -- module occurrence (representing eg day or evening - usually 'A')
			${castToString("mab.mab_agrp")} as assessment_group, -- group of assessment components forming one assessment choice
			${castToString("mab.mab_seq")} as seq -- individual assessments (e.g each exam or coursework component)
			from $sitsSchema.cam_mab mab -- Module Assessment Body, containing assessment components
				join $sitsSchema.cam_mav mav -- Module Availability which indicates which modules are available in the year
					on mab.map_code = mav.mod_code
				join $sitsSchema.ins_mod mod
					on mav.mod_code = mod.mod_code
			where mod.mod_iuse = 'Y' and -- in use
						mod.mot_code not in ('S-', 'D') and -- module type - not suspended, discontinued?
						mav.psl_code = 'Y' and
						mab.mab_agrp is not null and
						mav.ayr_code in (:academic_year_code)"""

	// for students who register for modules through SITS,this gets their assessments before their choices are confirmed
	def GetUnconfirmedModuleRegistrations = s"""
		select
			sms.ayr_code as academic_year_code,
			spr.spr_code as spr_code,
			wss.wss_seat as seat_number,
			sms.sms_occl as mav_occurrence, -- module occurrence (representing eg day or evening - usually 'A')
			sms.mod_code as module_code,
			sms.sms_agrp as assessment_group,
			mab.mab_seq as sequence,
			sas.sas_actm as actual_mark,
			sas.sas_actg as actual_grade,
			sas.sas_agrm as agreed_mark,
			sas.sas_agrg as agreed_grade
				from $sitsSchema.srs_scj scj -- Student Course Join  - gives us most significant course
					join $sitsSchema.ins_spr spr -- Student Programme Route - gives us SPR code
						on scj.scj_sprc = spr.spr_code and
							(spr.sts_code is null or spr.sts_code not like 'P%' or spr.sts_code != 'D') -- no perm withdrawn or deceased students

					join $sitsSchema.cam_sms sms -- Student Module Selection table, storing unconfirmed module registrations
						on sms.spr_code = scj.scj_sprc

					join $sitsSchema.cam_ssn ssn -- SSN holds module registration status
						on sms.spr_code = ssn.ssn_sprc and ssn.ssn_ayrc = sms.ayr_code and ssn.ssn_mrgs != 'CON' -- module choices confirmed

					left join $sitsSchema.cam_mab mab -- Module Assessment Body, containing assessment components (needed for the sequences)
						on mab.map_code = sms.mod_code and mab.mab_agrp = sms.sms_agrp

					left join $sitsSchema.cam_wss wss -- WSS is "Slot Student"
						on wss.wss_sprc = spr.spr_code and wss.wss_ayrc = sms.ayr_code and wss.wss_modc = sms.mod_code
							and wss.wss_mabs = mab.mab_seq and $dialectRegexpLike(wss.wss_wspc, '^EX[A-Z]{3}[0-9]{2}$$')

					left join $sitsSchema.cam_sas sas -- Where component marks go
						on sas.spr_code = sms.spr_code and sas.ayr_code = sms.ayr_code and sas.mod_code = sms.mod_code
							and sas.mav_occur = sms.sms_occl and sas.mab_seq = mab.mab_seq

			where
				scj.scj_udfa in ('Y','y') and -- most significant courses only
				sms.ayr_code in (:academic_year_code)"""

	// this gets a student's assessments from the SMO table, which stores confirmed module choices
	def GetConfirmedModuleRegistrations = s"""
		select
			smo.ayr_code as academic_year_code,
			spr.spr_code as spr_code,
			wss.wss_seat as seat_number,
			smo.mav_occur as mav_occurrence, -- module occurrence (representing eg day or evening - usually 'A')
			smo.mod_code as module_code,
			smo.smo_agrp as assessment_group,
			mab.mab_seq as sequence,
			sas.sas_actm as actual_mark,
			sas.sas_actg as actual_grade,
			sas.sas_agrm as agreed_mark,
			sas.sas_agrg as agreed_grade
				from $sitsSchema.srs_scj scj
					join $sitsSchema.ins_spr spr
						on scj.scj_sprc = spr.spr_code and
							(spr.sts_code is null or spr.sts_code not like 'P%' or spr.sts_code != 'D') -- no perm withdrawn or deceased students

					join $sitsSchema.cam_smo smo
						on smo.spr_code = spr.spr_code and
							(smo.smo_rtsc is null or (smo.smo_rtsc not like 'X%' and smo.smo_rtsc != 'Z')) -- no WMG cancelled

					join $sitsSchema.cam_ssn ssn
						on smo.spr_code = ssn.ssn_sprc and ssn.ssn_ayrc = smo.ayr_code and ssn.ssn_mrgs = 'CON' -- confirmed module choices

					left join $sitsSchema.cam_mab mab -- Module Assessment Body, containing assessment components (needed for the sequences)
						on mab.map_code = smo.mod_code and mab.mab_agrp = smo.smo_agrp

					left join $sitsSchema.cam_wss wss -- WSS is "Slot Student"
						on wss.wss_sprc = spr.spr_code and wss.wss_ayrc = smo.ayr_code and wss.wss_modc = smo.mod_code
							and wss.wss_mabs = mab.mab_seq and $dialectRegexpLike(wss.wss_wspc, '^EX[A-Z]{3}[0-9]{2}$$')

					left join $sitsSchema.cam_sas sas -- Where component marks go
						on sas.spr_code = smo.spr_code and sas.ayr_code = smo.ayr_code and sas.mod_code = smo.mod_code
							and sas.mav_occur = smo.mav_occur and sas.mab_seq = mab.mab_seq

			where
				scj.scj_udfa in ('Y','y') and -- most significant courses only
				smo.ayr_code in (:academic_year_code)"""

	def GetAutoUploadedConfirmedModuleRegistrations = s"""
		select
			smo.ayr_code as academic_year_code,
			spr.spr_code as spr_code,
			wss.wss_seat as seat_number,
			smo.mav_occur as mav_occurrence,
			smo.mod_code as module_code,
			smo.smo_agrp as assessment_group,
			mab.mab_seq as sequence,
			sas.sas_actm as actual_mark,
			sas.sas_actg as actual_grade,
			sas.sas_agrm as agreed_mark,
			sas.sas_agrg as agreed_grade
				from $sitsSchema.srs_scj scj
					join $sitsSchema.ins_spr spr
						on scj.scj_sprc = spr.spr_code and
							(spr.sts_code is null or spr.sts_code not like 'P%' or spr.sts_code != 'D') -- no perm withdrawn or deceased students

					join $sitsSchema.cam_smo smo
						on smo.spr_code = spr.spr_code and
							(smo.smo_rtsc is null or (smo.smo_rtsc not like 'X%' and smo.smo_rtsc != 'Z')) -- no WMG cancelled

					left outer join $sitsSchema.cam_ssn ssn
						on smo.spr_code = ssn.ssn_sprc and ssn.ssn_ayrc = smo.ayr_code

					left join $sitsSchema.cam_mab mab -- Module Assessment Body, containing assessment components (needed for the sequences)
						on mab.map_code = smo.mod_code and mab.mab_agrp = smo.smo_agrp

					left join $sitsSchema.cam_wss wss -- WSS is "Slot Student"
						on wss.wss_sprc = spr.spr_code and wss.wss_ayrc = smo.ayr_code and wss.wss_modc = smo.mod_code
							and wss.wss_mabs = mab.mab_seq and $dialectRegexpLike(wss.wss_wspc, '^EX[A-Z]{3}[0-9]{2}$$')

					left join $sitsSchema.cam_sas sas -- Where component marks go
						on sas.spr_code = smo.spr_code and sas.ayr_code = smo.ayr_code and sas.mod_code = smo.mod_code
							and sas.mav_occur = smo.mav_occur and sas.mab_seq = mab.mab_seq

			where
				scj.scj_udfa in ('Y','y') and -- most significant courses only
				smo.ayr_code in (:academic_year_code) and
				ssn.ssn_sprc is null -- no matching SSN"""

	def GetAllAssessmentGroupMembers = s"""
			$GetUnconfirmedModuleRegistrations
				union all
			$GetConfirmedModuleRegistrations
				union all
			$GetAutoUploadedConfirmedModuleRegistrations
		order by academic_year_code, module_code, mav_occurrence, assessment_group, sequence, spr_code"""

	def GetAllGradeBoundaries = s"""
		select
			mkc.mks_code as marks_code,
			mkc.mkc_grade as grade,
			mkc.mkc_minm as minimum_mark,
			mkc.mkc_maxm as maximum_mark,
			mkc.mkc_sigs as signal_status
		from $sitsSchema.cam_mkc mkc
		where mkc_proc = 'SAS' and mkc_minm is not null and mkc_maxm is not null
	"""

	class AssessmentComponentQuery(ds: DataSource) extends MappingSqlQuery[AssessmentComponent](ds, GetAssessmentsQuery) {
		declareParameter(new SqlParameter("academic_year_code", Types.VARCHAR))
		compile()
		override def mapRow(rs: ResultSet, rowNumber: Int): AssessmentComponent = {
			val a = new AssessmentComponent
			a.moduleCode = rs.getString("module_code")
			a.sequence = rs.getString("seq")
			a.name = rs.getString("name")
			a.assessmentGroup = rs.getString("assessment_group")
			a.assessmentType = AssessmentType(rs.getString("assessment_code"))
			a.inUse = rs.getString("in_use") match {
				case "Y" | "y" => true
				case _ => false
			}
			a.marksCode = rs.getString("marks_code")
			a
		}
	}

	class UpstreamAssessmentGroupQuery(ds: DataSource) extends MappingSqlQueryWithParameters[UpstreamAssessmentGroup](ds, GetAllAssessmentGroups) {
		declareParameter(new SqlParameter("academic_year_code", Types.VARCHAR))
		this.compile()
		override def mapRow(rs: ResultSet, rowNumber: Int, params: Array[java.lang.Object], context: JMap[_, _]): UpstreamAssessmentGroup =
			mapRowToAssessmentGroup(rs)
	}

	def mapRowToAssessmentGroup(rs: ResultSet): UpstreamAssessmentGroup = {
		val ag = new UpstreamAssessmentGroup()
		ag.moduleCode = rs.getString("module_code")
		ag.academicYear = AcademicYear.parse(rs.getString("academic_year_code"))
		ag.assessmentGroup = rs.getString("assessment_group")
		ag.occurrence = rs.getString("mav_occurrence")
		ag.sequence = rs.getString("seq")
		ag
	}

	class GradeBoundaryQuery(ds: DataSource) extends MappingSqlQuery[GradeBoundary](ds, GetAllGradeBoundaries) {
		compile()
		override def mapRow(rs: ResultSet, rowNumber: Int): GradeBoundary = {
			GradeBoundary(
				rs.getString("marks_code"),
				rs.getString("grade"),
				rs.getInt("minimum_mark"),
				rs.getInt("maximum_mark"),
				rs.getString("signal_status")
			)
		}
	}

}