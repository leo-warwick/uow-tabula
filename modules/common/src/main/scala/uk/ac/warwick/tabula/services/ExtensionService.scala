package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions._

trait ExtensionServiceComponent {
	def extensionService: ExtensionService
}

trait AutowiringExtensionServiceComponent extends ExtensionServiceComponent {
	var extensionService = Wire[ExtensionService]
}
trait ExtensionService {
	def getExtensionById(id: String): Option[Extension]
	def countExtensions(assignment: Assignment): Int
	def hasExtensions(assignment: Assignment): Boolean
	def countUnapprovedExtensions(assignment: Assignment): Int
	def hasUnapprovedExtensions(assignment: Assignment): Boolean
	def getUnapprovedExtensions(assignment: Assignment): Seq[Extension]

	def deleteAttachment(extension: Extension, attachment: FileAttachment): Unit
	def delete(extension: Extension): Unit
	def save(extension: Extension): Unit
}

abstract class AbstractExtensionService extends ExtensionService {
	self: ExtensionDaoComponent with FileAttachmentServiceComponent =>

	def getExtensionById(id: String) = transactional(readOnly = true) { extensionDao.getExtensionById(id) }
	def countExtensions(assignment: Assignment): Int = transactional(readOnly = true) { extensionDao.countExtensions(assignment) }
	def countUnapprovedExtensions(assignment: Assignment): Int = transactional(readOnly = true) { extensionDao.countUnapprovedExtensions(assignment) }
	def getUnapprovedExtensions(assignment: Assignment): Seq[Extension] = transactional(readOnly = true) { extensionDao.getUnapprovedExtensions(assignment) }

	def hasExtensions(assignment: Assignment): Boolean = countExtensions(assignment) > 0
	def hasUnapprovedExtensions(assignment: Assignment): Boolean = countUnapprovedExtensions(assignment) > 0

	def deleteAttachment(extension: Extension, attachment: FileAttachment) = {
		attachment.extension.removeAttachment(attachment)
		fileAttachmentService.deleteAttachments(Seq(attachment))
	}

	def delete(extension: Extension) = transactional() { extensionDao.delete(extension) }
	def save(extension: Extension) = transactional() { extensionDao.saveOrUpdate(extension) }
}

@Service(value = "extensionService")
class ExtensionServiceImpl
	extends AbstractExtensionService
		with AutowiringExtensionDaoComponent
		with AutowiringFileAttachmentServiceComponent
		with AutowiringMaintenanceModeServiceComponent