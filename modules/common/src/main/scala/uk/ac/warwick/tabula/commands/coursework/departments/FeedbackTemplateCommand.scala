package uk.ac.warwick.tabula.commands.coursework.departments

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.commands.{Description, Command}
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.model.{FeedbackTemplate, Department}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ZipService}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.permissions._
import org.springframework.validation.BindingResult

abstract class FeedbackTemplateCommand(val department:Department)
	extends Command[Seq[FeedbackTemplate]] with BindListener {

	var file: UploadedFile = new UploadedFile

	override def onBind(result:BindingResult) {
		transactional() {
			file.onBind(result)
		}
	}

	// describe the thing that's happening.
	override def describe(d:Description) {
		d.department(department)
	}

	override def describeResult(d: Description, templates: Seq[FeedbackTemplate]) {
		d.department(department)
		 .property("feedbackTemplate" -> templates.map { _.id })
		 .fileAttachments(templates.map { _.attachment })
	}
}

class BulkFeedbackTemplateCommand(department: Department) extends FeedbackTemplateCommand(department) with AutowiringModuleAndDepartmentServiceComponent {
	PermissionCheck(Permissions.FeedbackTemplate.Manage, department)

	override def applyInternal() = {
		transactional() {
			val feedbackTemplates = if (!file.attached.isEmpty) {
				for (attachment <- file.attached) yield {
					val feedbackForm = new FeedbackTemplate
					feedbackForm.name = attachment.name
					feedbackForm.department = department
					feedbackForm.attachFile(attachment)
					department.feedbackTemplates.add(feedbackForm)

					feedbackForm
				}
			} else Nil

			moduleAndDepartmentService.save(department)

			feedbackTemplates
		}
	}
}

class EditFeedbackTemplateCommand(department:Department, val template: FeedbackTemplate) extends FeedbackTemplateCommand(department) with AutowiringModuleAndDepartmentServiceComponent {

	mustBeLinked(template, department)
	PermissionCheck(Permissions.FeedbackTemplate.Manage, template)

	var zipService = Wire.auto[ZipService]

	var id:String = _
	var name:String = _
	var description:String = _

	override def applyInternal() = {
		transactional() {
			val feedbackTemplate = department.feedbackTemplates.find(_.id == id).get
			feedbackTemplate.name = name
			feedbackTemplate.description = description
			feedbackTemplate.department = department

			if (!file.attached.isEmpty) {
				for (attachment <- file.attached) {
					feedbackTemplate.attachFile(attachment)
				}
			}

			moduleAndDepartmentService.save(department)

			// invalidate any zip files for linked assignments
			feedbackTemplate.assignments.foreach(zipService.invalidateSubmissionZip(_))

			Seq(feedbackTemplate)
		}
	}
}

class DeleteFeedbackTemplateCommand(department:Department, val template: FeedbackTemplate) extends FeedbackTemplateCommand(department) with Logging {

	mustBeLinked(template, department)
	PermissionCheck(Permissions.FeedbackTemplate.Manage, template)

	var id:String = _

	override def applyInternal() = {
		transactional() {
			val feedbackTemplate= department.feedbackTemplates.find(_.id == id).get
			if (feedbackTemplate.hasAssignments) {
				logger.error("Cannot delete feedback template " + feedbackTemplate.id + " - it is still linked to assignments")
				Nil
			} else {
				department.feedbackTemplates.remove(feedbackTemplate)
				Seq(feedbackTemplate)
			}
		}
	}
}