package uk.ac.warwick.tabula.commands.cm2.assignments.extensions

import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.data.model.forms.Extension

/**
	* This could be a separate service, but it's so noddy it's not (yet) worth it
	*/
trait HibernateExtensionPersistenceComponent extends ExtensionPersistenceComponent with Daoisms {
	def delete(attachment: FileAttachment): Unit = {
		attachment.extension.removeAttachment(attachment)
		session.delete(attachment)
	}
	def delete(extension: Extension): Unit = session.delete(extension)
	def save(extension: Extension): Unit = session.saveOrUpdate(extension)
}

trait ExtensionPersistenceComponent {
	def delete(attachment: FileAttachment)
	def delete(extension: Extension)
	def save(extension: Extension)
}