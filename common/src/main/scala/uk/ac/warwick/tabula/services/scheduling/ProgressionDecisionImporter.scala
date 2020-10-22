package uk.ac.warwick.tabula.services.scheduling

import java.sql.{ResultSet, Types}

import javax.sql.DataSource
import org.apache.commons.lang3.builder.{EqualsBuilder, HashCodeBuilder}
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports.{JList, JMap}
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model.{DegreeType, ProgressionDecision, ProgressionDecisionOutcome, ProgressionDecisionProcessStatus}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.sandbox.SandboxData
import uk.ac.warwick.tabula.services.scheduling.ProgressionDecisionImporter.{ProgressionDecisionsByAcademicYearsQuery, ProgressionDecisionsByUniversityIdsQuery}
import uk.ac.warwick.tabula.{AcademicYear, AutowiringFeaturesComponent, Features, ToString}

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

/**
 * Import progression decisions from SITS.
 *
 */
trait ProgressionDecisionImporter {
  def getProgressionDecisionRowsForAcademicYears(academicYears: Seq[AcademicYear]): Seq[ProgressionDecisionRow]
  def getProgressionDecisionRowsForUniversityIds(universityIds: Seq[String]): Seq[ProgressionDecisionRow]
}

@Profile(Array("dev", "test", "production"))
@Service
class ProgressionDecisionImporterImpl extends ProgressionDecisionImporter with AutowiringSitsDataSourceComponent with TaskBenchmarking with AutowiringFeaturesComponent {
  lazy val progressionDecisionsByAcademicYearsQuery: ProgressionDecisionsByAcademicYearsQuery = new ProgressionDecisionsByAcademicYearsQuery(sitsDataSource)
  lazy val progressionDecisionsByUniversityIdsQuery: ProgressionDecisionsByUniversityIdsQuery = new ProgressionDecisionsByUniversityIdsQuery(sitsDataSource)

  private def yearsToImportArray(yearsToImport: Seq[AcademicYear]): JList[String] = yearsToImport.map(_.toString).asJava: JList[String]
  def getProgressionDecisionRowsForAcademicYears(academicYears: Seq[AcademicYear]): Seq[ProgressionDecisionRow] =
    benchmarkTask(s"Fetch progression decisions for $academicYears") {
      progressionDecisionsByAcademicYearsQuery.executeByNamedParam(JMap("academicYears" -> yearsToImportArray(academicYears))).asScala.distinct.toSeq
    }

  def getProgressionDecisionRowsForUniversityIds(universityIds: Seq[String]): Seq[ProgressionDecisionRow] =
    benchmarkTask(s"Fetch module registrations for $universityIds") {
      universityIds.grouped(Daoisms.MaxInClauseCountOracle).flatMap { ids =>
        progressionDecisionsByUniversityIdsQuery.executeByNamedParam(JMap("universityIds" -> ids.asJava)).asScala.distinct.toSeq
      }.toSeq
    }
}

@Profile(Array("sandbox"))
@Service
class SandboxProgressionDecisionImporter extends ProgressionDecisionImporter {

  override def getProgressionDecisionRowsForAcademicYears(academicYears: Seq[AcademicYear]): Seq[ProgressionDecisionRow] =
    SandboxData.Departments.values
      .flatMap(_.routes.values)
      .flatMap(route => route.studentsStartId to route.studentsEndId)
      .flatMap(universityId => studentProgressionDecisionRows(universityId.toString))
      .filter(pd => academicYears.contains(pd.academicYear))
      .toSeq

  override def getProgressionDecisionRowsForUniversityIds(universityIds: Seq[String]): Seq[ProgressionDecisionRow] =
    universityIds.flatMap(studentProgressionDecisionRows)

  def studentProgressionDecisionRows(universityId: String): Seq[ProgressionDecisionRow] = {
    SandboxData.Departments.flatMap(_._2.routes.values)
      .find(route => route.degreeType == DegreeType.Undergraduate && (route.studentsStartId to route.studentsEndId).contains(universityId.toInt))
      .map { _ =>
        val yearOfStudy = ((universityId.toLong % 3) + 1).toInt
        (1 until yearOfStudy).reverse.map(AcademicYear.now() - _).zipWithIndex.map { case (academicYear, index) =>
          new ProgressionDecisionRow(
            sprCode = "%s/1".format(universityId),
            sequence = "%03d".format(index + 1),
            resitPeriod = false,
            academicYear = academicYear,
            outcome = Some(ProgressionDecisionOutcome.UndergraduateProceedHonours),
            notes = None,
            minutes = None,
            status = ProgressionDecisionProcessStatus.Complete.code
          )
        }
      }
      .getOrElse(Nil)
  }
}

object ProgressionDecisionImporter extends Logging {
  val sitsSchema: String = Wire.property("${schema.sits}")

  var features: Features = Wire[Features]

