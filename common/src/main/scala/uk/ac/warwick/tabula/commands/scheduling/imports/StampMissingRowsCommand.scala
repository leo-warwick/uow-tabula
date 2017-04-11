package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model.StudentCourseYearKey
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.scheduling.{AutowiringProfileImporterComponent, ProfileImporterComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import Transactions._

import scala.collection.JavaConverters._

object StampMissingRowsCommand {
	def apply() =
		new StampMissingRowsCommandInternal
			with AutowiringMemberDaoComponent
			with AutowiringStudentCourseYearDetailsDaoComponent
			with AutowiringStudentCourseDetailsDaoComponent
			with AutowiringProfileImporterComponent
			with ComposableCommandWithoutTransaction[Unit]
			with StampMissingRowsDescription
			with MissingRowsPermissions
}


class StampMissingRowsCommandInternal extends CommandInternal[Unit] with Logging with Daoisms with ChecksStudentsInSits {

	self: MemberDaoComponent with StudentCourseYearDetailsDaoComponent with StudentCourseDetailsDaoComponent
		with ProfileImporterComponent =>


	override def applyInternal(): Unit = {

		val allUniversityIDs = transactional() { memberDao.getFreshUniversityIds.toSet }
		logger.info(s"${allUniversityIDs.size} students to fetch from SITS")

		val studentsFound = checkSitsForStudents(allUniversityIDs)

		if (studentsFound.universityIdsSeen.isEmpty) {
			throw new UnsupportedOperationException("Could not find any students, so not marking all as missing")
		}

		if (studentsFound.scjCodesSeen.isEmpty) {
			throw new UnsupportedOperationException("Could not find any SCJ codes, so not marking all as missing")
		}

		if (studentsFound.studentCourseYearKeysSeen.isEmpty) {
			throw new UnsupportedOperationException("Could not find any year enrolments, so not marking all as missing")
		}

		val newStaleUniversityIds: Seq[String] = {
			(allUniversityIDs -- studentsFound.universityIdsSeen).toSeq
		}

		val newStaleScjCodes: Seq[String] = {
			val allFreshScjCodes = transactional() { studentCourseDetailsDao.getFreshScjCodes.toSet }
			(allFreshScjCodes -- studentsFound.scjCodesSeen).toSeq
		}

		val newStaleScydIds: Seq[String] = {
			val scydIdsSeen = transactional() { studentCourseYearDetailsDao.convertKeysToIds(studentsFound.studentCourseYearKeysSeen.toSeq) }
			val allFreshIds = transactional() { studentCourseYearDetailsDao.getFreshIds.toSet }
			transactional() { (allFreshIds -- scydIdsSeen).toSeq }
		}

		logger.warn(s"Timestamping ${newStaleUniversityIds.size} missing students")
		transactional() { memberDao.stampMissingFromImport(newStaleUniversityIds, DateTime.now) }

		logger.warn(s"Timestamping ${newStaleScjCodes.size} missing studentCourseDetails")
		transactional() { studentCourseDetailsDao.stampMissingFromImport(newStaleScjCodes, DateTime.now) }

		logger.warn(s"Timestamping ${newStaleScydIds.size} missing studentCourseYearDetails")
		transactional() { studentCourseYearDetailsDao.stampMissingFromImport(newStaleScydIds, DateTime.now) }

		transactional() { session.flush() }
		transactional() { session.clear() }

		val newFreshUniIds = transactional() { memberDao.getFreshUniversityIds.toSet }
		val uniIDsStillNotMarked = newStaleUniversityIds.toSet.intersect(newFreshUniIds)
		if (uniIDsStillNotMarked.nonEmpty) {
			logger.error(s"There are still stale IDs that weren't marked as missing (${uniIDsStillNotMarked.size} total); here are a few: ${uniIDsStillNotMarked.take(5).mkString(", ")}")
		} else {
			logger.info(s"All ${newStaleUniversityIds.size} Uni IDs marked correctly")
		}
	}

}

trait MissingRowsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}
}

trait StampMissingRowsDescription extends Describable[Unit] {
	override lazy val eventName = "StampMissingRows"
	override def describe(d: Description) {
	}
}

trait ChecksStudentsInSits {

	self: ProfileImporterComponent with Logging =>

	case class StudentsFound(
		universityIdsSeen: Set[String],
		scjCodesSeen: Set[String],
		studentCourseYearKeysSeen: Set[StudentCourseYearKey]
	)

	def checkSitsForStudents(universityIds: Set[String]): StudentsFound = {
		val parsedSitsRows = universityIds.grouped(Daoisms.MaxInClauseCount).zipWithIndex.map { case (ids, groupCount) =>
			val sitsRows = profileImporter.multipleStudentInformationQuery.executeByNamedParam(
				Map("universityIds" -> ids.toSeq.asJava).asJava
			).asScala

			logger.info(s"${(groupCount + 1) * Daoisms.MaxInClauseCount} students requested from SITS; ${sitsRows.size} rows found")
			(
				sitsRows.map(_.universityId.getOrElse("")).distinct,
				sitsRows.map(_.scjCode).distinct,
				sitsRows.map(row => new StudentCourseYearKey(row.scjCode, row.sceSequenceNumber)).distinct
				)
		}.toSeq


		val universityIdsSeen = parsedSitsRows.flatMap(_._1)
		val scjCodesSeen = parsedSitsRows.flatMap(_._2)
		val studentCourseYearKeysSeen = parsedSitsRows.flatMap(_._3)

		StudentsFound(universityIdsSeen.toSet, scjCodesSeen.toSet, studentCourseYearKeysSeen.toSet)
	}

}
