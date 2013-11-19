package uk.ac.warwick.tabula.scheduling.commands.imports

import scala.collection.mutable.HashSet
import scala.language.implicitConversions

import org.joda.time.DateTime
import org.springframework.transaction.annotation.Transactional

import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, PersistenceTestBase}
import uk.ac.warwick.tabula.data.{MemberDaoImpl, ModuleRegistrationDaoImpl, StudentCourseDetailsDaoImpl, StudentCourseYearDetailsDaoImpl}
import uk.ac.warwick.tabula.data.model.{ModuleRegistration, StudentCourseYearKey}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.helpers.ImportRowTracker
import uk.ac.warwick.tabula.scheduling.services.{SitsAcademicYearAware, SitsAcademicYearService}
import uk.ac.warwick.tabula.services.SmallGroupService

class ImportProfilesCommandTest extends PersistenceTestBase with Mockito with Logging with SitsAcademicYearAware {
	trait Environment {
		val year = new AcademicYear(2013)

		// set up a department
		val dept = Fixtures.department("EN", "English")

		// set up a student
		val stu = Fixtures.student(universityId = "0000001", userId="student")
		session.saveOrUpdate(stu)

		val scd = stu.mostSignificantCourseDetails.get
		session.saveOrUpdate(scd)
		session.flush

		val scyd = scd.freshStudentCourseYearDetails.head

		// set up another student
		val stu2 = Fixtures.student(universityId = "0000002", userId="student2")
		session.saveOrUpdate(stu2)

		val scd2 = stu2.mostSignificantCourseDetails.get
		session.saveOrUpdate(scd2)

		session.flush
		session.clear

		val scyd2 = scd2.freshStudentCourseYearDetails.head

		// create a module
		val existingMod = Fixtures.module("ax101", "Pointless Deliberations")
		session.saveOrUpdate(existingMod)
		session.flush

		// register the student on the module
		val existingMr = new ModuleRegistration(scd, existingMod, new java.math.BigDecimal(30), new AcademicYear(2013), "A")
		session.saveOrUpdate(existingMr)
		scd.moduleRegistrations.add(existingMr)
		session.saveOrUpdate(scd)
		session.flush

		// make another module
		val newMod = Fixtures.module("zy909", "Meaningful Exchanges")
		session.saveOrUpdate(newMod)
		session.flush

		// mock required services
		val mrDao = smartMock[ModuleRegistrationDaoImpl]
		mrDao.sessionFactory = sessionFactory
		mrDao.getByUsercodesAndYear(Seq("abcde"), year) returns Seq(existingMr)

		val memberDao = new MemberDaoImpl
		memberDao.sessionFactory = sessionFactory

		val scdDao = new StudentCourseDetailsDaoImpl
		scdDao.sessionFactory = sessionFactory

		val scydDao = new StudentCourseYearDetailsDaoImpl
		scydDao.sessionFactory = sessionFactory


		val key1 = new StudentCourseYearKey(scyd.studentCourseDetails.scjCode, scyd.sceSequenceNumber)

		val key2 = new StudentCourseYearKey(scyd2.studentCourseDetails.scjCode, scyd2.sceSequenceNumber)

		val sitsAcademicYearService = smartMock[SitsAcademicYearService]
		sitsAcademicYearService.getCurrentSitsAcademicYearString returns "13/14"

		val smallGroupService = smartMock[SmallGroupService]

		val command = new ImportProfilesCommand
		command.sessionFactory = sessionFactory
		command.sitsAcademicYearService = sitsAcademicYearService
		command.moduleRegistrationDao = mrDao
		command.smallGroupService = smallGroupService
		command.memberDao = memberDao
		command.studentCourseDetailsDao = scdDao
		command.studentCourseYearDetailsDao = scydDao
	}

	@Transactional
	@Test def testDeleteOldModuleRegistrations() {
		new Environment {
			// check that if the new MR matches the old, it will not be deleted:
			command.deleteOldModuleRegistrations(Seq("abcde"), Seq(existingMr))
			scd.moduleRegistrations.contains(existingMr) should be (true)
			session.flush

			val newMr = new ModuleRegistration(scd, newMod, new java.math.BigDecimal(30), new AcademicYear(2013), "A")
			session.saveOrUpdate(newMr)
			session.flush
			scd.moduleRegistrations.add(newMr)
			session.flush

			// now check that if the new MR does not match the old, the old will be deleted:
			command.deleteOldModuleRegistrations(Seq("abcde"), Seq(newMr))
			session.flush
			scd.moduleRegistrations.contains(existingMr) should be (false)
		}
	}

