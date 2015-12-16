package uk.ac.warwick.tabula.scheduling.commands.imports

import org.joda.time.DateTime
import org.springframework.beans.BeanWrapperImpl
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, Description, Unaudited}
import uk.ac.warwick.tabula.data.ModeOfAttendanceDao
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.ModeOfAttendance
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.scheduling.helpers.PropertyCopying
import uk.ac.warwick.tabula.scheduling.services.ModeOfAttendanceInfo

class ImportModeOfAttendanceCommand(info: ModeOfAttendanceInfo)
	extends Command[(ModeOfAttendance, ImportAcademicInformationCommand.ImportResult)] with Logging
	with Unaudited with PropertyCopying {

	PermissionCheck(Permissions.ImportSystemData)

	var modeOfAttendanceDao = Wire.auto[ModeOfAttendanceDao]

	// A couple of intermediate properties that will be transformed later
	var code = info.code
	var shortName = info.shortName
	var fullName = info.fullName

	override def applyInternal() = transactional() ({
		val modeOfAttendanceExisting = modeOfAttendanceDao.getByCode(code)

		logger.debug("Importing mode of attendance " + code + " into " + modeOfAttendanceExisting)

		val isTransient = !modeOfAttendanceExisting.isDefined

		val modeOfAttendance = modeOfAttendanceExisting match {
			case Some(modeOfAttendance: ModeOfAttendance) => modeOfAttendance
			case _ => new ModeOfAttendance()
		}

		val commandBean = new BeanWrapperImpl(this)
		val modeOfAttendanceBean = new BeanWrapperImpl(modeOfAttendance)

		val hasChanged = copyBasicProperties(properties, commandBean, modeOfAttendanceBean)

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + modeOfAttendance)

			modeOfAttendance.lastUpdatedDate = DateTime.now
			modeOfAttendanceDao.saveOrUpdate(modeOfAttendance)
		}

		val result =
			if (isTransient) ImportAcademicInformationCommand.ImportResult(added = 1)
			else if (hasChanged) ImportAcademicInformationCommand.ImportResult(deleted = 1)
			else ImportAcademicInformationCommand.ImportResult()

		(modeOfAttendance, result)
	})

	private val properties = Set(
		"code", "shortName", "fullName"
	)

	override def describe(d: Description) = d.property("shortName" -> shortName)

}
