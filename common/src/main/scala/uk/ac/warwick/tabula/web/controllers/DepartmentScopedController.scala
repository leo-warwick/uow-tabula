package uk.ac.warwick.tabula.web.controllers

import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Department, UserSettings}
import uk.ac.warwick.tabula.helpers.RequestLevelCaching
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.{MaintenanceModeServiceComponent, ModuleAndDepartmentServiceComponent, UserSettingsServiceComponent}

import scala.collection.JavaConverters._

trait DepartmentScopedController extends RequestLevelCaching[(CurrentUser, Permission), Seq[Department]] {

	self: BaseController with UserSettingsServiceComponent with ModuleAndDepartmentServiceComponent with MaintenanceModeServiceComponent =>

	def departmentPermission: Permission

	@ModelAttribute("departmentsWithPermission")
	def departmentsWithPermission: Seq[Department] = {
		def withSubDepartments(d: Department) = Seq(d) ++ d.children.asScala.toSeq.sortBy(_.fullName)

		cachedBy((user, departmentPermission)) {
			moduleAndDepartmentService.departmentsWithPermission(user, departmentPermission)
				.toSeq.sortBy(_.fullName)
				.flatMap(withSubDepartments).distinct
		}
	}

	protected def retrieveActiveDepartment(departmentOption: Option[Department]): Option[Department] = {
		departmentOption match {
			case Some(department) if (departmentsWithPermission.contains(department) || user.god) && maintenanceModeService.enabled =>
				// Don't store if maintenance mode is enabled
				Some(department)
			case Some(department) if user.apparentUser.isFoundUser && (departmentsWithPermission.contains(department) || user.god) =>
				// Store the new active department and return it
				val settings = new UserSettings(user.apparentId)
				settings.activeDepartment = department
				transactional() {
					userSettingsService.save(user, settings)
				}
				Some(department)
			case Some(department) =>
				None
			case _ =>
				userSettingsService.getByUserId(user.apparentId).flatMap(_.activeDepartment).filter(departmentsWithPermission.contains)
		}
	}

	/**
	 * This should be overriden to just call retrieveActiveDepartment,
	 * but with the PathVariable-provided department as an argument (or null),
	 * and annotated with @ModelAttribute("activeDepartment").
	 */
	def activeDepartment(department: Department): Option[Department]

}
