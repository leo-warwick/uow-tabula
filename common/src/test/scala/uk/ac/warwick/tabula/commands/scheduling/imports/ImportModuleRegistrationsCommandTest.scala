package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.{DateTime, LocalDate}
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{ModuleRegistrationDao, StudentCourseDetailsDao}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.services.scheduling.ModuleRegistrationRow
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, PersistenceTestBase}

class ImportModuleRegistrationsCommandTest extends PersistenceTestBase with Mockito with Logging {

	trait Environment {
		val stu: StudentMember = Fixtures.student(universityId = "0000001", userId="student")
		session.saveOrUpdate(stu)

		val scd: StudentCourseDetails = stu.mostSignificantCourseDetails.get
		session.saveOrUpdate(scd)

		val mod: Module = Fixtures.module("ax101", "Pointless Deliberations")
		session.saveOrUpdate(mod)
		session.flush()

		val mr = new ModuleRegistration(scd, mod, new JBigDecimal(30), new AcademicYear(2013), "A")
		session.saveOrUpdate(mr)
		session.flush()

		val cats = new JBigDecimal(30)
		val year = new AcademicYear(2013)
		val occurrence = "O"

		val madService: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]
		madService.getModuleBySitsCode("AX101-30") returns Some(mod)

		val modRegRow1 = new ModuleRegistrationRow(scd.scjCode, "AX101-30", cats, "A", "C", occurrence, "13/14",
			Some(new JBigDecimal("90.0")), "A", Some(new JBigDecimal("90.0")), "A")
		val modRegRow2 = new ModuleRegistrationRow(scd.scjCode, "AX101-30", cats, "A", "O", occurrence, "13/14",
			Some(new JBigDecimal("50.0")), "C", Some(new JBigDecimal("50.0")), "C")

		val scdDao: StudentCourseDetailsDao = smartMock[StudentCourseDetailsDao]
		scdDao.getByScjCode("0000001/1") returns Some(scd)

		val mrDao: ModuleRegistrationDao = smartMock[ModuleRegistrationDao]
		mrDao.getByNotionalKey(scd, mod, cats, year, occurrence) returns Some(mr)
	}

	@Transactional
	@Test def captureModuleRegistration() {
		new Environment {

			// apply the command
			val command = new ImportModuleRegistrationsCommand(scd, Seq(modRegRow1), Set(mod))
			command.moduleAndDepartmentService = madService
			command.moduleRegistrationDao = mrDao

			val newModRegs: Seq[ModuleRegistration] = command.applyInternal()

			// check results
			newModRegs.size should be (1)
			newModRegs.head.academicYear should be (new AcademicYear(2013))
			newModRegs.head.assessmentGroup should be ("A")
			newModRegs.head.module should be (mod)
			newModRegs.head.cats should be (cats)
			newModRegs.head.occurrence should be (occurrence)
			newModRegs.head.selectionStatus.description should be ("Core")
			newModRegs.head.studentCourseDetails should be (scd)
			newModRegs.head.lastUpdatedDate.getDayOfMonth should be (LocalDate.now.getDayOfMonth)

			// now reset the last updated date to 10 days ago:
			val tenDaysAgo: DateTime = DateTime.now.minusDays(10)
			newModRegs.head.lastUpdatedDate = tenDaysAgo
			newModRegs.head.lastUpdatedDate.getDayOfMonth should be (tenDaysAgo.getDayOfMonth)
			session.flush()

			// now re-import the same mod reg - the lastupdateddate shouldn't change
			val command2 = new ImportModuleRegistrationsCommand(scd, Seq(modRegRow1), Set(mod))
			command2.moduleAndDepartmentService = madService
			command2.moduleRegistrationDao = mrDao


			val newModRegs2: Seq[ModuleRegistration] = command2.applyInternal()
			newModRegs2.head.lastUpdatedDate.getDayOfMonth should be (tenDaysAgo.getDayOfMonth)

			// try just changing the selection status:
			val command3 = new ImportModuleRegistrationsCommand(scd, Seq(modRegRow2), Set(mod))
			command3.moduleAndDepartmentService = madService
			command3.moduleRegistrationDao = mrDao

			val newModRegs3: Seq[ModuleRegistration] = command3.applyInternal()
			newModRegs3.head.selectionStatus.description should be ("Option")
		}
	}
}
