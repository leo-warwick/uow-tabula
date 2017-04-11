package uk.ac.warwick.tabula.services.scheduling

import java.sql.Types
import java.util
import javax.sql.DataSource

import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.SqlUpdate
import org.springframework.jdbc.core.SqlParameter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports.{JHashMap, _}
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointReport
import uk.ac.warwick.tabula.services.scheduling.ExportAttendanceToSitsService.{ExportAttendanceToSitsUpdateQuery, ExportAttendanceToSitsCountQuery}

trait ExportAttendanceToSitsServiceComponent {
	def exportAttendanceToSitsService: ExportAttendanceToSitsService
}

trait AutowiringExportAttendanceToSitsServiceComponent extends ExportAttendanceToSitsServiceComponent {
	var exportAttendanceToSitsService: ExportAttendanceToSitsService = Wire[ExportAttendanceToSitsService]
}

trait ExportAttendanceToSitsService {
	def exportToSits(report: MonitoringPointReport): Boolean
}

class AbstractExportAttendanceToSitsService extends ExportAttendanceToSitsService {

	self: SitsDataSourceComponent =>
	def exportToSits(report: MonitoringPointReport): Boolean = {
		val countQuery = new ExportAttendanceToSitsCountQuery(sitsDataSource)
		val count = countQuery.getHighestCount(JHashMap(("studentId", report.student.id))) + 1

		val monitoringPeriod = {
			if (report.monitoringPeriod.indexOf("vacation") >= 0)
				s"${report.monitoringPeriod} ${report.academicYear.startYear}/${report.academicYear.endYear.toString.substring(2)}"
			else
				s"${report.monitoringPeriod} term ${report.academicYear.startYear}/${report.academicYear.endYear.toString.substring(2)}"
		}
		val parameterMap = JHashMap(
			("studentId", report.student.id),
			("counter", "%03d".format(count)),
			("now", DateTime.now.toDate),
			("academicYear", report.academicYear.toString),
			("deptCode", Option(report.studentCourseYearDetails).flatMap { scyd => Option(scyd.enrolmentDepartment) }.fold("") { _.code }.toUpperCase),
			("courseCode", Option(report.studentCourseDetails).flatMap { scd => Option(scd.course) }.fold("") { _.code }),
			("recorder", report.reporter),
			("missedPoints", report.missed),
			("monitoringPeriod", monitoringPeriod)
		)

		val updateQuery = new ExportAttendanceToSitsUpdateQuery(sitsDataSource)
		updateQuery.updateByNamedParam(parameterMap) == 1
	}
}

object ExportAttendanceToSitsService {
	val sitsSchema: String = Wire.property("${schema.sits}")

	// find the latest row in the Student Absence (SAB) table in SITS for the student
	final def GetHighestExistingSequence = f"""
		select max(sab_seq2) from $sitsSchema.srs_sab
		where sab_stuc = :studentId
	"""

	// insert into Student Absence (SAB) table
	final def PushToSITSSql = f"""
		insert into $sitsSchema.srs_sab
		(SAB_STUC,SAB_SEQ2,SAB_RAAC,SAB_ENDD,SAB_AYRC,SAB_UDF2,SAB_UDF3,SAB_UDF4,SAB_UDF5,SAB_UDF9,SAB_UDFJ)
		values (:studentId, :counter, 'UNAUTH', :now, :academicYear, :deptCode, :courseCode, :recorder, :missedPoints, 'Tabula', :monitoringPeriod )
	"""

	class ExportAttendanceToSitsCountQuery(ds: DataSource) extends NamedParameterJdbcTemplate(ds) {
		def getHighestCount(params: util.HashMap[String, Object]): Int = {
			this.queryForObject(GetHighestExistingSequence, params, classOf[JInteger]).asInstanceOf[Int]
		}
	}

	class ExportAttendanceToSitsUpdateQuery(ds: DataSource) extends SqlUpdate(ds, PushToSITSSql) {

		declareParameter(new SqlParameter("studentId", Types.VARCHAR))
		declareParameter(new SqlParameter("counter", Types.VARCHAR))
		declareParameter(new SqlParameter("now", Types.DATE))
		declareParameter(new SqlParameter("academicYear", Types.VARCHAR))
		declareParameter(new SqlParameter("deptCode", Types.VARCHAR))
		declareParameter(new SqlParameter("courseCode", Types.VARCHAR))
		declareParameter(new SqlParameter("recorder", Types.VARCHAR))
		declareParameter(new SqlParameter("missedPoints", Types.VARCHAR))
		declareParameter(new SqlParameter("monitoringPeriod", Types.VARCHAR))
		compile()

	}
}

@Profile(Array("dev", "test", "production"))
@Service
class ExportAttendanceToSitsServiceImpl
	extends AbstractExportAttendanceToSitsService with AutowiringSitsDataSourceComponent

@Profile(Array("sandbox"))
@Service
class ExportAttendanceToSitsSandboxService extends ExportAttendanceToSitsService {
	def exportToSits(report: MonitoringPointReport) = false
}


trait SitsDataSourceComponent {
	def sitsDataSource: DataSource
}

trait AutowiringSitsDataSourceComponent extends SitsDataSourceComponent {
	var sitsDataSource: DataSource = Wire[DataSource]("sitsDataSource")
}