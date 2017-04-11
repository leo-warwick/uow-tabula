package uk.ac.warwick.tabula.commands.scheduling.imports

import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.JavaImports.JBigDecimal
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{StudentCourseDetailsDaoImpl, StudentCourseYearDetailsDaoImpl, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.scheduling.{SitsAcademicYearAware, SitsAcademicYearService}
import uk.ac.warwick.tabula.services.{ModuleRegistrationServiceImpl, SmallGroupService}

import scala.language.implicitConversions

class ImportProfilesCommandTest extends PersistenceTestBase with Mockito with Logging with SitsAcademicYearAware {
	trait Environment {
		val year = new AcademicYear(2013)

		// set up a department
		val dept: Department = Fixtures.department("EN", "English")

		// set up a student
		var stu: StudentMember = Fixtures.student(universityId = "0000001", userId="student")
		session.saveOrUpdate(stu)

		var scd: StudentCourseDetails = stu.mostSignificantCourseDetails.get
		session.saveOrUpdate(scd)
		session.flush()

		var scyd: StudentCourseYearDetails = scd.freshStudentCourseYearDetails.head

		// set up another student
		val stu2: StudentMember = Fixtures.student(universityId = "0000002", userId="student2")
		session.saveOrUpdate(stu2)

		val scd2: StudentCourseDetails = stu2.mostSignificantCourseDetails.get
		session.saveOrUpdate(scd2)

		session.flush()
		session.clear()

		var scyd2: StudentCourseYearDetails = scd2.freshStudentCourseYearDetails.head

		// create a module
		val existingMod: Module = Fixtures.module("ax101", "Pointless Deliberations")
		session.saveOrUpdate(existingMod)
		session.flush()

		// register the student on the module
		val existingMr = new ModuleRegistration(scd, existingMod, new JBigDecimal(30), new AcademicYear(2013), "A")
		session.saveOrUpdate(existingMr)
		scd.addModuleRegistration(existingMr)
		session.saveOrUpdate(scd)
		session.flush()

		// make another module
		val newMod: Module = Fixtures.module("zy909", "Meaningful Exchanges")
		session.saveOrUpdate(newMod)
		session.flush()

		// mock required services
		val mrService: ModuleRegistrationServiceImpl = smartMock[ModuleRegistrationServiceImpl]
		mrService.getByUsercodesAndYear(Seq("abcde"), year) returns Seq(existingMr)

		val memberDao = new AutowiringMemberDaoImpl
		memberDao.sessionFactory = sessionFactory

		val scdDao = new StudentCourseDetailsDaoImpl
		scdDao.sessionFactory = sessionFactory

		val scydDao = new StudentCourseYearDetailsDaoImpl
		scydDao.sessionFactory = sessionFactory


		val key1 = new StudentCourseYearKey(scyd.studentCourseDetails.scjCode, scyd.sceSequenceNumber)

		val key2 = new StudentCourseYearKey(scyd2.studentCourseDetails.scjCode, scyd2.sceSequenceNumber)

		val sitsAcademicYearService: SitsAcademicYearService = smartMock[SitsAcademicYearService]
		sitsAcademicYearService.getCurrentSitsAcademicYearString returns "13/14"

		val smallGroupService: SmallGroupService = smartMock[SmallGroupService]

		val command = new ImportProfilesCommand
		command.features = new FeaturesImpl
		command.sessionFactory = sessionFactory
		command.sitsAcademicYearService = sitsAcademicYearService
		command.moduleRegistrationService = mrService
		command.smallGroupService = smallGroupService
		command.memberDao = memberDao
		command.studentCourseDetailsDao = scdDao
		command.studentCourseYearDetailsDao = scydDao
	}

	@Transactional
	@Test def testDeleteOldModuleRegistrations() {
		new Environment {
			command.features.autoGroupDeregistration = true

			// check that if the new MR matches the old, it will not be deleted:
			command.deleteOldModuleRegistrations(Seq("abcde"), Seq(existingMr))
			scd.moduleRegistrations.contains(existingMr) should be (true)
			session.flush()

			val newMr = new ModuleRegistration(scd, newMod, new JBigDecimal(30), new AcademicYear(2013), "A")
			session.saveOrUpdate(newMr)
			session.flush()
			scd.addModuleRegistration(newMr)
			session.flush()

			// now check that if the new MR does not match the old, the old will be deleted:
			command.deleteOldModuleRegistrations(Seq("abcde"), Seq(newMr))
			session.flush()
			scd.moduleRegistrations.contains(existingMr) should be (false)
		}
	}

	@Transactional
	@Test
	def testConvertKeysToIds(): Unit = {
		new Environment {

			scyd = scydDao.getBySceKey(scyd.studentCourseDetails, scyd.sceSequenceNumber).get
			val idForScyd: String = scyd.id

			scyd2 = scydDao.getBySceKey(scyd2.studentCourseDetails, scyd2.sceSequenceNumber).get
			val idForScyd2: String = scyd2.id

			val keys = Seq(key1, key2)

			val ids: Seq[String] = command.studentCourseYearDetailsDao.convertKeysToIds(keys)

			ids.size should be (2)

			ids.contains(idForScyd) should be (true)
			ids.contains(idForScyd2) should be (true)

		}
	}
}
