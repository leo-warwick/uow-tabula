package uk.ac.warwick.tabula.services.scheduling

import java.sql.ResultSet
import javax.sql.DataSource

import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.scheduling.imports.{ImportLevelCommand, ImportAcademicInformationCommand}
import uk.ac.warwick.tabula.data.LevelDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Level
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.collection.JavaConversions._

trait LevelImporter extends Logging {
	var levelDao: LevelDao = Wire[LevelDao]

	private var levelMap: Map[String, Level] = _

	def getLevelByCodeCached(code: String): Option[Level] = {
		if (levelMap == null) updateLevelMap()

		code.maybeText.flatMap {
			levelCode => levelMap.get(levelCode)
		}
	}

	protected def updateLevelMap() {
		levelMap = slurpLevels()
	}

	private def slurpLevels(): Map[String, Level] = {
		transactional(readOnly = true) {
			logger.debug("refreshing SITS level map")

			(for {
				levelCode <- levelDao.getAllLevelCodes
				level <- levelDao.getByCode(levelCode)
			} yield (levelCode -> level)).toMap

		}
	}

	def importLevels(): ImportAcademicInformationCommand.ImportResult = {
		val results = getImportCommands().map { _.apply()._2 }

		updateLevelMap()

		ImportAcademicInformationCommand.combineResults(results)
	}

	def getImportCommands(): Seq[ImportLevelCommand]
}

@Profile(Array("dev", "test", "production")) @Service
class SitsLevelImporter extends LevelImporter {
	import SitsLevelImporter._

	var sits: DataSource = Wire[DataSource]("sitsDataSource")

	lazy val levelsQuery = new LevelsQuery(sits)

	def getImportCommands(): Seq[ImportLevelCommand] = {
		levelsQuery.execute.toSeq
	}
}

object SitsLevelImporter {
	val sitsSchema: String = Wire.property("${schema.sits}")

	// the level table in SITS holds value such as
	// 1 (Undergraduate Level 1, M1 (taught PG) and M2 (research PG).
	def GetLevel = f"""
		select lev_code, lev_snam, lev_name from $sitsSchema.cam_lev
		"""

	class LevelsQuery(ds: DataSource) extends MappingSqlQuery[ImportLevelCommand](ds, GetLevel) {
		compile()
		override def mapRow(resultSet: ResultSet, rowNumber: Int) =
			new ImportLevelCommand(
				LevelInfo(
					code=resultSet.getString("lev_code"),
					shortName=resultSet.getString("lev_snam"),
					fullName=resultSet.getString("lev_name")
				)
			)
	}

}

case class LevelInfo(code: String, shortName: String, fullName: String)

@Profile(Array("sandbox")) @Service
class SandboxLevelImporter extends LevelImporter {

	def getImportCommands(): Seq[ImportLevelCommand] =
		Seq(
			new ImportLevelCommand(LevelInfo("1", "LEVEL 1", "Undergraduate Level 1")),
			new ImportLevelCommand(LevelInfo("2", "LEVEL 2", "Undergraduate Level 2")),
			new ImportLevelCommand(LevelInfo("3", "LEVEL 3", "Undergraduate Level 3")),
			new ImportLevelCommand(LevelInfo("4", "LEVEL 4", "Undergraduate Level 4")),
			new ImportLevelCommand(LevelInfo("5", "LEVEL 5", "Undergraduate Level 5")),
			new ImportLevelCommand(LevelInfo("6", "LEVEL 6", "Undergraduate Level 6")),
			new ImportLevelCommand(LevelInfo("7", "LEVEL 7", "Undergraduate Level 7")),
			new ImportLevelCommand(LevelInfo("8", "LEVEL 8", "Undergraduate Level 8")),
			new ImportLevelCommand(LevelInfo("9", "LEVEL 9", "Undergraduate Level 9")),
			new ImportLevelCommand(LevelInfo("9", "LEVEL 9", "Undergraduate Level 9")),
			new ImportLevelCommand(LevelInfo("10", "LEVEL 10", "Undergraduate Level 10")),
			new ImportLevelCommand(LevelInfo("11", "LEVEL 11", "Undergraduate Level 11")),
			new ImportLevelCommand(LevelInfo("12", "LEVEL 12", "Undergraduate Level 12")),
			new ImportLevelCommand(LevelInfo("F", "FOUNDATION", "Foundation")),
			new ImportLevelCommand(LevelInfo("M1", "TAUGHT (PG)", "Taught Postgraduate Level")),
			new ImportLevelCommand(LevelInfo("M2", "RESEARCH (PG)", "Research Postgraduate Level")),
			new ImportLevelCommand(LevelInfo("X","POST EXPERIENCE", "Post-Experience"))
	)
}

trait LevelImporterComponent {
	def levelImporter: LevelImporter
}

trait AutowiringLevelImporterComponent extends LevelImporterComponent {
	var levelImporter: LevelImporter = Wire[LevelImporter]
}
