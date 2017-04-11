package uk.ac.warwick.tabula.services

import scala.collection.JavaConverters._
import scala.reflect._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.data.DepartmentDao
import uk.ac.warwick.tabula.data.ModuleDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import uk.ac.warwick.tabula.roles.ModuleManagerRoleDefinition
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.spring.Wire

/**
 * Handles data about modules and departments
 */
@Service
class ModuleAndDepartmentService extends Logging {

	@Autowired var moduleDao: ModuleDao = _
	@Autowired var departmentDao: DepartmentDao = _
	@Autowired var userLookup: UserLookupService = _
	@Autowired var securityService: SecurityService = _
	@Autowired var permissionsService: PermissionsService = _
	def groupService: LenientGroupService = userLookup.getGroupService

	def allDepartments: Seq[Department] = transactional(readOnly = true) {
		departmentDao.allDepartments
	}

	def allRootDepartments: Seq[Department] = transactional(readOnly = true) {
		departmentDao.allRootDepartments
	}

	def allModules: Seq[Module] = transactional(readOnly = true) {
		moduleDao.allModules
	}

	def getDepartmentByCode(code: String): Option[Department] = transactional(readOnly = true) {
		departmentDao.getByCode(code)
	}

	def getDepartmentById(code: String): Option[Department] = transactional(readOnly = true) {
		departmentDao.getById(code)
	}

	def getModuleByCode(code: String): Option[Module] = transactional(readOnly = true) {
		moduleDao.getByCode(code.toLowerCase)
	}

	def getModulesByCodes(codes: Seq[String]): Seq[Module] = transactional(readOnly = true) {
		moduleDao.getAllByCodes(codes.map(_.toLowerCase))
	}

	def getModuleBySitsCode(sitsModuleCode: String): Option[Module] = transactional(readOnly = true) {
		Module.stripCats(sitsModuleCode) match {
			case Some(code) => moduleDao.getByCode(code.toLowerCase)
			case _ => None
		}
	}

	def getModuleById(id: String): Option[Module] = transactional(readOnly = true) {
		moduleDao.getById(id)
	}

	def getModuleTeachingInformation(moduleCode: String, departmentCode: String): Option[ModuleTeachingInformation] = transactional(readOnly = true) {
		moduleDao.getTeachingInformationByModuleCodeAndDepartmentCode(moduleCode, departmentCode)
	}

	// We may have a granted role that's overridden later, so we also need to do a security service check as well
	// as getting the role itself

	def departmentsWithPermission(user: CurrentUser, permission: Permission): Set[Department] =
		permissionsService.getAllPermissionDefinitionsFor[Department](user, permission)
			.filter {
				department => securityService.can(user, permission, department)
			}

	def modulesWithPermission(user: CurrentUser, permission: Permission): Set[Module] =
		permissionsService.getAllPermissionDefinitionsFor[Module](user, permission)
			.filter { module => securityService.can(user, permission, module) }

	def modulesWithPermission(user: CurrentUser, permission: Permission, dept: Department): Set[Module] =
		modulesWithPermission(user, permission).filter { _.adminDepartment == dept }

	def modulesInDepartmentsWithPermission(user: CurrentUser, permission: Permission): Set[Module] = {
		departmentsWithPermission(user, permission) flatMap (dept => dept.modules.asScala)
	}
	def modulesInDepartmentWithPermission(user: CurrentUser, permission: Permission, dept: Department): Set[Module] = {
		if (departmentsWithPermission(user, permission) contains dept) dept.modules.asScala.toSet else Set()
	}

	def addOwner(dept: Department, owner: String): Unit = transactional() {
		val role = permissionsService.getOrCreateGrantedRole(dept, DepartmentalAdministratorRoleDefinition)
		role.users.knownType.addUserId(owner)
		permissionsService.saveOrUpdate(role)
		permissionsService.clearCachesForUser((owner, classTag[Department]))
	}

	def removeOwner(dept: Department, owner: String): Unit = transactional() {
		val role = permissionsService.getOrCreateGrantedRole(dept, DepartmentalAdministratorRoleDefinition)
		role.users.knownType.removeUserId(owner)
		permissionsService.saveOrUpdate(role)
		permissionsService.clearCachesForUser((owner, classTag[Department]))
	}

	def addModuleManager(module: Module, owner: String): Unit = transactional() {
		val role = permissionsService.getOrCreateGrantedRole(module, ModuleManagerRoleDefinition)
		role.users.knownType.addUserId(owner)
		permissionsService.saveOrUpdate(role)
		permissionsService.clearCachesForUser((owner, classTag[Module]))
	}

	def removeModuleManager(module: Module, owner: String): Unit = transactional() {
		val role = permissionsService.getOrCreateGrantedRole(module, ModuleManagerRoleDefinition)
		role.users.knownType.removeUserId(owner)
		permissionsService.saveOrUpdate(role)
		permissionsService.clearCachesForUser((owner, classTag[Module]))
	}

	def saveOrUpdate(dept: Department): Unit = transactional() {
		departmentDao.saveOrUpdate(dept)
	}

	def saveOrUpdate(module: Module): Unit = transactional() {
		moduleDao.saveOrUpdate(module)
	}

	def saveOrUpdate(teachingInfo: ModuleTeachingInformation): Unit = transactional() {
		moduleDao.saveOrUpdate(teachingInfo)
	}

	def delete(teachingInfo: ModuleTeachingInformation): Unit = transactional() {
		moduleDao.delete(teachingInfo)
	}

	def stampMissingModules(seenCodes: Seq[String]): Unit = transactional() {
		moduleDao.stampMissingFromImport(moduleDao.allModules.map(_.code) filterNot seenCodes.contains)
	}

	def hasAssignments(module: Module): Boolean = {
		moduleDao.hasAssignments(module)
	}

	def findModulesNamedLike(query: String): Seq[Module] =
		moduleDao.findModulesNamedLike(query)

	def findModulesByRoutes(routes: Seq[Route], academicYear: AcademicYear): Seq[Module] =
		moduleDao.findByRoutes(routes, academicYear)

	def findModulesByYearOfStudy(department: Department, yearsOfStudy: Seq[Integer], academicYear: AcademicYear): Seq[Module] =
		moduleDao.findByYearOfStudy(department, yearsOfStudy, academicYear)

	def findByRouteYearAcademicYear(route: Route, yearOfStudy: Int, academicYear: AcademicYear): Seq[Module] =
		moduleDao.findByRouteYearAcademicYear(route, yearOfStudy, academicYear)
}

trait ModuleAndDepartmentServiceComponent {
	def moduleAndDepartmentService: ModuleAndDepartmentService
}

trait AutowiringModuleAndDepartmentServiceComponent extends ModuleAndDepartmentServiceComponent {
	var moduleAndDepartmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]

}