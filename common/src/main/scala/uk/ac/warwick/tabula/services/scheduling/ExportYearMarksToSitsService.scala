package uk.ac.warwick.tabula.services.scheduling

import java.sql.Types
import javax.sql.DataSource

import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.SqlUpdate
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports.{JHashMap, _}
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails
import uk.ac.warwick.tabula.services.scheduling.ExportYearMarksToSitsService.ExportYearMarksToSitsUpdateQuery

trait ExportYearMarksToSitsServiceComponent {
	def exportYearMarksToSitsService: ExportYearMarksToSitsService
}

trait AutowiringExportYearMarksToSitsServiceComponent extends ExportYearMarksToSitsServiceComponent {
	var exportYearMarksToSitsService: ExportYearMarksToSitsService = Wire[ExportYearMarksToSitsService]
}

trait ExportYearMarksToSitsService {
	def exportToSits(scyd: StudentCourseYearDetails): Boolean
}

class AbstractExportYearMarksToSitsService extends ExportYearMarksToSitsService {

	self: SitsDataSourceComponent =>
	def exportToSits(scyd: StudentCourseYearDetails): Boolean = {
		val parameterMap: JMap[String, String] = JHashMap(
			("mark", scyd.agreedMark.toString),
			("scjcode", scyd.studentCourseDetails.scjCode),
			("academicyear", scyd.academicYear.toString),
			("yearofstudy", scyd.yearOfStudy.toString),
			("scesequence", scyd.sceSequenceNumberSitsFormat)
		)

		val updateQuery = new ExportYearMarksToSitsUpdateQuery(sitsDataSource)
		updateQuery.updateByNamedParam(parameterMap) == 1
	}
}

object ExportYearMarksToSitsService {
	val sitsSchema: String = Wire.property("${schema.sits}")

	// insert into Student Course Enrolment (SCE) table
	final def PushToSITSSql = f"""
		update $sitsSchema.SRS_SCE
		set SCE_UDFJ = :mark
		where SCE_SCJC = :scjcode and SCE_AYRC = :academicyear and SCE_BLOK = :yearofstudy and SCE_SEQ2 = :scesequence
	"""

	class ExportYearMarksToSitsUpdateQuery(ds: DataSource) extends SqlUpdate(ds, PushToSITSSql) {

		declareParameter(new SqlParameter("mark", Types.VARCHAR))
		declareParameter(new SqlParameter("scjcode", Types.VARCHAR))
		declareParameter(new SqlParameter("academicyear", Types.VARCHAR))
		declareParameter(new SqlParameter("yearofstudy", Types.VARCHAR))
		declareParameter(new SqlParameter("scesequence", Types.VARCHAR))
		compile()

	}
}

@Profile(Array("dev", "test", "production"))
@Service
class ExportYearMarksToSitsServiceImpl
	extends AbstractExportYearMarksToSitsService with AutowiringSitsDataSourceComponent

@Profile(Array("sandbox"))
@Service
class ExportYearMarksToSitsSandboxService extends ExportYearMarksToSitsService {
	def exportToSits(scyd: StudentCourseYearDetails) = false
}