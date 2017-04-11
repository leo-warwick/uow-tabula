package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.{AutowiringCourseDaoComponent, AutowiringRouteDaoComponent, CourseDaoComponent, RouteDaoComponent}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.roles.RouteManagerRoleDefinition
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}

import scala.collection.JavaConverters._
import scala.reflect._

/**
 * Handles data about courses and routes
 */
trait CourseAndRouteServiceComponent {
	def courseAndRouteService: CourseAndRouteService
}

trait AutowiringCourseAndRouteServiceComponent extends CourseAndRouteServiceComponent {
	var courseAndRouteService: CourseAndRouteService = Wire[CourseAndRouteService]
}

trait CourseAndRouteService extends RouteDaoComponent with CourseDaoComponent with SecurityServiceComponent with PermissionsServiceComponent with ModuleAndDepartmentServiceComponent {

	def save(route: Route): Unit
	def saveOrUpdate(course: Course): Unit
	def saveOrUpdate(teachingInfo: RouteTeachingInformation): Unit
	def saveOrUpdate(courseYearWeighting: CourseYearWeighting): Unit

	def delete(teachingInfo: RouteTeachingInformation): Unit
	def delete(courseYearWeighting: CourseYearWeighting): Unit

	def allRoutes: Seq[Route]
	def getRouteById(id: String): Option[Route]
	def getRouteByCode(code: String): Option[Route]
	def getRoutesByCodes(codes: Seq[String]): Seq[Route]
	def findRoutesInDepartment(department: Department): Seq[Route]
	def findRoutesNamedLike(query: String): Seq[Route]
	def stampMissingRoutes(dept: Department, seenCodes: Seq[String]): Int
	def routesWithPermission(user: CurrentUser, permission: Permission): Set[Route]
	def routesWithPermission(user: CurrentUser, permission: Permission, dept: Department): Set[Route]
	def routesInDepartmentsWithPermission(user: CurrentUser, permission: Permission): Set[Route]
	def routesInDepartmentWithPermission(user: CurrentUser, permission: Permission, dept: Department): Set[Route]

	def getCourseByCode(code: String): Option[Course]
	def findCoursesInDepartment(department: Department): Seq[Course]

	def getRouteTeachingInformation(routeCode: String, departmentCode: String): Option[RouteTeachingInformation]

	def addRouteManager(route: Route, owner: String): Unit
	def removeRouteManager(route: Route, owner: String): Unit

	def getCourseYearWeighting(courseCode: String, academicYear: AcademicYear, yearOfStudy: YearOfStudy): Option[CourseYearWeighting]
	def findAllCourseYearWeightings(courses: Seq[Course], academicYear: AcademicYear): Seq[CourseYearWeighting]

}

abstract class AbstractCourseAndRouteService extends CourseAndRouteService {
	self: RouteDaoComponent with CourseDaoComponent =>

	def allRoutes: Seq[Route] = transactional(readOnly = true) { routeDao.allRoutes }

	def save(route: Route): Unit = routeDao.saveOrUpdate(route)
	def getRouteById(id: String): Option[Route] = transactional(readOnly = true) { routeDao.getById(id) }

	def getRouteByCode(code: String): Option[Route] = code.maybeText.flatMap {
		rcode => transactional(readOnly = true) { routeDao.getByCode(rcode.toLowerCase) }
	}

	def getRoutesByCodes(codes: Seq[String]): Seq[Route] = transactional(readOnly = true) {
		routeDao.getAllByCodes(codes.map(_.toLowerCase))
	}

	def getCourseByCode(code: String): Option[Course] = code.maybeText.flatMap {
		ccode => courseDao.getByCode(ccode)
	}

	def saveOrUpdate(course: Course): Unit =
		courseDao.saveOrUpdate(course)

	def findCoursesInDepartment(department: Department): Seq[Course] =
		courseDao.findByDepartment(department)

	def findRoutesInDepartment(department: Department): Seq[Route] =
		routeDao.findByDepartment(department)

	def saveOrUpdate(teachingInfo: RouteTeachingInformation): Unit = transactional() {
		routeDao.saveOrUpdate(teachingInfo)
	}

	def delete(teachingInfo: RouteTeachingInformation): Unit = transactional() {
		routeDao.delete(teachingInfo)
	}

	def findRoutesNamedLike(query: String): Seq[Route] =
		routeDao.findRoutesNamedLike(query)

	def getRouteTeachingInformation(routeCode: String, departmentCode: String): Option[RouteTeachingInformation] = transactional(readOnly = true) {
		routeDao.getTeachingInformationByRouteCodeAndDepartmentCode(routeCode, departmentCode)
	}

	def stampMissingRoutes(dept: Department, seenCodes: Seq[String]): Int = transactional() {
		routeDao.stampMissingRows(dept, seenCodes)
	}

	def routesWithPermission(user: CurrentUser, permission: Permission): Set[Route] =
		permissionsService.getAllPermissionDefinitionsFor[Route](user, permission)
			.filter { route => securityService.can(user, permission, route) }

	def routesWithPermission(user: CurrentUser, permission: Permission, dept: Department): Set[Route] =
		routesWithPermission(user, permission).filter { _.adminDepartment == dept }

	def routesInDepartmentsWithPermission(user: CurrentUser, permission: Permission): Set[Route] = {
		moduleAndDepartmentService.departmentsWithPermission(user, permission) flatMap (dept => dept.routes.asScala)
	}
	def routesInDepartmentWithPermission(user: CurrentUser, permission: Permission, dept: Department): Set[Route] = {
		if (moduleAndDepartmentService.departmentsWithPermission(user, permission) contains dept) dept.routes.asScala.toSet else Set()
	}

	def addRouteManager(route: Route, owner: String): Unit = transactional() {
		val role = permissionsService.getOrCreateGrantedRole(route, RouteManagerRoleDefinition)
		role.users.knownType.addUserId(owner)
		permissionsService.saveOrUpdate(role)
		permissionsService.clearCachesForUser((owner, classTag[Route]))
	}

	def removeRouteManager(route: Route, owner: String): Unit = transactional() {
		val role = permissionsService.getOrCreateGrantedRole(route, RouteManagerRoleDefinition)
		role.users.knownType.removeUserId(owner)
		permissionsService.saveOrUpdate(role)
		permissionsService.clearCachesForUser((owner, classTag[Route]))
	}

	def getCourseYearWeighting(courseCode: String, academicYear: AcademicYear, yearOfStudy: YearOfStudy): Option[CourseYearWeighting] =
		courseDao.getCourseYearWeighting(courseCode, academicYear, yearOfStudy)

	def findAllCourseYearWeightings(courses: Seq[Course], academicYear: AcademicYear): Seq[CourseYearWeighting] =
		courseDao.findAllCourseYearWeightings(courses, academicYear)

	def saveOrUpdate(courseYearWeighting: CourseYearWeighting): Unit =
		courseDao.saveOrUpdate(courseYearWeighting)

	def delete(courseYearWeighting: CourseYearWeighting): Unit =
		courseDao.delete(courseYearWeighting)

}

@Service("courseAndRouteService")
class CourseAndRouteServiceImpl
	extends AbstractCourseAndRouteService
		with AutowiringRouteDaoComponent
		with AutowiringCourseDaoComponent
		with AutowiringSecurityServiceComponent
		with AutowiringPermissionsServiceComponent
		with AutowiringModuleAndDepartmentServiceComponent