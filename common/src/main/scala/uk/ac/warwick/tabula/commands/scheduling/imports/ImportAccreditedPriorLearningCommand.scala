package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.beans.{BeanWrapper, BeanWrapperImpl}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Command, Description, Unaudited}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.{AccreditedPriorLearning, Award, Level, StudentCourseDetails}
import uk.ac.warwick.tabula.data.{AccreditedPriorLearningDao, StudentCourseDetailsDao}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.scheduling.PropertyCopying
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.scheduling.{AutowiringAwardImporterComponent, AutowiringLevelImporterComponent, AccreditedPriorLearningRow}

class ImportAccreditedPriorLearningCommand(accreditedPriorLearningRow: AccreditedPriorLearningRow)
	extends Command[Option[AccreditedPriorLearning]]
	with Logging with Unaudited with PropertyCopying
	with AutowiringAwardImporterComponent
	with AutowiringLevelImporterComponent {

	PermissionCheck(Permissions.ImportSystemData)

	var accreditedPriorLearningDao: AccreditedPriorLearningDao = Wire[AccreditedPriorLearningDao]
	var studentCourseDetailsDao: StudentCourseDetailsDao = Wire[StudentCourseDetailsDao]

	val scjCode: String = accreditedPriorLearningRow.scjCode
	val awardCode: String = accreditedPriorLearningRow.awardCode
	val sequenceNumber: Int = accreditedPriorLearningRow.sequenceNumber
	val academicYear: AcademicYear = AcademicYear.parse(accreditedPriorLearningRow.academicYear)
	val cats: JBigDecimal = accreditedPriorLearningRow.cats
	val levelCode: String = accreditedPriorLearningRow.levelCode
	val reason: String = accreditedPriorLearningRow.reason

	override def applyInternal(): Option[AccreditedPriorLearning] = transactional() ({
		logger.debug("Importing accredited prior learning for student " + scjCode + ", award " + awardCode)

		studentCourseDetailsDao.getByScjCode(scjCode) match {
			case None =>
				logger.warn("Can't record accredited prior learning - could not find a StudentCourseDetails for " + scjCode)
				None
			case Some(scd: StudentCourseDetails) =>
				awardImporter.getAwardByCodeCached(awardCode) match {
					case None =>
						logger.warn("Can't record accredited prior learning - could not find award for award code " + awardCode)
						None
					case Some(award: Award) =>
						levelImporter.getLevelByCodeCached(levelCode) match {
							case None =>
								logger.warn ("Can't record accredited prior learning - couldn't find level for level code " + levelCode)
								None
							case Some(level: Level) => storeAccreditedPriorLearning(scd, award, level)
						}
				}
		}
	})


	def storeAccreditedPriorLearning(scd: StudentCourseDetails, award: Award, level: Level): Some[AccreditedPriorLearning] = {
		val accreditedPriorLearningExisting: Option[AccreditedPriorLearning] = accreditedPriorLearningDao.getByNotionalKey(scd, award, sequenceNumber)
		val isTransient = accreditedPriorLearningExisting.isEmpty

		val accreditedPriorLearning = accreditedPriorLearningExisting match {
			case Some(accreditedPriorLearning: AccreditedPriorLearning) => accreditedPriorLearning
			case _ => new AccreditedPriorLearning(scd, award, sequenceNumber, academicYear, cats, level, reason)
		}

		val commandBean = new BeanWrapperImpl(this)
		val accreditedPriorLearningBean = new BeanWrapperImpl(accreditedPriorLearning)

		val hasChanged = copyBasicProperties(properties, commandBean, accreditedPriorLearningBean) |
			copyLevel(accreditedPriorLearningBean, levelCode)

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + accreditedPriorLearning)

			accreditedPriorLearning.lastUpdatedDate = DateTime.now
			accreditedPriorLearningDao.saveOrUpdate(accreditedPriorLearning)
		}

		Some(accreditedPriorLearning)
	}

	def copyLevel(destinationBean: BeanWrapper, levelCode: String): Boolean = {
		val property = "level"
		val oldValue = destinationBean.getPropertyValue(property)

		val level = levelImporter.getLevelByCodeCached(levelCode)
		level match {
			case Some(level: Level) =>
				if (oldValue != level) {
					destinationBean.setPropertyValue(property, level)
					true
				}
				else false
			case None =>
				if (oldValue != null) {
					destinationBean.setPropertyValue(property, null)
					true
				}
				else false
		}
	}

	private val properties = Set(
		"academicYear", "cats", "reason"
	)

	override def describe(d: Description): Unit = d.properties("scjCode" -> scjCode)

}
