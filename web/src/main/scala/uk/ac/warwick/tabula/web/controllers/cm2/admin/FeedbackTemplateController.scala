package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.services.{AutowiringCourseAndRouteServiceComponent, AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent, AutowiringUserLookupComponent}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.departments.{BulkFeedbackTemplateCommand, DeleteFeedbackTemplateCommand, EditFeedbackTemplateCommand}
import uk.ac.warwick.tabula.data.model.{Department, FeedbackTemplate}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.DepartmentScopedController
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController


@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{dept}/settings/feedback-templates"))
class FeedbackTemplateController extends CourseworkController
	with DepartmentScopedController with AutowiringUserLookupComponent
	with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringCourseAndRouteServiceComponent with AutowiringMaintenanceModeServiceComponent  {

	override def departmentPermission: Permission = Permissions.FeedbackTemplate.Manage

	@ModelAttribute def bulkFeedbackTemplateCommand(@PathVariable dept:Department)
		= new BulkFeedbackTemplateCommand(mandatory(dept))

	
	@ModelAttribute("activeDepartment")
	def activeDepartment(@PathVariable dept: Department): Option[Department] = retrieveActiveDepartment(Option(dept))


	 @RequestMapping(method=Array(GET, HEAD))
	 def list(cmd:BulkFeedbackTemplateCommand, errors:Errors): Mav = {

			val dept = cmd.department

		 	Mav(s"$urlPrefix/admin/feedbackforms/manage-feedback-templates",
				"department" -> dept
		 	)
		
	 }

	@RequestMapping(method=Array(POST))
	def saveBulk(cmd:BulkFeedbackTemplateCommand, errors:Errors): Mav = {
		if (errors.hasErrors){
			list(cmd, errors)
		}
		else{
			cmd.apply()
			Redirect(Routes.admin.feedbackTemplates(cmd.department))
		}
	}

}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{dept}/settings/feedback-templates/edit/{template}"))
class EditFeedbackTemplateController extends CourseworkController {

	@ModelAttribute def editFeedbackTemplateCommand(@PathVariable dept:Department, @PathVariable template:FeedbackTemplate)
		= new EditFeedbackTemplateCommand(dept, template)

	@RequestMapping(method=Array(GET))
	def edit(cmd:EditFeedbackTemplateCommand, errors:Errors): Mav = {
		val dept = cmd.department
		val template = cmd.template

		cmd.id = template.id
		cmd.name = template.name
		cmd.description = template.description

		val model = Mav(s"$urlPrefix/admin/feedbackforms/edit-feedback-template",
			"department" -> dept,
			"template" -> template
		).noNavigation()
		model
	}

	@RequestMapping(method=Array(POST))
	def save(cmd:EditFeedbackTemplateCommand, errors:Errors): Mav = {
		if (errors.hasErrors){
			edit(cmd, errors)
		}
		else{
			cmd.apply()
			val model = Mav("ajax_success").noNavigation()
			model
		}
	}

}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{dept}/settings/feedback-templates/delete/{template}"))
class DeleteFeedbackTemplateController extends CourseworkController {

	@ModelAttribute def deleteFeedbackTemplateCommand(@PathVariable dept:Department, @PathVariable template:FeedbackTemplate)
	= new DeleteFeedbackTemplateCommand(dept, template)

	@RequestMapping(method=Array(GET))
	def deleteCheck(cmd:DeleteFeedbackTemplateCommand, errors: Errors): Mav = {
		val template = cmd.template
		val dept = cmd.department

		cmd.id = template.id
		val model = Mav(s"$urlPrefix/admin/feedbackforms/delete-feedback-template",
			"department" -> dept,
			"template" -> template
		).noNavigation()
		model

	}

	@RequestMapping(method=Array(POST))
	def delete(cmd:DeleteFeedbackTemplateCommand, errors: Errors): Mav = {
		cmd.apply()
		val model = Mav("ajax_success").noNavigation()
		model
	}

}