	@Transactional
	@Test def testStampMissingRows() {
		new Environment {
			val tracker = new ImportRowTracker

			tracker.memberDao = memberDao
			tracker.studentCourseDetailsDao = scdDao
			tracker.studentCourseYearDetailsDao = scydDao

			tracker.universityIdsSeen.add(stu.universityId)
			tracker.scjCodesSeen.add(scd.scjCode)

			tracker.studentCourseYearDetailsSeen.add(key1)

			command.stampMissingRows(tracker, DateTime.now)
			session.flush
			session.clear

			stu.missingFromImportSince should be (null)
			scd.missingFromImportSince should be (null)
			scyd.missingFromImportSince should be (null)

			tracker.universityIdsSeen.remove(stu.universityId)

			command.stampMissingRows(tracker, DateTime.now)
			session.flush
			session.clear

			val stuRefetched = memberDao.getByUniversityIdStaleOrFresh("0000001").get

			scd.missingFromImportSince should be (null)
			scyd.missingFromImportSince should be (null)
			stuRefetched.missingFromImportSince should not be (null)

			tracker.scjCodesSeen.remove(scd.scjCode)
			command.stampMissingRows(tracker, DateTime.now)
			session.flush
			session.clear

			val stuFetched1 = memberDao.getByUniversityIdStaleOrFresh("0000001").get
			val scdFetched1 = scdDao.getByScjCodeStaleOrFresh("0000001/1").get
			val scydFetched1 = scydDao.getBySceKeyStaleOrFresh(scd, scyd.sceSequenceNumber).get

			stuFetched1.missingFromImportSince should not be (null)
			scdFetched1.missingFromImportSince should not be (null)
			scydFetched1.missingFromImportSince should be (null)

			tracker.studentCourseYearDetailsSeen.remove(key1)
			command.stampMissingRows(tracker, DateTime.now)
			session.flush
			session.clear

			val stuFetched2 = memberDao.getByUniversityIdStaleOrFresh("0000001").get
			val scdFetched2 = scdDao.getByScjCodeStaleOrFresh("0000001/1").get
			val scydFetched2 = scydDao.getBySceKeyStaleOrFresh(scd, scyd.sceSequenceNumber).get

			stuFetched2.missingFromImportSince should not be (null)
			scdFetched2.missingFromImportSince should not be (null)
			scydFetched2.missingFromImportSince should not be (null)

		}
	}

	@Transactional
	@Test def testUpdateMissingForIndividual() {
		new Environment {
			val tracker = new ImportRowTracker
			tracker.universityIdsSeen.add(stu.universityId)
			tracker.scjCodesSeen.add(scd.scjCode)

			val key = new StudentCourseYearKey(scyd.studentCourseDetails.scjCode, scyd.sceSequenceNumber)
			tracker.studentCourseYearDetailsSeen.add(key)

			command.updateMissingForIndividual(stu, tracker)
			session.flush
			session.clear

			val stuRefreshed0 = memberDao.getByUniversityIdStaleOrFresh(stu.universityId).get
			val scdRefreshed0 = scdDao.getByScjCodeStaleOrFresh(scd.scjCode).get
			val scydRefreshed0 = scydDao.getBySceKeyStaleOrFresh(scd, scyd.sceSequenceNumber).get

			stuRefreshed0.missingFromImportSince should be (null)
			scdRefreshed0.missingFromImportSince should be (null)
			scydRefreshed0.missingFromImportSince should be (null)

			tracker.universityIdsSeen.remove(stu.universityId)

			command.updateMissingForIndividual(stu, tracker)

			val stuRefreshed = memberDao.getByUniversityIdStaleOrFresh(stu.universityId).get
			val scdRefreshed = scdDao.getByScjCodeStaleOrFresh(scd.scjCode).get
			val scydRefreshed = scydDao.getBySceKeyStaleOrFresh(scd, scyd.sceSequenceNumber).get

			stuRefreshed.missingFromImportSince should not be (null)
			scdRefreshed.missingFromImportSince should be (null)
			scydRefreshed.missingFromImportSince should be (null)

			tracker.scjCodesSeen.remove(scd.scjCode)
			command.updateMissingForIndividual(stu, tracker)

			val stuRefreshed2 = memberDao.getByUniversityIdStaleOrFresh(stu.universityId).get
			val scdRefreshed2 = scdDao.getByScjCodeStaleOrFresh(scd.scjCode).get
			val scydRefreshed2 = scydDao.getBySceKeyStaleOrFresh(scd, scyd.sceSequenceNumber).get

			stuRefreshed2.missingFromImportSince should not be (null)
			scdRefreshed2.missingFromImportSince should not be (null)
			scydRefreshed2.missingFromImportSince should be (null)

			tracker.studentCourseYearDetailsSeen.remove(key)
			command.updateMissingForIndividual(stu, tracker)

			val stuRefreshed3 = memberDao.getByUniversityIdStaleOrFresh(stu.universityId).get
			val scdRefreshed3 = scdDao.getByScjCodeStaleOrFresh(scd.scjCode).get
			val scydRefreshed3 = scydDao.getBySceKeyStaleOrFresh(scd, scyd.sceSequenceNumber).get

			stuRefreshed3.missingFromImportSince should not be (null)
			scdRefreshed3.missingFromImportSince should not be (null)
			scydRefreshed3.missingFromImportSince should not be (null)

		}
	}

	@Transactional
	@Test
	def testConvertKeysToIds {
		new Environment {

			val scydFromDb = scydDao.getBySceKey(scyd.studentCourseDetails, scyd.sceSequenceNumber)
			val idForScyd = scydFromDb.get.id

			val scyd2FromDb = scydDao.getBySceKey(scyd2.studentCourseDetails, scyd2.sceSequenceNumber)
			val idForScyd2 = scyd2FromDb.get.id

			val keys = new HashSet[StudentCourseYearKey]

			keys.add(key1)
			keys.add(key2)

			val ids = command.studentCourseYearDetailsDao.convertKeysToIds(keys)

			ids.size should be (2)

			ids.contains(idForScyd) should be (true)
			ids.contains(idForScyd2) should be (true)

		}
	}
}
