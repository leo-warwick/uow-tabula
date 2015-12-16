package uk.ac.warwick.tabula.scheduling.commands.imports

import org.joda.time.DateTime
import org.springframework.beans.BeanWrapperImpl
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, Description, Unaudited}
import uk.ac.warwick.tabula.data.DisabilityDao
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.Disability
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.scheduling.helpers.PropertyCopying
import uk.ac.warwick.tabula.scheduling.services.DisabilityInfo

class ImportDisabilitiesCommand(info: DisabilityInfo)
	extends Command[(Disability, ImportAcademicInformationCommand.ImportResult)] with Logging
	with Unaudited with PropertyCopying {

	PermissionCheck(Permissions.ImportSystemData)

	var disabilityDao = Wire.auto[DisabilityDao]

	var code = info.code
	var shortName = info.shortName
	var sitsDefinition = info.definition

	override def applyInternal(): (Disability, ImportAcademicInformationCommand.ImportResult) = transactional() {
		val disabilityExisting = disabilityDao.getByCode(code)

		logger.debug("Importing disability " + code + " into " + disabilityExisting)

		val isTransient = !disabilityExisting.isDefined

		val disability = disabilityExisting.getOrElse(new Disability)

		val commandBean = new BeanWrapperImpl(this)
		val disabilityBean = new BeanWrapperImpl(disability)

		val hasChanged = copyBasicProperties(properties, commandBean, disabilityBean)

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + disability)

			disability.lastUpdatedDate = DateTime.now
			disabilityDao.saveOrUpdate(disability)
		}

		val result =
			if (isTransient) ImportAcademicInformationCommand.ImportResult(added = 1)
			else if (hasChanged) ImportAcademicInformationCommand.ImportResult(deleted = 1)
			else ImportAcademicInformationCommand.ImportResult()

		(disability, result)
	}

	private val properties = Set(
		"code", "shortName", "sitsDefinition"
	)

	override def describe(d: Description) = d.property("shortName" -> shortName)

}
