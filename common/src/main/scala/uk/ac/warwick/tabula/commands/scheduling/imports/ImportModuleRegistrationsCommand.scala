package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.beans.{BeanWrapper, PropertyAccessorFactory}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Command, Description, Unaudited}
import uk.ac.warwick.tabula.data.ModuleRegistrationDao
import uk.ac.warwick.tabula.data.model.{Module, ModuleRegistration, ModuleSelectionStatus, StudentCourseDetails}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.scheduling.PropertyCopying
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.scheduling.ModuleRegistrationRow

class ImportModuleRegistrationsCommand(course: StudentCourseDetails, courseRows: Seq[ModuleRegistrationRow], modules: Set[Module])
	extends Command[Seq[ModuleRegistration]] with Logging with Unaudited with PropertyCopying {

	PermissionCheck(Permissions.ImportSystemData)

	var moduleRegistrationDao: ModuleRegistrationDao = Wire[ModuleRegistrationDao]



	override def applyInternal(): Seq[ModuleRegistration] = {
		logger.debug("Importing module registration for student " + course.scjCode)

		courseRows.map { modRegRow =>
			val module = modules.find(_.code == Module.stripCats(modRegRow.sitsModuleCode).get.toLowerCase).get
			val existingRegistration = course.moduleRegistrations.find(mr =>
				mr.module.code == module.code
					&& mr.academicYear == AcademicYear.parse(modRegRow.academicYear)
					&& mr.cats == modRegRow.cats
					&& mr.occurrence == modRegRow.occurrence
			)

			val isTransient = existingRegistration.isEmpty

			val moduleRegistration = existingRegistration match {
				case Some(moduleRegistration: ModuleRegistration) =>
					moduleRegistration
				case _ =>
					val mr = new ModuleRegistration(
						course,
						module,
						modRegRow.cats,
						AcademicYear.parse(modRegRow.academicYear),
						modRegRow.occurrence
					)
					course.addModuleRegistration(mr)
					mr
			}

			val rowBean = PropertyAccessorFactory.forBeanPropertyAccess(modRegRow)
			val moduleRegistrationBean = PropertyAccessorFactory.forBeanPropertyAccess(moduleRegistration)

			val hasChanged = copyBasicProperties(properties, rowBean, moduleRegistrationBean) |
				copySelectionStatus(moduleRegistrationBean, modRegRow.selectionStatusCode) |
				copyBigDecimal(moduleRegistrationBean, "actualMark", modRegRow.actualMark) |
				copyBigDecimal(moduleRegistrationBean, "agreedMark", modRegRow.agreedMark)

			if (isTransient || hasChanged) {
				logger.debug("Saving changes for " + moduleRegistration)

				moduleRegistration.lastUpdatedDate = DateTime.now
				moduleRegistrationDao.saveOrUpdate(moduleRegistration)
			}

			moduleRegistration

		}
	}

	def copySelectionStatus(destinationBean: BeanWrapper, selectionStatusCode: String): Boolean = {
		val property = "selectionStatus"
		val oldValue = destinationBean.getPropertyValue(property)
		val newValue = ModuleSelectionStatus.fromCode(selectionStatusCode)

		logger.debug("Property " + property + ": " + oldValue + " -> " + newValue)

		// null == null in Scala so this is safe for unset values
		if (oldValue != newValue) {
			logger.debug("Detected property change for " + property + " (" + oldValue + " -> " + newValue + "); setting value")

			destinationBean.setPropertyValue(property, newValue)
			true
		}
		else false
	}

	private val properties = Set(
		"assessmentGroup", "occurrence", "actualGrade", "agreedGrade"
	)

	override def describe(d: Description): Unit = d.properties("scjCode" -> course.scjCode)

}
