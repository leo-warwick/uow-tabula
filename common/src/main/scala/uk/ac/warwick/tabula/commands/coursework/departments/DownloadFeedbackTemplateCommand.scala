package uk.ac.warwick.tabula.commands.coursework.departments

import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Description, ReadOnly, Command}
import uk.ac.warwick.tabula.services.fileserver.RenderableAttachment
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import beans.BeanProperty
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.FeedbackTemplate
import uk.ac.warwick.tabula.permissions._


class DownloadFeedbackTemplateCommand(
		val department: Department,
		val template: FeedbackTemplate,
		val filename: String,
		user: CurrentUser)
		extends Command[Option[RenderableFile]] with ReadOnly{

	mustBeLinked(template, department)
	PermissionCheck(Permissions.FeedbackTemplate.Read, template)

	private var fileFound: Boolean = _
	var callback: (RenderableFile) => Unit = _

	def applyInternal(): Option[RenderableAttachment] = {

		val attachment = Option(template.attachment)
		val renderableAttachment = attachment find (_.name == filename) map (a => new RenderableAttachment(a))

		fileFound = renderableAttachment.isDefined
		if (callback != null) {
			renderableAttachment.map { callback(_) }
		}
		renderableAttachment
	}

	override def describe(d: Description): Unit = d
		.department(department)
		.property("template", template.id)

	override def describeResult(d: Description): Unit = d
		.property("fileFound", fileFound)

}
