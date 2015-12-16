package uk.ac.warwick.tabula.scheduling.commands.imports

import org.joda.time.DateTime
import org.springframework.beans.BeanWrapperImpl
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, Description, Unaudited}
import uk.ac.warwick.tabula.data.CourseDao
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.Course
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.scheduling.helpers.PropertyCopying
import uk.ac.warwick.tabula.scheduling.services.CourseInfo

class ImportCourseCommand(info: CourseInfo)
	extends Command[(Course, ImportAcademicInformationCommand.ImportResult)] with Logging
	with Unaudited with PropertyCopying {

	PermissionCheck(Permissions.ImportSystemData)

	var courseDao = Wire.auto[CourseDao]

	var code = info.code
	var shortName = info.shortName
	var name = info.fullName
	var title = info.title

	override def applyInternal() = transactional() {
		val courseExisting = courseDao.getByCode(code)

		logger.debug("Importing course " + code + " into " + courseExisting)

		val isTransient = !courseExisting.isDefined

		val course = courseExisting match {
			case Some(crs: Course) => crs
			case _ => new Course()
		}

		val commandBean = new BeanWrapperImpl(this)
		val courseBean = new BeanWrapperImpl(course)

		val hasChanged = copyBasicProperties(properties, commandBean, courseBean)

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + course)

			course.lastUpdatedDate = DateTime.now
			courseDao.saveOrUpdate(course)
		}

		val result =
			if (isTransient) ImportAcademicInformationCommand.ImportResult(added = 1)
			else if (hasChanged) ImportAcademicInformationCommand.ImportResult(deleted = 1)
			else ImportAcademicInformationCommand.ImportResult()

		(course, result)
	}

	private val properties = Set(
		"code", "shortName", "name", "title"
	)

	override def describe(d: Description) = d.property("shortName" -> shortName)

}
