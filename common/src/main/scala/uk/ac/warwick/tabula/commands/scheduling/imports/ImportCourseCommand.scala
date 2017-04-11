package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.beans.BeanWrapperImpl
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.scheduling.imports.ImportAcademicInformationCommand.ImportResult
import uk.ac.warwick.tabula.commands.{Command, Description, Unaudited}
import uk.ac.warwick.tabula.data.{CourseDao, Daoisms}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.Course
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.scheduling.PropertyCopying
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.scheduling.CourseInfo

class ImportCourseCommand(info: CourseInfo)
	extends Command[(Course, ImportAcademicInformationCommand.ImportResult)] with Logging with Daoisms
	with Unaudited with PropertyCopying {

	PermissionCheck(Permissions.ImportSystemData)

	var courseDao: CourseDao = Wire.auto[CourseDao]

	var code: String = info.code
	var shortName: String = info.shortName
	var name: String = info.fullName
	var title: String = info.title

	override def applyInternal(): (Course, ImportResult) = transactional() {
		val courseExisting = courseDao.getByCode(code)

		logger.debug("Importing course " + code + " into " + courseExisting)

		val isTransient = courseExisting.isEmpty

		val course = courseExisting match {
			case Some(crs: Course) => crs
			case _ => new Course()
		}

		val commandBean = new BeanWrapperImpl(this)
		val courseBean = new BeanWrapperImpl(course)

		val hasChanged = copyBasicProperties(properties, commandBean, courseBean) |
			(if (course.department.isEmpty) {
				copyObjectProperty("department", info.departmentCode, courseBean, toDepartment(info.departmentCode))
			} else {
				false
			})

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

	override def describe(d: Description): Unit = d.property("shortName" -> shortName)

}
