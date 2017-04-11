package uk.ac.warwick.tabula.commands.cm2.departments

import org.springframework.validation.BindingResult
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, Description, UploadedFile}
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Department, FeedbackTemplate}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.system.BindListener

import scala.collection.JavaConversions._

abstract class FeedbackTemplateCommand(val department:Department)
	extends Command[Seq[FeedbackTemplate]] with Daoisms with BindListener {

	var file:UploadedFile = new UploadedFile

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

class BulkFeedbackTemplateCommand(department:Department) extends FeedbackTemplateCommand(department) {

	PermissionCheck(Permissions.FeedbackTemplate.Manage, department)

	override def applyInternal(): Seq[FeedbackTemplate] = {
		transactional() {
			val feedbackTemplates = if (!file.attached.isEmpty) {
				for (attachment <- file.attached) yield {
					val feedbackForm = new FeedbackTemplate
					feedbackForm.name = attachment.name
					feedbackForm.department = department
					feedbackForm.attachFile(attachment)
					department.feedbackTemplates.add(feedbackForm)
					session.saveOrUpdate(feedbackForm)

					feedbackForm
				}
			} else Nil
			session.saveOrUpdate(department)

			feedbackTemplates
		}
	}
}

class EditFeedbackTemplateCommand(department:Department, val template: FeedbackTemplate) extends FeedbackTemplateCommand(department) {

	mustBeLinked(template, department)
	PermissionCheck(Permissions.FeedbackTemplate.Manage, template)

	var zipService: ZipService = Wire.auto[ZipService]

	var id:String = _
	var name:String = _
	var description:String = _

	override def applyInternal(): Seq[FeedbackTemplate] = {
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
			session.update(feedbackTemplate)
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

	override def applyInternal(): Seq[FeedbackTemplate] = {
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