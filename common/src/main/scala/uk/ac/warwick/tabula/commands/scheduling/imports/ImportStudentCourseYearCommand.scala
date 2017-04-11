package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.beans.{BeanWrapper, BeanWrapperImpl}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Command, Description, Unaudited}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{Daoisms, StudentCourseYearDetailsDao}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.scheduling.{PropertyCopying, SitsStudentRow}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.scheduling._

class ImportStudentCourseYearCommand(row: SitsStudentRow, studentCourseDetails: StudentCourseDetails)
	extends Command[StudentCourseYearDetails] with Logging with Daoisms
	with Unaudited with PropertyCopying {

	var modeOfAttendanceImporter: ModeOfAttendanceImporter = Wire[ModeOfAttendanceImporter]
	var profileService: ProfileService = Wire[ProfileService]
	var studentCourseYearDetailsDao: StudentCourseYearDetailsDao = Wire[StudentCourseYearDetailsDao]
	var courseAndRouteService: CourseAndRouteService = Wire[CourseAndRouteService]

	val sceSequenceNumber: _root_.uk.ac.warwick.tabula.JavaImports.JInteger = row.sceSequenceNumber

	override def applyInternal(): StudentCourseYearDetails = {
		val studentCourseYearDetailsExisting = studentCourseYearDetailsDao.getBySceKeyStaleOrFresh(
			studentCourseDetails,
			sceSequenceNumber)

		logger.debug("Importing student course details for " + studentCourseDetails.scjCode + ", " + sceSequenceNumber)

		val rowBean = new BeanWrapperImpl(row)

		val (isTransient, studentCourseYearDetails) = studentCourseYearDetailsExisting match {
			case Some(studentCourseYearDetails: StudentCourseYearDetails) => (false, studentCourseYearDetails)
			case _ => (true, new StudentCourseYearDetails(studentCourseDetails, sceSequenceNumber, AcademicYear.parse(row.academicYearString)))
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


		studentCourseYearDetails
	}

	private val basicStudentCourseYearProperties = Set(
		"yearOfStudy"
	)

	private def copyStudentCourseYearProperties(commandBean: BeanWrapper, studentCourseYearBean: BeanWrapper) = {
		copyBasicProperties(basicStudentCourseYearProperties, commandBean, studentCourseYearBean) |
		copyObjectProperty("enrolmentDepartment", row.enrolmentDepartmentCode, studentCourseYearBean, toDepartment(row.enrolmentDepartmentCode)) |
		copyObjectProperty("enrolmentStatus", row.enrolmentStatusCode, studentCourseYearBean, toSitsStatus(row.enrolmentStatusCode)) |
		copyObjectProperty("route", row.sceRouteCode, studentCourseYearBean, courseAndRouteService.getRouteByCode(row.sceRouteCode)) |
		copyModeOfAttendance(row.modeOfAttendanceCode, studentCourseYearBean) |
		copyModuleRegistrationStatus(row.moduleRegistrationStatusCode, studentCourseYearBean) |
		copyAcademicYear("academicYear", row.academicYearString, studentCourseYearBean) |
		copyEnrolledOrCompleted("enrolledOrCompleted", row.reasonForTransferCode, row.enrolmentStatusCode, studentCourseYearBean) |
		copyAgreedMark(commandBean, studentCourseYearBean)
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

	def copyModuleRegistrationStatus(code: String, destinationBean: BeanWrapper): Boolean = {
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

	// We only want to import an agreed mark from SITS if:
	// * there isn't a mark waiting to be uploaded (agreedMarkUploadedDate is not null) or
	// * there isn't currently a mark (agreedMark is null)
	private def copyAgreedMark(commandBean: BeanWrapper, destinationBean: BeanWrapper): Boolean = {
		val oldMark = destinationBean.getPropertyValue("agreedMark").asInstanceOf[JBigDecimal]
		val oldMarkUploadedDate = destinationBean.getPropertyValue("agreedMarkUploadedDate").asInstanceOf[DateTime]
		if (oldMarkUploadedDate != null || oldMark == null) {
			copyBasicProperties(Set("agreedMark"), commandBean, destinationBean)
		} else {
			false
		}
	}

	private def toModeOfAttendance(code: String) = {
		if (code == null || code == "") {
			null
		} else {
			modeOfAttendanceImporter.getModeOfAttendanceForCode(code).orNull
		}
	}

	override def describe(d: Description): Unit = d.property("scjCode" -> studentCourseDetails.scjCode).property("sceSequenceNumber" -> sceSequenceNumber)
}
