package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.data.model.{ModuleRegistration, ModuleSelectionStatus, StudentCourseDetails}
import uk.ac.warwick.tabula.data.{ModuleRegistrationDao, ModuleRegistrationDaoComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

class ModuleRegistrationServiceTest extends TestBase with Mockito {

	val mockModuleRegistrationDao: ModuleRegistrationDao = smartMock[ModuleRegistrationDao]

	trait Fixture {
		val service = new AbstractModuleRegistrationService with ModuleRegistrationDaoComponent {
			val moduleRegistrationDao: ModuleRegistrationDao = mockModuleRegistrationDao
		}
	}

	@Test
	def weightedMeanYearMark(): Unit = {
		val module1 = Fixtures.module("xx101")
		val module2 = Fixtures.module("xx102")
		val module3 = Fixtures.module("xx103")
		val module4 = Fixtures.module("xx104")
		val module5 = Fixtures.module("xx105")
		val module6 = Fixtures.module("xx106")
		new Fixture {
			val moduleRegistrations = Seq(
				Fixtures.moduleRegistration(null, module1, BigDecimal(30).underlying, null, agreedMark = BigDecimal(100)),
				Fixtures.moduleRegistration(null, module2, BigDecimal(45).underlying, null, agreedMark = BigDecimal(58)),
				Fixtures.moduleRegistration(null, module3, BigDecimal(15).underlying, null, agreedMark = BigDecimal(30)),
				Fixtures.moduleRegistration(null, module4, BigDecimal(7.5).underlying, null, agreedMark = BigDecimal(0)),
				Fixtures.moduleRegistration(null, module5, BigDecimal(7.5).underlying, null, agreedMark = BigDecimal(97)),
				Fixtures.moduleRegistration(null, module6, BigDecimal(15).underlying, null, agreedMark = BigDecimal(64))
			)
			val result: Either[String, BigDecimal] = service.weightedMeanYearMark(moduleRegistrations, Map())
			result.isRight should be {true}
			result.right.get.scale should be (1)
			result.right.get.doubleValue() should be (64.6)

			val moduleRegistrationsWithMissingAgreedMark = Seq(
				Fixtures.moduleRegistration(null, module1, BigDecimal(30).underlying, null, agreedMark = BigDecimal(100)),
				Fixtures.moduleRegistration(null, module2, BigDecimal(45).underlying, null, agreedMark = BigDecimal(58)),
				Fixtures.moduleRegistration(null, module3, BigDecimal(15).underlying, null, agreedMark = BigDecimal(30)),
				Fixtures.moduleRegistration(null, module4, BigDecimal(7.5).underlying, null, agreedMark = null),
				Fixtures.moduleRegistration(null, module5, BigDecimal(7.5).underlying, null, agreedMark = BigDecimal(97)),
				Fixtures.moduleRegistration(null, module6, BigDecimal(15).underlying, null, agreedMark = BigDecimal(64))
			)
			val noResult: Either[String, BigDecimal] = service.weightedMeanYearMark(moduleRegistrationsWithMissingAgreedMark, Map())
			noResult.isRight should be {false}

			val moduleRegistrationsWithOverriddenMark = Seq(
				Fixtures.moduleRegistration(null, module1, BigDecimal(30).underlying, null, agreedMark = BigDecimal(100)),
				Fixtures.moduleRegistration(null, module2, BigDecimal(45).underlying, null, agreedMark = BigDecimal(58)),
				Fixtures.moduleRegistration(null, module3, BigDecimal(15).underlying, null, agreedMark = BigDecimal(30)),
				Fixtures.moduleRegistration(null, module4, BigDecimal(7.5).underlying, null, agreedMark = null),
				Fixtures.moduleRegistration(null, module5, BigDecimal(7.5).underlying, null, agreedMark = BigDecimal(97)),
				Fixtures.moduleRegistration(null, module6, BigDecimal(15).underlying, null, agreedMark = BigDecimal(64))
			)
			val overriddenResult: Either[String, BigDecimal] = service.weightedMeanYearMark(moduleRegistrationsWithOverriddenMark, Map(module4 -> 0))
			overriddenResult.isRight should be {true}
			overriddenResult.right.get.scale should be (1)
			overriddenResult.right.get.doubleValue() should be (64.6)
		}
	}

	@Test
	def overcattedModuleSubsets(): Unit = {
		new Fixture {
			val scd: StudentCourseDetails = Fixtures.student().mostSignificantCourse
			val academicYear = AcademicYear(2014)
			scd.latestStudentCourseYearDetails.academicYear = academicYear
			val moduleRegistrations = Seq(
				Fixtures.moduleRegistration(scd, Fixtures.module("ch3c5"), BigDecimal(6).underlying, academicYear, "", BigDecimal(78), ModuleSelectionStatus.Core),
				Fixtures.moduleRegistration(scd, Fixtures.module("ch3f2"), BigDecimal(15).underlying, academicYear, "", BigDecimal(79), ModuleSelectionStatus.Core),
				Fixtures.moduleRegistration(scd, Fixtures.module("ch3f3"), BigDecimal(30).underlying, academicYear, "", BigDecimal(86), ModuleSelectionStatus.Core),
				Fixtures.moduleRegistration(scd, Fixtures.module("ch3c3"), BigDecimal(30).underlying, academicYear, "", BigDecimal(70), ModuleSelectionStatus.Core),
				Fixtures.moduleRegistration(scd, Fixtures.module("ch3f4"), BigDecimal(15).underlying, academicYear, "", BigDecimal(79), ModuleSelectionStatus.Option),
				Fixtures.moduleRegistration(scd, Fixtures.module("ch3f6"), BigDecimal(15).underlying, academicYear, "", BigDecimal(65), ModuleSelectionStatus.Option),
				Fixtures.moduleRegistration(scd, Fixtures.module("ch3f7"), BigDecimal(15).underlying, academicYear, "", BigDecimal(69), ModuleSelectionStatus.Option),
				Fixtures.moduleRegistration(scd, Fixtures.module("ch3f8"), BigDecimal(15).underlying, academicYear, "", BigDecimal(68), ModuleSelectionStatus.Option)
			)
			moduleRegistrations.foreach(scd.addModuleRegistration)
			val result: Seq[(BigDecimal, Seq[ModuleRegistration])] = service.overcattedModuleSubsets(scd.latestStudentCourseYearDetails.toExamGridEntityYear, Map(), 120, Seq()) // TODO check route rules
			// There are 81 CATS of core modules, leaving 39 to reach the normal load of 120
			// All the options are 15 CATS, so there are 5 combinations of modules that are valid (4 with 3 in each and 1 with 4)
			result.size should be (5)
		}
	}

}
