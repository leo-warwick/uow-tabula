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
import uk.ac.warwick.tabula.data.model.RecordedDecision
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.scheduling.ExportDecisionsToSitsService._
import uk.ac.warwick.tabula.JavaImports._

import scala.util.Try

trait ExportDecisionsToSitsServiceComponent {
  def exportDecisionsToSitsService: ExportDecisionsToSitsService
}

trait AutowiringExportDecisionsToSitsServiceComponent extends ExportDecisionsToSitsServiceComponent {
  var exportDecisionsToSitsService: ExportDecisionsToSitsService = Wire[ExportDecisionsToSitsService]
}

trait ExportDecisionsToSitsService {
  def updateDecision(resit: RecordedDecision): Int
}

object ExportDecisionsToSitsService {
  val sitsSchema: String = Wire.property("${schema.sits}")


  final def UpdateDecisionRecordSql: String =
    s"""
       |update $sitsSchema.cam_spi
       |set
       |  spi_note = :note,
       |  spi_mint = :minutes,
       |  spi_pitc = :decision
       |where
       |  spi_sprc = :sprCode
       |  and spi_payr = :academicYear
       |  and spi_seq2 = :sequence
       |
       |""".stripMargin

  class UpdateDecisionRecordQuery(ds: DataSource) extends SqlUpdate(ds, UpdateDecisionRecordSql) {
    declareParameter(new SqlParameter("sprCode", Types.VARCHAR))
    declareParameter(new SqlParameter("academicYear", Types.VARCHAR))
    declareParameter(new SqlParameter("sequence", Types.VARCHAR))
    declareParameter(new SqlParameter("note", Types.VARCHAR))
    declareParameter(new SqlParameter("minutes", Types.VARCHAR))
    declareParameter(new SqlParameter("decision", Types.VARCHAR))
    compile()
  }


}

class RecordedDecisionsParameterGetter(decision: RecordedDecision) {

  def updateParams: JMap[String, Any] = JHashMap(
    "sprCode" -> decision.sprCode,
    "academicYear" -> decision.academicYear.toString,
    "sequence" -> decision.sequence,
    "note" -> decision.notes,
    "minutes" -> decision.minutes,
    "decision" -> decision.decision.pitCode
  )
}

class AbstractExportDecisionsToSitsService extends ExportDecisionsToSitsService with Logging {
  self: SitsDataSourceComponent =>


  def updateDecision(decision: RecordedDecision): Int = {
    val parameterGetter = new RecordedDecisionsParameterGetter(decision)
    val updateQuery = new UpdateDecisionRecordQuery(sitsDataSource)
    updateQuery.updateByNamedParam(parameterGetter.updateParams)
  }

}

@Profile(Array("dev", "test", "production"))
@Service
class ExportDecisionsToSitsServiceImpl
  extends AbstractExportDecisionsToSitsService with AutowiringSitsDataSourceComponent

@Profile(Array("sandbox"))
@Service
class ExportDecisionsToSitsSandboxService extends ExportDecisionsToSitsService {
  def updateDecision(resit: RecordedDecision): Int = 0
}
