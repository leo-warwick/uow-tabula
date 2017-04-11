package uk.ac.warwick.tabula.web.controllers.cm2.admin

import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestMethod}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.departments.ExtensionSettingsCommand
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent, ModuleAndDepartmentService}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{department}/settings/extensions"))
class ExtensionSettingsController extends CourseworkController
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringMaintenanceModeServiceComponent {

	@Autowired var moduleService: ModuleAndDepartmentService = _

	@ModelAttribute def extensionSettingsCommand(@PathVariable department:Department) = new ExtensionSettingsCommand(mandatory(department))

	val academicYear = activeAcademicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))

	validatesSelf[ExtensionSettingsCommand]

	@RequestMapping(method=Array(RequestMethod.GET, RequestMethod.HEAD))
	def viewSettings(@PathVariable department:Department, user:CurrentUser, cmd:ExtensionSettingsCommand, errors:Errors): Mav =
		Mav(s"$urlPrefix/admin/extension-settings"
	)

	@RequestMapping(method=Array(RequestMethod.POST))
	def saveSettings(@PathVariable department:Department, @Valid cmd:ExtensionSettingsCommand, errors:Errors): Mav = {
		if (errors.hasErrors){
			viewSettings(department, user, cmd, errors)
		} else {
			cmd.apply()
			Redirect(Routes.admin.department(cmd.department, academicYear))
		}
	}
}