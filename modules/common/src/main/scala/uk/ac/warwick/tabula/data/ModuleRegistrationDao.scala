package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.ModuleRegistration
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Module

trait ModuleRegistrationDao {
	def saveOrUpdate(moduleRegistration: ModuleRegistration)
	def delete(moduleRegistration: ModuleRegistration)
	def getByNotionalKey(
		studentCourseDetails: StudentCourseDetails,
		module: Module,
		cats: java.math.BigDecimal,
		academicYear: AcademicYear,
		occurrence: String
	): Option[ModuleRegistration]
	def getByUsercodesAndYear(usercodes: Seq[String], academicYear: AcademicYear) : Seq[ModuleRegistration]
}

@Repository
class ModuleRegistrationDaoImpl extends ModuleRegistrationDao with Daoisms {

	def saveOrUpdate(moduleRegistration: ModuleRegistration) = session.saveOrUpdate(moduleRegistration)
	def delete(moduleRegistration: ModuleRegistration) = session.delete(moduleRegistration)

	def getByNotionalKey(
		studentCourseDetails: StudentCourseDetails,
		module: Module,
		cats: java.math.BigDecimal,
		academicYear: AcademicYear,
		occurrence: String
	) =
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

	def evict(moduleRegistration: ModuleRegistration) = session.evict(moduleRegistration)
}
