package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.beans.BeanWrapperImpl
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, Description, Unaudited}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.Award
import uk.ac.warwick.tabula.data.{AwardDao, Daoisms}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.scheduling.PropertyCopying
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.scheduling.AwardInfo

class ImportAwardCommand(info: AwardInfo)
	extends Command[(Award, ImportAcademicInformationCommand.ImportResult)] with Logging with Daoisms
	with Unaudited with PropertyCopying {

	PermissionCheck(Permissions.ImportSystemData)

	var awardDao: AwardDao = Wire.auto[AwardDao]

	var code: String = info.code
	var shortName: String = info.shortName
	var name: String = info.fullName

	override def applyInternal(): (Award, ImportAcademicInformationCommand.ImportResult) = transactional() {
		val awardExisting = awardDao.getByCode(code)

		logger.debug("Importing award " + code + " into " + awardExisting)

		val isTransient = !awardExisting.isDefined

		val award = awardExisting.getOrElse(new Award)

		val commandBean = new BeanWrapperImpl(this)
		val awardBean = new BeanWrapperImpl(award)

		val hasChanged = copyBasicProperties(properties, commandBean, awardBean)

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + award)

			award.lastUpdatedDate = DateTime.now
			awardDao.saveOrUpdate(award)
		}

		val result =
			if (isTransient) ImportAcademicInformationCommand.ImportResult(added = 1)
			else if (hasChanged) ImportAcademicInformationCommand.ImportResult(deleted = 1)
			else ImportAcademicInformationCommand.ImportResult()

		(award, result)
	}

	private val properties = Set(
		"code", "shortName", "name"
	)

	override def describe(d: Description): Unit = d.property("shortName" -> shortName)

}
