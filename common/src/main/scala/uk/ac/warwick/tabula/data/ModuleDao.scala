package uk.ac.warwick.tabula.data

import org.hibernate.criterion.Restrictions._
import org.hibernate.criterion.{Order, Projections}
import org.joda.time.DateTime.now
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._

trait ModuleDao {
	def allModules: Seq[Module]
	def saveOrUpdate(module: Module)
	def saveOrUpdate(teachingInfo: ModuleTeachingInformation)
	def delete(teachingInfo: ModuleTeachingInformation)
	def getByCode(code: String): Option[Module]
	def getAllByCodes(codes: Seq[String]): Seq[Module]
	def getTeachingInformationByModuleCodeAndDepartmentCode(moduleCode: String, departmentCode: String): Option[ModuleTeachingInformation]
	def getById(id: String): Option[Module]
	def stampMissingFromImport(newStaleModuleCodes: Seq[String])
	def hasAssignments(module: Module): Boolean
	def findModulesNamedLike(query: String): Seq[Module]
	def findByRoutes(routes: Seq[Route], academicYear: AcademicYear): Seq[Module]
	def findByYearOfStudy(department: Department, yearsOfStudy: Seq[Integer], academicYear: AcademicYear): Seq[Module]
	def findByRouteYearAcademicYear(route: Route, yearOfStudy: Int, academicYear: AcademicYear): Seq[Module]
}

@Repository
class ModuleDaoImpl extends ModuleDao with Daoisms {

	def allModules: Seq[Module] =
		session.newCriteria[Module]
			.addOrder(Order.asc("code"))
			.seq
			.distinct

	def saveOrUpdate(module: Module): Unit = session.saveOrUpdate(module)
	def saveOrUpdate(teachingInfo: ModuleTeachingInformation): Unit = session.saveOrUpdate(teachingInfo)
	def delete(teachingInfo: ModuleTeachingInformation): Unit = session.delete(teachingInfo)

	def getByCode(code: String): Option[Module] =
		session.newQuery[Module]("from Module m where code = :code").setString("code", code).uniqueResult

	def getAllByCodes(codes: Seq[String]): Seq[Module] = {
		safeInSeq(() => { session.newCriteria[Module] }, "code", codes)
	}

	def getTeachingInformationByModuleCodeAndDepartmentCode(moduleCode: String, departmentCode: String): Option[ModuleTeachingInformation] =
		session.newCriteria[ModuleTeachingInformation]
			.createAlias("module", "module")
			.createAlias("department", "department")
			.add(is("module.code", moduleCode.toLowerCase))
			.add(is("department.code", departmentCode.toLowerCase))
			.uniqueResult

	def getById(id: String): Option[Module] = getById[Module](id)

	def stampMissingFromImport(staleModuleCodes: Seq[String]): Unit = {
		staleModuleCodes.grouped(Daoisms.MaxInClauseCount).foreach { staleCodes =>
			val sqlString = """
				update
					Module
				set
					missingFromImportSince = :now
				where
					code in (:staleModuleCodes)
		 		and
		 			missingFromImportSince is null
			"""

			session.newQuery(sqlString)
				.setParameter("now", now)
				.setParameterList("staleModuleCodes", staleCodes)
				.executeUpdate()
		}
	}

	def hasAssignments(module: Module): Boolean = {
		session.newCriteria[Assignment]
			.add(is("module", module))
			.count.intValue > 0
	}

	def findModulesNamedLike(query: String): Seq[Module] = {
		val maxResults = 20
		val modulesByCode = session.newCriteria[Module]
			.add(like("code", s"%${query.toLowerCase}%").ignoreCase)
			.addOrder(Order.asc("code"))
			.setMaxResults(maxResults).seq

		if (modulesByCode.size < maxResults) {
			val modulesByName = session.newCriteria[Module]
				.add(like("name", s"%${query.toLowerCase}%").ignoreCase)
				.addOrder(Order.asc("code"))
				.setMaxResults(maxResults - modulesByCode.size).seq
			(modulesByCode ++ modulesByName).distinct
		} else {
			modulesByCode
		}
	}

	def findByRoutes(routes: Seq[Route], academicYear: AcademicYear): Seq[Module] = {
		if (routes.isEmpty) {
			Seq()
		} else {
			session.newQuery[Module](
				"""
					select module from
						Module as module,
						ModuleRegistration as registration,
						StudentCourseDetails as scd,
						Route as route,
						StudentMember as student
					where
						module.id = registration.module.id
						and scd = registration.studentCourseDetails
						and scd.currentRoute.id = route.id
						and student.mostSignificantCourse = scd
						and route in (:routes)
						and registration.academicYear = :academicYear
				""")
				.setParameterList("routes", routes)
				.setParameter("academicYear", academicYear)
				.seq
		}
	}

	def findByYearOfStudy(department: Department, yearsOfStudy: Seq[Integer], academicYear: AcademicYear): Seq[Module] = {
		if (yearsOfStudy.isEmpty) {
			Seq()
		} else {
			session.newQuery[Module](
				"""
					select module from
						Module as module,
						ModuleRegistration as registration,
						StudentCourseDetails as scd,
						StudentCourseYearDetails as scyd,
						StudentMember as student,
						Route as route
					where
						module.id = registration.module.id
						and scd = registration.studentCourseDetails
						and scyd.studentCourseDetails = scd
						and student.mostSignificantCourse = scd
						and scd.currentRoute.id = route.id
						and scyd.yearOfStudy in (:yearsOfStudy)
						and registration.academicYear = :academicYear
						and scyd.academicYear = :academicYear
						and (
							student.homeDepartment = :department
			 				or scd.department = :department
							or scyd.enrolmentDepartment = :department
			 				or route.adminDepartment = :department
						)
				""")
				.setParameter("department", department)
				.setParameterList("yearsOfStudy", yearsOfStudy)
				.setParameter("academicYear", academicYear)
				.seq
		}
	}

	def findByRouteYearAcademicYear(route: Route, yearOfStudy: Int, academicYear: AcademicYear): Seq[Module] = {
		session.newQuery[Module](
			"""
					select distinct module from
						Module as module,
						ModuleRegistration as registration,
						StudentCourseDetails as scd,
						Route as route,
						StudentMember as student,
						StudentCourseYearDetails as scyd
					where
						module.id = registration.module.id
						and scd = registration.studentCourseDetails
						and scd.currentRoute.id = route.id
						and student.mostSignificantCourse = scd
						and scyd.studentCourseDetails = scd
						and route = :route
						and registration.academicYear = :academicYear
						and scyd.academicYear = :academicYear
						and scyd.yearOfStudy = :yearOfStudy
			""")
			.setParameter("route", route)
			.setParameter("yearOfStudy", yearOfStudy)
			.setParameter("academicYear", academicYear)
			.seq
	}

}