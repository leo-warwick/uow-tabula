package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model.{Department, FeedbackTemplate}
import uk.ac.warwick.tabula.commands.coursework.departments.{BulkFeedbackTemplateCommand, DeleteFeedbackTemplateCommand, EditFeedbackTemplateCommand}
import uk.ac.warwick.tabula.web.Mav
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.coursework.web.Routes

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/admin/department/{dept}/settings/feedback-templates"))
class OldFeedbackTemplateController extends OldCourseworkController {

	@ModelAttribute def bulkFeedbackTemplateCommand(@PathVariable dept:Department)
		= new BulkFeedbackTemplateCommand(dept)

	// Add the common breadcrumbs to the model.
	def crumbed(mav:Mav, dept:Department):Mav = mav.crumbs(Breadcrumbs.Department(dept))

	@RequestMapping(method=Array(GET, HEAD))
	def list(cmd:BulkFeedbackTemplateCommand, errors:Errors): Mav = {
		val dept = cmd.department

		val model = Mav(s"$urlPrefix/admin/feedbackforms/manage-feedback-templates",
			"department" -> dept
		)
		crumbed(model, dept)
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

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/admin/department/{dept}/settings/feedback-templates/edit/{template}"))
class OldEditFeedbackTemplateController extends OldCourseworkController {

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

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/admin/department/{dept}/settings/feedback-templates/delete/{template}"))
class OldDeleteFeedbackTemplateController extends OldCourseworkController {

	@ModelAttribute def deleteFeedbackTemplateCommand(@PathVariable dept:Department, @PathVariable template:FeedbackTemplate)
		= new DeleteFeedbackTemplateCommand(dept, template)

	@RequestMapping(method=Array(GET))
	def deleteCheck(cmd:DeleteFeedbackTemplateCommand, errors:Errors): Mav = {
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
	def delete(cmd:DeleteFeedbackTemplateCommand, errors:Errors): Mav = {
		cmd.apply()
		val model = Mav("ajax_success").noNavigation()
		model
	}

}