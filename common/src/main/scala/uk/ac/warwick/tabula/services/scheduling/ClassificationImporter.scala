package uk.ac.warwick.tabula.services.scheduling

import java.sql.ResultSet

import javax.sql.DataSource
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.scheduling.imports.{ImportAcademicInformationCommand, ImportClassificationCommand}
import uk.ac.warwick.tabula.data.ClassificationDao
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.Classification
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.jdk.CollectionConverters._

trait ClassificationImporter extends Logging {
  var classificationDao: ClassificationDao = Wire[ClassificationDao]

  private var classificationMap: Map[String, Classification] = _

  protected def updateClassificationMap(): Unit = {
    classificationMap = slurpClassifications()
  }

  private def slurpClassifications(): Map[String, Classification] = {
    transactional(readOnly = true) {
      logger.debug("refreshing SITS classification map")

      (for {
        classification <- classificationDao.getAll
      } yield classification.code -> classification).toMap

    }
  }

  def importClassifications(): ImportAcademicInformationCommand.ImportResult = {
    val results = getImportCommands().map(_.apply()._2)

    updateClassificationMap()

    ImportAcademicInformationCommand.combineResults(results)
  }

  def getClassificationByCodeCached(code: String): Option[Classification] = {
    if (classificationMap == null) updateClassificationMap()

    code.maybeText.flatMap {
      classificationCode => classificationMap.get(classificationCode)
    }
  }

  def getImportCommands(): Seq[ImportClassificationCommand]
}

@Profile(Array("dev", "test", "production"))
@Service
class SitsClassificationImporter extends ClassificationImporter {

  import SitsClassificationImporter._

  var sits: DataSource = Wire[DataSource]("sitsDataSource")

  lazy val classificationsQuery = new ClassificationsQuery(sits)

  def getImportCommands(): Seq[ImportClassificationCommand] = {
    classificationsQuery.execute.asScala.toSeq
  }
}

object SitsClassificationImporter {
  val sitsSchema: String = Wire.property("${schema.sits}")

  def GetClassification = f"select cla_code, cla_snam, cla_name from $sitsSchema.cam_cla"

  class ClassificationsQuery(ds: DataSource) extends MappingSqlQuery[ImportClassificationCommand](ds, GetClassification) {
    compile()

    override def mapRow(resultSet: ResultSet, rowNumber: Int) =
      new ImportClassificationCommand(
        ClassificationInfo(
          code = resultSet.getString("cla_code"),
          shortName = resultSet.getString("cla_snam"),
          fullName = resultSet.getString("cla_name")
        )
      )
  }

}


case class ClassificationInfo(code: String, shortName: String, fullName: String)

@Profile(Array("sandbox"))
@Service
class SandboxClassificationImporter extends ClassificationImporter {

  def getImportCommands(): Seq[ImportClassificationCommand] =
    Seq(
      // the most common classifications for current students:
      new ImportClassificationCommand(ClassificationInfo("01", "1 ST CLASS HONS", "First Class")),
      new ImportClassificationCommand(ClassificationInfo("02", "2 (I) HONS", "Second Class, Upper Division")),
      new ImportClassificationCommand(ClassificationInfo("03", "2(II)", "Second Class, Lower Division")),
      new ImportClassificationCommand(ClassificationInfo("05", "3 RD CLASS HONS", "Third Class")),
      new ImportClassificationCommand(ClassificationInfo("09", "PASS DEGREE", "Pass Degree")),
      new ImportClassificationCommand(ClassificationInfo("12", "DISTINCTION", "(with Distinction)")),
    )
}

trait ClassificationImporterComponent {
  def classificationImporter: ClassificationImporter
}

trait AutowiringClassificationImporterComponent extends ClassificationImporterComponent {
  var classificationImporter: ClassificationImporter = Wire[ClassificationImporter]
}
