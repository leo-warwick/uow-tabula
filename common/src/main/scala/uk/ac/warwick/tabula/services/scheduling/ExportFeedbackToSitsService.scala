package uk.ac.warwick.tabula.services.scheduling

import java.sql.{ResultSet, Types}
import java.util
import javax.sql.DataSource

import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.{MappingSqlQueryWithParameters, SqlUpdate}
import org.springframework.jdbc.core.SqlParameter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.scheduling.imports.ImportMemberHelpers
import uk.ac.warwick.tabula.data.model.{AssessmentGroup, FeedbackForSits}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.scheduling.ExportFeedbackToSitsService.{CountQuery, ExportFeedbackToSitsQuery, PartialMatchQuery, SasRow}

import scala.collection.JavaConverters._
import scala.collection.mutable

trait ExportFeedbackToSitsServiceComponent {
	def exportFeedbackToSitsService: ExportFeedbackToSitsService
}

trait AutowiringExportFeedbackToSitsServiceComponent extends ExportFeedbackToSitsServiceComponent {
	var exportFeedbackToSitsService: ExportFeedbackToSitsService = Wire[ExportFeedbackToSitsService]
}

trait ExportFeedbackToSitsService {
	def countMatchingSasRecords(feedbackForSits: FeedbackForSits): Integer
	def exportToSits(feedbackToLoad: FeedbackForSits): Integer
	def getPartialMatchingSasRecords(feedbackForSits: FeedbackForSits): Seq[ExportFeedbackToSitsService.SasRow]
}

class ParameterGetter(feedbackForSits: FeedbackForSits) {
	val assessGroups: mutable.Buffer[AssessmentGroup] = feedbackForSits.feedback.assessmentGroups.asScala
	val possibleOccurrenceSequencePairs: mutable.Buffer[(String, String)] = assessGroups.map(assessGroup => (assessGroup.occurrence, assessGroup.assessmentComponent.sequence))

	def getQueryParams: Option[util.HashMap[String, Object]] = possibleOccurrenceSequencePairs match {
		case pairs if pairs.isEmpty => None
		case _ => Option(JHashMap(
			// for the where clause
			("studentId", feedbackForSits.feedback.studentIdentifier),
			("academicYear", feedbackForSits.feedback.academicYear.toString),
			("moduleCodeMatcher", feedbackForSits.feedback.module.code.toUpperCase + "%"),
			("now", DateTime.now.toDate),

			// in theory we should look for a record with occurrence and sequence from the same pair,
			// but in practice there won't be any ambiguity since the record is already determined
			// by student, module code and year
			("occurrences", possibleOccurrenceSequencePairs.map(_._1).asJava),
			("sequences", possibleOccurrenceSequencePairs.map(_._2).asJava)
		))
	}

	def getUpdateParams(mark: Integer, grade: String): Option[util.HashMap[String, Object]] = possibleOccurrenceSequencePairs match {
		case pairs if pairs.isEmpty => None
		case _ => Option(JHashMap(
			// for the where clause
			("studentId", feedbackForSits.feedback.studentIdentifier),
			("academicYear", feedbackForSits.feedback.academicYear.toString),
			("moduleCodeMatcher", feedbackForSits.feedback.module.code.toUpperCase + "%"),
			("now", DateTime.now.toDate),

			// in theory we should look for a record with occurrence and sequence from the same pair,
			// but in practice there won't be any ambiguity since the record is already determined
			// by student, module code and year
			("occurrences", possibleOccurrenceSequencePairs.map(_._1).asJava),
			("sequences", possibleOccurrenceSequencePairs.map(_._2).asJava),

			// data to insert
			("actualMark", mark),
			("actualGrade", grade)
		))
	}

}


class AbstractExportFeedbackToSitsService extends ExportFeedbackToSitsService with Logging {

	self: SitsDataSourceComponent =>

	def countMatchingSasRecords(feedbackForSits: FeedbackForSits): Integer = {
		val countQuery = new CountQuery(sitsDataSource)
		val parameterGetter: ParameterGetter = new ParameterGetter(feedbackForSits)
		parameterGetter.getQueryParams match {
			case Some(params) =>
				countQuery.getCount(params)
			case None =>
				logger.warn(s"Cannot upload feedback ${feedbackForSits.feedback.id} for SITS as no assessment groups found")
				0
		}
	}

	def exportToSits(feedbackForSits: FeedbackForSits): Integer = {
		val parameterGetter: ParameterGetter = new ParameterGetter(feedbackForSits)
		val updateQuery = new ExportFeedbackToSitsQuery(sitsDataSource)

		val grade = feedbackForSits.feedback.latestGrade
		val mark = feedbackForSits.feedback.latestMark
		val numRowsChanged = {
			if (grade.isDefined && mark.isDefined) {
				parameterGetter.getUpdateParams(mark.get, grade.get) match {
					case Some(params) =>
						updateQuery.updateByNamedParam(params)
					case None =>
						logger.warn(s"Cannot upload feedback ${feedbackForSits.feedback.id} for SITS as no assessment groups found")
						0
				}
			} else {
				logger.warn(f"Not updating SITS CAM_SAS for feedback ${feedbackForSits.feedback.id} - no latest mark or grade found")
				0 // issue a warning when the FeedbackForSits record is created, not here
			}
		}
		numRowsChanged
	}

