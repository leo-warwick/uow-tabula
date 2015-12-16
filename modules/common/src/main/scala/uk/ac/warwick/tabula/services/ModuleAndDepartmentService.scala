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
import uk.ac.warwick.tabula.services.permissions.AutowiringPermissionsServiceComponent
import uk.ac.warwick.spring.Wire

/**
 * Handles data about modules and departments
 */
@Service
class ModuleAndDepartmentService extends Logging
	with AutowiringUserLookupComponent
	with AutowiringSecurityServiceComponent
	with AutowiringPermissionsServiceComponent {

	@Autowired var moduleDao: ModuleDao = _
	@Autowired var departmentDao: DepartmentDao = _
	def groupService = userLookup.getGroupService

	def allDepartments = transactional(readOnly = true) {
		departmentDao.allDepartments
	}

	def allRootDepartments = transactional(readOnly = true) {
		departmentDao.allRootDepartments
	}

	def allModules = transactional(readOnly = true) {
		moduleDao.allModules
	}

	def getDepartmentByCode(code: String) = transactional(readOnly = true) {
		departmentDao.getByCode(code)
	}

	def getDepartmentById(code: String) = transactional(readOnly = true) {
		departmentDao.getById(code)
	}

	def getModuleByCode(code: String) = transactional(readOnly = true) {
		moduleDao.getByCode(code.toLowerCase)
	}

	def getModulesByCodes(codes: Seq[String]) = transactional(readOnly = true) {
		moduleDao.getAllByCodes(codes.map(_.toLowerCase))
	}

	def getModuleBySitsCode(sitsModuleCode: String) = transactional(readOnly = true) {
		Module.stripCats(sitsModuleCode) match {
			case Some(code) => moduleDao.getByCode(code.toLowerCase)
			case _ => None
		}
	}

	def getModuleById(id: String) = transactional(readOnly = true) {
		moduleDao.getById(id)
	}

	def getModuleTeachingInformation(moduleCode: String, departmentCode: String) = transactional(readOnly = true) {
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

	def modulesInDepartmentsWithPermission(user: CurrentUser, permission: Permission) = {
		departmentsWithPermission(user, permission) flatMap (dept => dept.modules.asScala)
	}
	def modulesInDepartmentWithPermission(user: CurrentUser, permission: Permission, dept: Department): Set[Module] = {
		if (departmentsWithPermission(user, permission) contains dept) dept.modules.asScala.toSet else Set()
	}

	def addOwner(dept: Department, owner: String) = transactional() {
		val role = permissionsService.getOrCreateGrantedRole(dept, DepartmentalAdministratorRoleDefinition)
		role.users.knownType.addUserId(owner)
		permissionsService.saveOrUpdate(role)
		permissionsService.clearCachesForUser((owner, classTag[Department]))
	}

	def removeOwner(dept: Department, owner: String) = transactional() {
		val role = permissionsService.getOrCreateGrantedRole(dept, DepartmentalAdministratorRoleDefinition)
		role.users.knownType.removeUserId(owner)
		permissionsService.saveOrUpdate(role)
		permissionsService.clearCachesForUser((owner, classTag[Department]))
	}

	def addModuleManager(module: Module, owner: String) = transactional() {
		val role = permissionsService.getOrCreateGrantedRole(module, ModuleManagerRoleDefinition)
		role.users.knownType.addUserId(owner)
		permissionsService.saveOrUpdate(role)
		permissionsService.clearCachesForUser((owner, classTag[Module]))
	}

	def removeModuleManager(module: Module, owner: String) = transactional() {
		val role = permissionsService.getOrCreateGrantedRole(module, ModuleManagerRoleDefinition)
		role.users.knownType.removeUserId(owner)
		permissionsService.saveOrUpdate(role)
		permissionsService.clearCachesForUser((owner, classTag[Module]))
	}

	def save(dept: Department) = transactional() {
		departmentDao.save(dept)
	}

	def saveOrUpdate(module: Module) = transactional() {
		moduleDao.saveOrUpdate(module)
	}

	def saveOrUpdate(teachingInfo: ModuleTeachingInformation) = transactional() {
		moduleDao.saveOrUpdate(teachingInfo)
	}

	def delete(teachingInfo: ModuleTeachingInformation) = transactional() {
		moduleDao.delete(teachingInfo)
	}

	def stampMissingModules(seenCodes: Seq[String]) = transactional() {
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
}

trait ModuleAndDepartmentServiceComponent {
	def moduleAndDepartmentService: ModuleAndDepartmentService
}

trait AutowiringModuleAndDepartmentServiceComponent extends ModuleAndDepartmentServiceComponent {
	var moduleAndDepartmentService = Wire[ModuleAndDepartmentService]

}