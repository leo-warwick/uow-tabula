package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.beans.BeanWrapperImpl
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.scheduling.imports.ImportAcademicInformationCommand.ImportResult
import uk.ac.warwick.tabula.commands.{Command, Description, Unaudited}
import uk.ac.warwick.tabula.data.{Daoisms, LevelDao}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.Level
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.scheduling.PropertyCopying
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.scheduling.LevelInfo

class ImportLevelCommand(info: LevelInfo)
	extends Command[(Level, ImportAcademicInformationCommand.ImportResult)] with Logging with Daoisms
	with Unaudited with PropertyCopying {

	PermissionCheck(Permissions.ImportSystemData)

	var levelDao: LevelDao = Wire.auto[LevelDao]

	var code: String = info.code
	var shortName: String = info.shortName
	var name: String = info.fullName

	override def applyInternal(): (Level, ImportResult) = transactional() {
		val levelExisting = levelDao.getByCode(code)

		logger.debug("Importing level " + code + " into " + levelExisting)

		val isTransient = !levelExisting.isDefined

		val level = levelExisting match {
			case Some(crs: Level) => crs
			case _ => new Level()
		}

		val commandBean = new BeanWrapperImpl(this)
		val levelBean = new BeanWrapperImpl(level)

		val hasChanged = copyBasicProperties(properties, commandBean, levelBean)

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + level)

			level.lastUpdatedDate = DateTime.now
			levelDao.saveOrUpdate(level)
		}

		val result =
			if (isTransient) ImportAcademicInformationCommand.ImportResult(added = 1)
			else if (hasChanged) ImportAcademicInformationCommand.ImportResult(deleted = 1)
			else ImportAcademicInformationCommand.ImportResult()

		(level, result)
	}

	private val properties = Set(
		"code", "shortName", "name"
	)

	override def describe(d: Description): Unit = d.property("shortName" -> shortName)

}