	def getPartialMatchingSasRecords(feedbackForSits: FeedbackForSits): Seq[ExportFeedbackToSitsService.SasRow] = {
		val parameterGetter: ParameterGetter = new ParameterGetter(feedbackForSits)
		parameterGetter.getQueryParams match {
			case Some(params) =>
				val query = new PartialMatchQuery(sitsDataSource)
				query.executeByNamedParam(params.asScala.filterKeys(_ != "now").asJava).asScala
			case None =>
				logger.warn(s"Cannot get partial matching SAS records for feedback ${feedbackForSits.feedback.id} as no assessment groups found")
				Nil
		}
	}
}

object ExportFeedbackToSitsService {
	val sitsSchema: String = Wire.property("${schema.sits}")
	val tabulaIdentifier = "Tabula"

	// match on the Student Programme Route (SPR) code for the student
	// mav_occur = module occurrence code
	// psl_code = "Period Slot"
	// mab_seq = sequence code determining an assessment component
	def whereClause = f"""where spr_code in (select spr_code from $sitsSchema.ins_spr where spr_stuc = :studentId)
		and mod_code like :moduleCodeMatcher
		and mav_occur in (:occurrences)
		and ayr_code = :academicYear
		and psl_code = 'Y'
		and mab_seq in (:sequences)
	"""

	// Only upload when the mark/grade is empty or was previously uploaded by Tabula
	def writeableWhereClause = f"""$whereClause
		and (
			sas_actm is null and sas_actg is null
			or sas_udf1 = '$tabulaIdentifier'
		)
	"""

	final def CountMatchingBlankSasRecordsSql = f"""
		select count(*) from $sitsSchema.cam_sas $writeableWhereClause
	"""

	class CountQuery(ds: DataSource) extends NamedParameterJdbcTemplate(ds) {

		def getCount(params: util.HashMap[String, Object]): Int = {
			this.queryForObject(CountMatchingBlankSasRecordsSql, params, classOf[JInteger]).asInstanceOf[Int]
		}
	}

	// update Student Assessment table (CAM_SAS) which holds module component marks
	// SAS_PRCS = Process Status - Value of I enables overall marks to be calculated in SITS
	// SAS_PROC = Current Process - Value of SAS enabled overall marks to be calculated in SITS
	// SAS_UDF1, SAS_UDF2 - user defined fields used for audit
	final def UpdateSITSFeedbackSql = f"""
		update $sitsSchema.cam_sas
		set sas_actm = :actualMark,
			sas_actg = :actualGrade,
			sas_prcs = 'I',
			sas_proc = 'SAS',
			sas_udf1 = '$tabulaIdentifier',
			sas_udf2 = :now
		$writeableWhereClause
	"""

	class ExportFeedbackToSitsQuery(ds: DataSource) extends SqlUpdate(ds, UpdateSITSFeedbackSql) {
		declareParameter(new SqlParameter("actualMark", Types.INTEGER))
		declareParameter(new SqlParameter("actualGrade", Types.VARCHAR))
		declareParameter(new SqlParameter("studentId", Types.VARCHAR))
		declareParameter(new SqlParameter("academicYear", Types.VARCHAR))
		declareParameter(new SqlParameter("moduleCodeMatcher", Types.VARCHAR))
		declareParameter(new SqlParameter("now", Types.DATE))
		declareParameter(new SqlParameter("occurrences", Types.VARCHAR))
		declareParameter(new SqlParameter("sequences", Types.VARCHAR))

		compile()

	}

	final def PartialMatchingSasRecordsSql = f"""
		select sas_actm, sas_actg, sas_udf1 from $sitsSchema.cam_sas $whereClause
	"""

	class PartialMatchQuery(ds: DataSource) extends MappingSqlQueryWithParameters[SasRow](ds, PartialMatchingSasRecordsSql) {
		declareParameter(new SqlParameter("studentId", Types.VARCHAR))
		declareParameter(new SqlParameter("academicYear", Types.VARCHAR))
		declareParameter(new SqlParameter("moduleCodeMatcher", Types.VARCHAR))
		declareParameter(new SqlParameter("occurrences", Types.VARCHAR))
		declareParameter(new SqlParameter("sequences", Types.VARCHAR))

		override def mapRow(rs: ResultSet, rowNum: Int, parameters: Array[AnyRef], context: JMap[_, _]): SasRow = {
			SasRow(
				ImportMemberHelpers.getInteger(rs, "sas_actm"),
				rs.getString("sas_actg"),
				rs.getString("sas_udf1")
			)
		}
	}

	case class SasRow(
		actualMark: Option[Int],
		actualGrade: String,
		uploader: String
	)
}

@Profile(Array("dev", "test", "production"))
@Service
class ExportFeedbackToSitsServiceImpl
	extends AbstractExportFeedbackToSitsService with AutowiringSitsDataSourceComponent

@Profile(Array("sandbox"))
@Service
class ExportFeedbackToSitsSandboxService extends ExportFeedbackToSitsService {
	def countMatchingSasRecords(feedbackForSits: FeedbackForSits) = 0
	def exportToSits(feedbackForSits: FeedbackForSits) = 0
	def getPartialMatchingSasRecords(feedbackForSits: FeedbackForSits): Seq[SasRow] = Nil
}
