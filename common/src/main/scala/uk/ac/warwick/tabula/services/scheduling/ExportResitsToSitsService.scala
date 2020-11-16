package uk.ac.warwick.tabula.services.scheduling

import java.sql.Types

import javax.sql.DataSource
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.SqlUpdate
import org.springframework.jdbc.core.SqlParameter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports.JMap
import uk.ac.warwick.tabula.data.model.RecordedResit
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.scheduling.ExportResitsToSitsService._
import uk.ac.warwick.tabula.JavaImports._

import scala.util.Try

trait ExportResitsToSitsServiceComponent {
  def exportResitsToSitsService: ExportResitsToSitsService
}

trait AutowiringExportResitsToSitsServiceComponent extends ExportResitsToSitsServiceComponent {
  var exportResitsToSitsService: ExportResitsToSitsService = Wire[ExportResitsToSitsService]
}

trait ExportResitsToSitsService {
  def createResit(resit: RecordedResit, rseq: String): Int
  def updateResit(resit: RecordedResit): Int
  def getNextResitSequence(resit: RecordedResit): String
}

object ExportResitsToSitsService {
  val sitsSchema: String = Wire.property("${schema.sits}")

  final def CreateResitRecordSql: String =
    s"""
       |insert into $sitsSchema.cam_sra
       |(
       |  spr_code,
       |  ayr_code,
       |  psl_code,
       |  mod_code,
       |  mav_occur,
       |  sra_seq,
       |  sra_rseq,
       |  mks_code,
       |  ast_code,
       |  sra_cura,
       |  sra_perc,
       |  sra_proc
       |) values (
       |  :sprCode,
       |  :academicYear,
       |  'Y',
       |  :moduleCode,
       |  :occurrence,
       |  :sequence,
       |  :resitSequence,
       |  :marksCode,
       |  :assessmentType,
       |  :attempt,
       |  :weighting,
       |  'RAS'
       |)
       |""".stripMargin

  class CreateResitRecordQuery(ds: DataSource) extends SqlUpdate(ds, CreateResitRecordSql) {
    declareParameter(new SqlParameter("sprCode", Types.VARCHAR))
    declareParameter(new SqlParameter("academicYear", Types.VARCHAR))
    declareParameter(new SqlParameter("moduleCode", Types.VARCHAR))
    declareParameter(new SqlParameter("occurrence", Types.VARCHAR))
    declareParameter(new SqlParameter("sequence", Types.VARCHAR))
    declareParameter(new SqlParameter("resitSequence", Types.VARCHAR))
    declareParameter(new SqlParameter("marksCode", Types.VARCHAR))
    declareParameter(new SqlParameter("assessmentType", Types.VARCHAR))
    declareParameter(new SqlParameter("attempt", Types.VARCHAR))
    declareParameter(new SqlParameter("weighting", Types.VARCHAR))
    compile()
  }

  final def UpdateResitRecordSql: String =
    s"""
       |update $sitsSchema.cam_sra
       |set
       |  ast_code = :assessmentType,
       |  sra_cura = :attempt,
       |  sra_perc = :weighting
       |where
       |  spr_code = :sprCode
       |  and ayr_code = :academicYear
       |  and mod_code = :moduleCode
       |  and sra_rseq = :resitSequence
       |
       |""".stripMargin

  class UpdateResitRecordQuery(ds: DataSource) extends SqlUpdate(ds, UpdateResitRecordSql) {
    declareParameter(new SqlParameter("sprCode", Types.VARCHAR))
    declareParameter(new SqlParameter("academicYear", Types.VARCHAR))
    declareParameter(new SqlParameter("moduleCode", Types.VARCHAR))
    declareParameter(new SqlParameter("resitSequence", Types.VARCHAR))
    declareParameter(new SqlParameter("assessmentType", Types.VARCHAR))
    declareParameter(new SqlParameter("attempt", Types.VARCHAR))
    declareParameter(new SqlParameter("weighting", Types.VARCHAR))
    compile()
  }


  final def CurrentSraSequenceSql =
    f"""
    select sra_rseq from $sitsSchema.cam_sra
      where spr_code = :sprCode
      and mod_code like :moduleCode
      and ayr_code = :academicYear
      order by sra_rseq desc
      fetch first 1 row only
    """

  class CurrentSraSequenceQuery(ds: DataSource) extends NamedParameterJdbcTemplate(ds) {
    def getCurrentSraSequence(params: JMap[String, Any]): Option[String] =
      Try(this.queryForObject(CurrentSraSequenceSql, params, classOf[String])).toOption
  }


}

class RecordedResitsParameterGetter(resit: RecordedResit) {
  def queryParams: JMap[String, Any] = JHashMap(
    "sprCode" -> resit.sprCode,
    "academicYear" -> resit.academicYear.toString,
    "moduleCode" -> resit.moduleCode,
  )

  def createParams(rseq: String): JMap[String, Any] = JHashMap(
    "sprCode" -> resit.sprCode,
    "academicYear" -> resit.academicYear.toString,
    "moduleCode" -> resit.moduleCode,
    "occurrence" -> resit.occurrence,
    "sequence" -> resit.sequence,
    "resitSequence" -> rseq,
    "marksCode" -> resit.marksCode,
    "assessmentType" -> resit.assessmentType.astCode,
    "attempt" -> resit.currentResitAttempt.value.toString,
    "weighting" -> resit.weighting.toString,
  )

  def updateParams: JMap[String, Any] = JHashMap(
    "sprCode" -> resit.sprCode,
    "academicYear" -> resit.academicYear.toString,
    "moduleCode" -> resit.moduleCode,
    "resitSequence" -> resit.resitSequence.orNull,
    "assessmentType" -> resit.assessmentType.astCode,
    "attempt" -> resit.currentResitAttempt.value.toString,
    "weighting" -> resit.weighting.toString,
  )
}

class AbstractExportResitsToSitsService extends ExportResitsToSitsService with Logging {
  self: SitsDataSourceComponent =>

  def createResit(resit: RecordedResit, rseq: String): Int = {
    val parameterGetter = new RecordedResitsParameterGetter(resit)
    val createQuery = new CreateResitRecordQuery(sitsDataSource)
    createQuery.updateByNamedParam(parameterGetter.createParams(rseq))
  }

  def updateResit(resit: RecordedResit): Int = {
    val parameterGetter = new RecordedResitsParameterGetter(resit)
    val updateQuery = new UpdateResitRecordQuery(sitsDataSource)
    updateQuery.updateByNamedParam(parameterGetter.updateParams)
  }

  override def getNextResitSequence(resit: RecordedResit): String = {
    val parameterGetter = new RecordedResitsParameterGetter(resit)
    val countQuery = new CurrentSraSequenceQuery(sitsDataSource)
    val highestResitSequence = countQuery.getCurrentSraSequence(parameterGetter.queryParams)
      .map(_.replaceAll("[^0-9]", "")) // strip out any characters
      .flatMap(s => s.toIntOption)
      .maxByOption(identity)
      .getOrElse(0)
    f"${highestResitSequence + 1 }%03d" // 3 chars padded with leading zeros
  }
}

@Profile(Array("dev", "test", "production"))
@Service
class ExportResitsToSitsServiceImpl
  extends AbstractExportResitsToSitsService with AutowiringSitsDataSourceComponent

@Profile(Array("sandbox"))
@Service
class ExportResitsToSitsSandboxService extends ExportResitsToSitsService {
  def createResit(resit: RecordedResit, rseq: String): Int = 0
  def updateResit(resit: RecordedResit): Int = 0
  def getNextResitSequence(resit: RecordedResit): String = "001"
}
