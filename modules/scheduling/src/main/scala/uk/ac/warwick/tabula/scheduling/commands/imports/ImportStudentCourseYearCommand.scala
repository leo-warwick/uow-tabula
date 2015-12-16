package uk.ac.warwick.tabula.scheduling.commands.imports

import org.joda.time.DateTime
import org.springframework.beans.{BeanWrapper, BeanWrapperImpl}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Command, Description, Unaudited}
import uk.ac.warwick.tabula.data.StudentCourseYearDetailsDao
import uk.ac.warwick.tabula.data.model.{ModeOfAttendance, ModuleRegistrationStatus, StudentCourseDetails, StudentCourseYearDetails, StudentCourseYearKey}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.scheduling.helpers.{ImportRowTracker, PropertyCopying, SitsStudentRow}
import uk.ac.warwick.tabula.scheduling.services.ModeOfAttendanceImporter
import uk.ac.warwick.tabula.services.ProfileService

class ImportStudentCourseYearCommand(row: SitsStudentRow, studentCourseDetails: StudentCourseDetails, importRowTracker: ImportRowTracker)
	extends Command[StudentCourseYearDetails] with Logging
	with Unaudited with PropertyCopying {
	import ImportMemberHelpers._

	var modeOfAttendanceImporter = Wire[ModeOfAttendanceImporter]
	var profileService = Wire[ProfileService]
	var studentCourseYearDetailsDao = Wire[StudentCourseYearDetailsDao]

	val sceSequenceNumber = row.sceSequenceNumber

	override def applyInternal(): StudentCourseYearDetails = {
		val studentCourseYearDetailsExisting = studentCourseYearDetailsDao.getBySceKeyStaleOrFresh(
			studentCourseDetails,
			sceSequenceNumber)

		logger.debug("Importing student course details for " + studentCourseDetails.scjCode + ", " + sceSequenceNumber)

		val rowBean = new BeanWrapperImpl(row)

		val (isTransient, studentCourseYearDetails) = studentCourseYearDetailsExisting match {
			case Some(studentCourseYearDetails: StudentCourseYearDetails) => (false, studentCourseYearDetails)
			case _ => (true, new StudentCourseYearDetails(studentCourseDetails, sceSequenceNumber,AcademicYear.parse(row.academicYearString)))
		}
		val studentCourseYearDetailsBean = new BeanWrapperImpl(studentCourseYearDetails)

		val hasChanged = (copyStudentCourseYearProperties(rowBean, studentCourseYearDetailsBean)
			| markAsSeenInSits(studentCourseYearDetailsBean))

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + studentCourseYearDetails)

			if (studentCourseDetails.latestStudentCourseYearDetails == null ||
				// need to include fresh or stale since this might be a row which was deleted but has been re-instated
				studentCourseDetails.freshOrStaleStudentCourseYearDetails.forall { _ <= studentCourseYearDetails }) {
				studentCourseDetails.latestStudentCourseYearDetails = studentCourseYearDetails
			}

			studentCourseYearDetails.lastUpdatedDate = DateTime.now
			studentCourseYearDetailsDao.saveOrUpdate(studentCourseYearDetails)
		}

		val key = new StudentCourseYearKey(studentCourseYearDetails.studentCourseDetails.scjCode, studentCourseYearDetails.sceSequenceNumber)
		importRowTracker.studentCourseYearDetailsSeen.add(key)

		studentCourseYearDetails
	}

	private val basicStudentCourseYearProperties = Set(
		"yearOfStudy"
	)

	private def copyStudentCourseYearProperties(commandBean: BeanWrapper, studentCourseYearBean: BeanWrapper) = {
		copyBasicProperties(basicStudentCourseYearProperties, commandBean, studentCourseYearBean) |
		copyObjectProperty("enrolmentDepartment", row.enrolmentDepartmentCode, studentCourseYearBean, toDepartment(row.enrolmentDepartmentCode)) |
		copyObjectProperty("enrolmentStatus", row.enrolmentStatusCode, studentCourseYearBean, toSitsStatus(row.enrolmentStatusCode)) |
		copyModeOfAttendance(row.modeOfAttendanceCode, studentCourseYearBean) |
		copyModuleRegistrationStatus(row.moduleRegistrationStatusCode, studentCourseYearBean) |
		copyAcademicYear("academicYear", row.academicYearString, studentCourseYearBean) |
		copyEnrolledOrCompleted("enrolledOrCompleted", row.reasonForTransferCode, row.enrolmentStatusCode, studentCourseYearBean)
	}

	private def copyModeOfAttendance(code: String, studentCourseYearBean: BeanWrapper) = {
		val property = "modeOfAttendance"
		val oldValue = studentCourseYearBean.getPropertyValue(property) match {
			case null => null
			case value: ModeOfAttendance => value
		}

		if (oldValue == null && code == null) false
		else if (oldValue == null) {
			// From no MOA to having an MOA
			studentCourseYearBean.setPropertyValue(property, toModeOfAttendance(code))
			true
		} else if (code == null) {
			// User had an SPR status code but now doesn't
			studentCourseYearBean.setPropertyValue(property, null)
			true
		} else if (oldValue.code == code.toLowerCase) {
			false
		}	else {
			studentCourseYearBean.setPropertyValue(property, toModeOfAttendance(code))
			true
		}
	}

	def copyModuleRegistrationStatus(code: String, destinationBean: BeanWrapper) = {
		val property = "moduleRegistrationStatus"
		val oldValue = destinationBean.getPropertyValue(property)
		val newValue = ModuleRegistrationStatus.fromCode(code)

		logger.debug("Property " + property + ": " + oldValue + " -> " + newValue)

		// null == null in Scala so this is safe for unset values
		if (oldValue != newValue) {
			logger.debug("Detected property change for " + property + " (" + oldValue + " -> " + newValue + "); setting value")

			destinationBean.setPropertyValue(property, newValue)
			true
		}
		else false
	}

	private def copyAcademicYear(property: String, acYearString: String, memberBean: BeanWrapper) = {
		val oldValue = memberBean.getPropertyValue(property) match {
			case value: AcademicYear => value
			case _ => null
		}

		val newValue = AcademicYear.parse(acYearString)

		if (oldValue == null && acYearString == null) false
		else if (oldValue == null) {
			// From no academic year to having an academic year
			memberBean.setPropertyValue(property, toAcademicYear(acYearString))
			true
		} else if (acYearString == null) {
			// Record had an academic year but now doesn't
			memberBean.setPropertyValue(property, null)
			true
		} else if (oldValue == newValue) {
			false
		} else {
			memberBean.setPropertyValue(property, toAcademicYear(acYearString))
			true
		}
	}

	private def copyEnrolledOrCompleted(property: String, reasonForTransferCode: String, enrolmentStatusCode: String, memberBean: BeanWrapper) = {
		val oldValue = memberBean.getPropertyValue(property)

		// EnrolledOrCompleted if SCE is not permanently withdrawn OR SCJ reason for transfer code is successful
		val newValue = !enrolmentStatusCode.safeStartsWith("P") || reasonForTransferCode.safeStartsWith("S")

		if (oldValue != newValue) {
			logger.debug(s"Detected property change for $property: $oldValue -> $newValue; setting value")

			memberBean.setPropertyValue(property, newValue)
			true
		} else false
	}

	private def toModeOfAttendance(code: String) = {
		if (code == null || code == "") {
			null
		} else {
			modeOfAttendanceImporter.getModeOfAttendanceForCode(code).orNull
		}
	}

	override def describe(d: Description) = d.property("scjCode" -> studentCourseDetails.scjCode).property("sceSequenceNumber" -> sceSequenceNumber)
}
