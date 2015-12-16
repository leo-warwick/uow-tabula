package uk.ac.warwick.tabula.scheduling.commands.imports

import org.joda.time.DateTime
import org.springframework.beans.BeanWrapperImpl
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, Description, Unaudited}
import uk.ac.warwick.tabula.data.SitsStatusDao
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.SitsStatus
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.scheduling.helpers.PropertyCopying
import uk.ac.warwick.tabula.scheduling.services.SitsStatusInfo

class ImportSitsStatusCommand(info: SitsStatusInfo) extends Command[(SitsStatus, ImportAcademicInformationCommand.ImportResult)] with Logging
	with Unaudited with PropertyCopying {

	PermissionCheck(Permissions.ImportSystemData)

	var sitsStatusDao = Wire.auto[SitsStatusDao]

	var code = info.code
	var shortName = info.shortName
	var fullName = info.fullName

	override def applyInternal() = transactional() ({
		val sitsStatusExisting = sitsStatusDao.getByCode(code)

		logger.debug("Importing SITS status " + code + " into " + sitsStatusExisting)

		val isTransient = !sitsStatusExisting.isDefined

		val sitsStatus = sitsStatusExisting match {
			case Some(sitsStatus: SitsStatus) => sitsStatus
			case _ => new SitsStatus()
		}

		val commandBean = new BeanWrapperImpl(ImportSitsStatusCommand.this)
		val sitsStatusBean = new BeanWrapperImpl(sitsStatus)

		val hasChanged = copyBasicProperties(properties, commandBean, sitsStatusBean)

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + sitsStatus)

			sitsStatus.lastUpdatedDate = DateTime.now
			sitsStatusDao.saveOrUpdate(sitsStatus)
		}

		val result =
			if (isTransient) ImportAcademicInformationCommand.ImportResult(added = 1)
			else if (hasChanged) ImportAcademicInformationCommand.ImportResult(deleted = 1)
			else ImportAcademicInformationCommand.ImportResult()

		(sitsStatus, result)
	})

	private val properties = Set(
		"code", "shortName", "fullName"
	)

	override def describe(d: Description) = d.property("shortName" -> shortName)

}
