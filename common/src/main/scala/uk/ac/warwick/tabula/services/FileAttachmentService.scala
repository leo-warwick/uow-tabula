package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{FileAttachment, FileAttachmentToken}
import uk.ac.warwick.tabula.data.{AutowiringFileDaoComponent, FileDaoComponent}

trait FileAttachmentServiceComponent {
	def fileAttachmentService: FileAttachmentService
}

trait AutowiringFileAttachmentServiceComponent extends FileAttachmentServiceComponent {
	var fileAttachmentService: FileAttachmentService = Wire[FileAttachmentService]
}

trait FileAttachmentService {
	def deleteAttachments(files: Seq[FileAttachment])
	def saveOrUpdate(attachmentToken: FileAttachmentToken): Unit
	def savePermanent(file: FileAttachment): Unit
	def saveTemporary(file: FileAttachment): Unit
	def getValidToken(attachment: FileAttachment): Option[FileAttachmentToken]
}


abstract class AbstractFileAttachmentService extends FileAttachmentService {

	self: FileDaoComponent =>

	def deleteAttachments(files: Seq[FileAttachment]): Unit = fileDao.deleteAttachments(files)
	def saveOrUpdate(token: FileAttachmentToken): Unit = fileDao.saveOrUpdate(token)
	def savePermanent(file: FileAttachment): Unit = fileDao.savePermanent(file)
	def saveTemporary(file: FileAttachment): Unit = fileDao.saveTemporary(file)
	def getValidToken(attachment: FileAttachment): Option[FileAttachmentToken] = fileDao.getValidToken(attachment)
}

@Service("fileAttachmentService")
class FileAttachmentServiceImpl
	extends AbstractFileAttachmentService
	with AutowiringFileDaoComponent