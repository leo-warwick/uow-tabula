package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.format.annotation.DateTimeFormat
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.DateFormats._
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommandWithoutTransaction, Describable, Description}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.{StudentCourseDetailsDaoComponent, StudentCourseYearDetailsDaoComponent, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.scheduling.{AutowiringProfileImporterComponent, ProfileImporterComponent}
import uk.ac.warwick.tabula.validators.WithinYears


object RecheckMissingRowsCommand {
	def apply() =
		new RecheckMissingRowsCommandInternal
			with AutowiringMemberDaoComponent
			with AutowiringStudentCourseYearDetailsDaoComponent
			with AutowiringStudentCourseDetailsDaoComponent
			with AutowiringProfileImporterComponent
			with ComposableCommandWithoutTransaction[Unit]
			with RecheckMissingRowsState
			with RecheckMissingRowsDescription
			with MissingRowsPermissions
}


class RecheckMissingRowsCommandInternal extends CommandInternal[Unit] with Logging
	with Daoisms with ChecksStudentsInSits {

	self: MemberDaoComponent with StudentCourseYearDetailsDaoComponent with StudentCourseDetailsDaoComponent
		with ProfileImporterComponent with RecheckMissingRowsState =>

	override def applyInternal(): Unit = {
		val universityIds = transactional() { memberDao.getMissingSince(from).toSet }
		logger.info(s"${universityIds.size} students to fetch from SITS that were marked as missing since ${CSVDate.print(from)}")

		val studentsFound = checkSitsForStudents(universityIds)

		val notStaleUniversityIds = universityIds.intersect(studentsFound.universityIdsSeen)
		val notStaleScjCodes = {
			val staleCodes =  transactional() { studentCourseDetailsDao.getStaleScjCodesSince(from).toSet }
			staleCodes.intersect(studentsFound.scjCodesSeen)
		}
		val notStaleScydIds = {
			val scydIdsSeen = transactional() { studentCourseYearDetailsDao.convertKeysToIds(studentsFound.studentCourseYearKeysSeen.toSeq).toSet }
			val staleIds = transactional() { studentCourseYearDetailsDao.getIdsStaleSince(from).toSet }
			staleIds.intersect(scydIdsSeen)
		}

		logger.warn(s"Removing timestamp for ${notStaleUniversityIds.size} Members")
		transactional() { memberDao.unstampPresentInImport(notStaleUniversityIds.toSeq) }

		logger.warn(s"Removing timestamp for ${notStaleScjCodes.size} studentCourseDetails")
		transactional() { studentCourseDetailsDao.unstampPresentInImport(notStaleScydIds.toSeq) }

		logger.warn(s"Removing timestamp for ${notStaleScydIds.size} studentCourseYearDetails")
		transactional() { studentCourseYearDetailsDao.unstampPresentInImport(notStaleScydIds.toSeq) }

		transactional() { session.flush() }
		transactional() { session.clear() }
	}

}

trait RecheckMissingRowsState {
	@WithinYears(maxPast = 20) @DateTimeFormat(pattern = DateFormats.DateTimePickerPattern)
	var from: DateTime = _
}

trait RecheckMissingRowsDescription extends Describable[Unit] {
	override lazy val eventName = "RecheckMissingRows"
	override def describe(d: Description) {}
}