  final def BaseSql: String =
    s"""
      |select
      |   spi.spi_sprc as spr_code,
      |   spi.spi_seq2 as sequence,
      |   spi.spi_pslc as scope_period,         -- 'S' for September resit period
      |   spi.spi_payr as academic_year_period, -- Period of study this applies to, so for resits it would be the previous year
      |   spi.spi_pit1 as agreed_outcome,
      |   spi.spi_note as notes,
      |   spi.spi_mint as minutes,
      |   spi.spi_prcs as status
      |from $sitsSchema.cam_spi spi
      |""".stripMargin

  final def ProgressionDecisionsForAcademicYearSql =
    s"$BaseSql where spi.spi_payr in (:academicYears)"

  final def ProgressionDecisionsForUniversityIdsSql =
    s"$BaseSql where SUBSTR(spi.spi_sprc, 0, 7) in (:universityIds)"

  def mapResultSet(rs: ResultSet): ProgressionDecisionRow = {
    val sprCode = rs.getString("spr_code")
    val sequence = rs.getString("sequence")
    new ProgressionDecisionRow(
      sprCode = sprCode,
      sequence = sequence,
      resitPeriod = rs.getString("scope_period").maybeText.contains("S"),
      academicYear = AcademicYear.parse(rs.getString("academic_year_period")),
      outcome = rs.getString("agreed_outcome").maybeText.flatMap(pc =>
        Try(ProgressionDecisionOutcome.forPitCode(pc)) match {
          case Success(o) => Some(o)
          case Failure(_) =>
            logger.warn(s"Unable to import decision for $sprCode, $sequence - $pc isn't a recognised agreed pit code")
            None
        }
      ),
      notes = rs.getString("notes").maybeText,
      minutes = rs.getString("minutes").maybeText,
      status = rs.getInt("status")
    )
  }

  class ProgressionDecisionsByAcademicYearsQuery(ds: DataSource)
    extends MappingSqlQuery[ProgressionDecisionRow](ds, ProgressionDecisionsForAcademicYearSql) {
    declareParameter(new SqlParameter("academicYears", Types.VARCHAR))
    compile()

    override def mapRow(resultSet: ResultSet, rowNumber: Int): ProgressionDecisionRow = mapResultSet(resultSet)
  }

  class ProgressionDecisionsByUniversityIdsQuery(ds: DataSource)
    extends MappingSqlQuery[ProgressionDecisionRow](ds, ProgressionDecisionsForUniversityIdsSql) {
    declareParameter(new SqlParameter("universityIds", Types.VARCHAR))
    compile()

    override def mapRow(resultSet: ResultSet, rowNumber: Int): ProgressionDecisionRow = mapResultSet(resultSet)
  }

}

trait ProgressionDecisionImporterComponent {
  def progressionDecisionImporter: ProgressionDecisionImporter
}

trait AutowiringProgressionDecisionImporterComponent extends ProgressionDecisionImporterComponent {
  var progressionDecisionImporter: ProgressionDecisionImporter = Wire[ProgressionDecisionImporter]
}

// Full class rather than case class so it can be BeanWrapped (these need to be vars)
class ProgressionDecisionRow(
  var sprCode: String,
  var sequence: String,
  var academicYear: AcademicYear,
  var outcome: Option[ProgressionDecisionOutcome],
  var notes: Option[String],
  var minutes: Option[String],
  var status: Int,
  var resitPeriod: Boolean,
) extends ToString {
  def toProgressionDecision: ProgressionDecision = {
    val pd = new ProgressionDecision
    pd.sprCode = sprCode
    pd.sequence = sequence
    pd.academicYear = academicYear
    pd.outcome = outcome
    pd.notes = notes
    pd.minutes = minutes
    pd.resitPeriod = resitPeriod
    pd.status = ProgressionDecisionProcessStatus.forCode(status)
    pd
  }

  override def hashCode(): Int =
    new HashCodeBuilder()
      .append(sprCode)
      .append(sequence)
      .append(academicYear)
      .append(outcome)
      .append(notes)
      .append(minutes)
      .append(resitPeriod)
      .append(status)
      .build()

  override def equals(obj: Any): Boolean = obj match {
    case other: ProgressionDecisionRow =>
      new EqualsBuilder()
        .append(sprCode, other.sprCode)
        .append(sequence, other.sequence)
        .append(academicYear, other.academicYear)
        .append(outcome, other.outcome)
        .append(notes, other.notes)
        .append(minutes, other.minutes)
        .append(resitPeriod, other.resitPeriod)
        .append(status, other.status)
        .build()
  }

  override def toStringProps: Seq[(String, Any)] = Seq(
    "sprCode" -> sprCode,
    "sequence" -> sequence,
    "academicYear" -> academicYear,
    "outcome" -> outcome,
    "notes" -> notes,
    "minutes" -> minutes,
    "resitPeriod" -> resitPeriod,
    "status" -> status
  )
}
