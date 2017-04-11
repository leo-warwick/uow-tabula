package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.JBigDecimal

trait ModuleRegistrationDaoComponent {
	val moduleRegistrationDao: ModuleRegistrationDao
}

trait AutowiringModuleRegistrationDaoComponent extends ModuleRegistrationDaoComponent {
	val moduleRegistrationDao: ModuleRegistrationDao = Wire[ModuleRegistrationDao]
}

trait ModuleRegistrationDao {
	def saveOrUpdate(moduleRegistration: ModuleRegistration): Unit
	def saveOrUpdate(coreRequiredModule: CoreRequiredModule): Unit
	def delete(coreRequiredModule: CoreRequiredModule): Unit
	def getByNotionalKey(
		studentCourseDetails: StudentCourseDetails,
		module: Module,
		cats: JBigDecimal,
		academicYear: AcademicYear,
		occurrence: String
	): Option[ModuleRegistration]
	def getByUsercodesAndYear(usercodes: Seq[String], academicYear: AcademicYear) : Seq[ModuleRegistration]
	def findCoreRequiredModules(route: Route, academicYear: AcademicYear, yearOfStudy: Int): Seq[CoreRequiredModule]
}

@Repository
class ModuleRegistrationDaoImpl extends ModuleRegistrationDao with Daoisms {

	def saveOrUpdate(moduleRegistration: ModuleRegistration): Unit = session.saveOrUpdate(moduleRegistration)

	def saveOrUpdate(coreRequiredModule: CoreRequiredModule): Unit = session.saveOrUpdate(coreRequiredModule)

	def delete(coreRequiredModule: CoreRequiredModule): Unit = session.delete(coreRequiredModule)

	def getByNotionalKey(
		studentCourseDetails: StudentCourseDetails,
		module: Module,
		cats: JBigDecimal,
		academicYear: AcademicYear,
		occurrence: String
	): Option[ModuleRegistration] =
		session.newCriteria[ModuleRegistration]
				.add(is("studentCourseDetails", studentCourseDetails))
				.add(is("module", module))
				.add(is("academicYear", academicYear))
				.add(is("cats", cats))
				.add(is("occurrence", occurrence))
				.uniqueResult

	def getByUsercodesAndYear(userCodes: Seq[String], academicYear: AcademicYear) : Seq[ModuleRegistration] = {
		val query = session.newQuery[ModuleRegistration]("""
				select distinct mr
					from ModuleRegistration mr
					where academicYear = :academicYear
					and studentCourseDetails.missingFromImportSince is null
					and studentCourseDetails.student.userId in :usercodes
				""")
					.setString("academicYear", academicYear.getStoreValue.toString)

		query.setParameterList("usercodes", userCodes)
					.seq
	}

	def findCoreRequiredModules(route: Route, academicYear: AcademicYear, yearOfStudy: Int): Seq[CoreRequiredModule] = {
		session.newCriteria[CoreRequiredModule]
		  .add(is("route", route))
			.add(is("academicYear", academicYear))
			.add(is("yearOfStudy", yearOfStudy))
		  .seq
	}
}
