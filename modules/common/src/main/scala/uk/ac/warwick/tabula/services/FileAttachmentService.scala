package uk.ac.warwick.tabula.services

import java.io.{FileInputStream, FileOutputStream, InputStream, File}

import org.joda.time.DateTime
import org.springframework.beans.factory.InitializingBean
import org.springframework.transaction.annotation.Propagation._
import org.springframework.util.FileCopyUtils
import org.springframework.web.multipart.MultipartFile
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.data.{AutowiringFileDaoComponent, FileDaoComponent}
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{FileAttachmentToken, FileAttachment}
import org.springframework.beans.factory.annotation.Value
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.util.files.hash.FileHasher
import uk.ac.warwick.util.files.hash.impl.SHAFileHasher

import FileAttachmentService._

trait FileAttachmentServiceComponent {
	def fileAttachmentService: FileAttachmentService
}

trait AutowiringFileAttachmentServiceComponent extends FileAttachmentServiceComponent {
	var fileAttachmentService = Wire[FileAttachmentService]
}

trait FileHasherComponent {
	def fileHasher: FileHasher
}

trait SHAFileHasherComponent extends FileHasherComponent {
	val fileHasher = new SHAFileHasher
}

trait FileAttachmentService {
	def deleteAttachments(files: Seq[FileAttachment])
	def saveOrUpdate(attachmentToken: FileAttachmentToken): Unit

	def saveTemporary(file: FileAttachment): FileAttachment
	def savePermanent(file: FileAttachment): FileAttachment

	def persistTemporary(multipartFiles: Seq[MultipartFile]): Seq[FileAttachment]

	/** Only for use by FileAttachment to find its own backing file. */
	def getData(id: String): Option[File]

	/**
		* Retrieves a File object where you can store data under this ID. It doesn't check
		* whether the File already exists. If you want to retrieve an existing file you must
		* use #getData which checks whether it exists and also knows to check the old-style path if needed.
		*/
	def targetFile(id: String): File

	def deleteOldTemporaryFiles(): Int

	def getAllFileIds(createdBefore: Option[DateTime] = None): Set[String]
	def getFileById(id: String): Option[FileAttachment]
	def getFileByStrippedId(id: String): Option[FileAttachment]
}

object FileAttachmentService {
	val idSplitSize = 2
	val idSplitSizeCompat = 4 // for existing paths split by 4 chars

	val TemporaryFileBatch = 1000 // query for this many each time
	val TemporaryFileSubBatch = 50 // run a separate transaction for each one
	val TemporaryFileMaxAgeInDays = 14 // TAB-2109
}

abstract class AbstractFileAttachmentService extends FileAttachmentService with Logging with InitializingBean {
	self: FileDaoComponent with FileHasherComponent =>

	@Value("${filesystem.attachment.dir}") var attachmentDir: File = _
	@Value("${filesystem.create.missing}") var createMissingDirectories: Boolean = _

	def deleteAttachments(files: Seq[FileAttachment]) = fileDao.deleteAttachments(files)
	def saveOrUpdate(token: FileAttachmentToken): Unit = fileDao.saveOrUpdate(token)

	private def saveAttachment(file: FileAttachment) = {
		fileDao.saveOrUpdate(file)

		if (!file.hasData && file.uploadedData != null) {
			persistFileData(file, file.uploadedData)
		}

		file
	}

	def saveTemporary(file: FileAttachment) = {
		file.temporary = true
		saveAttachment(file)
	}

	def savePermanent(file: FileAttachment) = {
		file.temporary = false
		saveAttachment(file)
	}

	def persistTemporary(multipartFiles: Seq[MultipartFile]): Seq[FileAttachment] = transactional() {
		multipartFiles.map { item =>
			val a = new FileAttachment
			a.name = new File(item.getOriginalFilename).getName
			a.uploadedData = item.getInputStream
			a.uploadedDataLength = item.getSize
			RequestInfo.fromThread.foreach { info => a.uploadedBy = info.user.userId }
			saveTemporary(a)
		}
	}

	def persistFileData(file: FileAttachment, inputStream: InputStream) {
		val target = targetFile(file.id)
		val directory = target.getParentFile
		directory.mkdirs()
		if (!directory.exists) throw new IllegalStateException("Couldn't create directory to store file")
		FileCopyUtils.copy(inputStream, new FileOutputStream(target))

		file.hash = fileHasher.hash(new FileInputStream(target))
		fileDao.saveOrUpdate(file)
	}

	private def partition(id: String, splitSize: Int): String = id.replace("-", "").grouped(splitSize).mkString("/")
	private def partition(id: String): String = partition(id, idSplitSize)
	private def partitionCompat(id: String): String = partition(id, idSplitSizeCompat)

	/**
		* Retrieves a File object where you can store data under this ID. It doesn't check
		* whether the File already exists. If you want to retrieve an existing file you must
		* use #getData which checks whether it exists and also knows to check the old-style path if needed.
		*/
	def targetFile(id: String): File = new File(attachmentDir, partition(id))
	private def targetFileCompat(id: String): File = new File(attachmentDir, partitionCompat(id))

	/** Only for use by FileAttachment to find its own backing file. */
	def getData(id: String): Option[File] = targetFile(id) match {
		case file: File if file.exists => Some(file)
		// If no file found, check if it's stored under old 4-character path style
		case _ => targetFileCompat(id) match {
			case file: File if file.exists => Some(file)
			case _ => None
		}
	}

	/**
		* Delete any temporary blobs that are more than 2 days old.
		*/
	def deleteOldTemporaryFiles(): Int = {
		val oldFiles = fileDao.findOldTemporaryFiles(TemporaryFileMaxAgeInDays, TemporaryFileBatch)
		/*
		 * This is a fun time for getting out of sync.
		 * Trying to run a few at a time in a separate transaction so that if something
		 * goes rubbish, there isn't too much out of sync.
		 */
		for (files <- oldFiles.grouped(TemporaryFileSubBatch)) deleteSomeFiles(files)

		oldFiles.size
	}

	private def deleteSomeFiles(files: Seq[FileAttachment]) {
		transactional(propagation = REQUIRES_NEW) {
			// To be safe, split off temporary files which are attached to non-temporary things
			// (which shouldn't happen, but we definitely don't want to delete things because of a bug elsewhere)
			// WARNING isAttached isn't exhaustive so this won't protect you all the time.
			val (dontDelete, okayToDelete) = files partition (_.isAttached)

			if (dontDelete.nonEmpty) {
				// Somewhere else in the app is failing to set temporary=false
				logger.error(
					"%d fileAttachments are temporary but are attached to another entity! " +
						"I won't delete them, but this is a bug that needs fixing!!" format dontDelete.size
				)
			}

			fileDao.deleteAttachments(okayToDelete)

			for (attachment <- okayToDelete; file <- getData(attachment.id)) {
				logger.info(s"Deleting attachment: id=${
					attachment.id
				}, path=${
					file.getAbsolutePath
				}, name=${
					attachment.name
				}, uploadedDate=${
					attachment.dateUploaded.toString()
				}, uploadedBy=${
					attachment.uploadedBy
				}")

				file.delete()
			}
		}
	}

	def getAllFileIds(createdBefore: Option[DateTime] = None): Set[String] = fileDao.getAllFileIds(createdBefore)
	def getFileById(id: String): Option[FileAttachment] = fileDao.getFileById(id)
	def getFileByStrippedId(id: String): Option[FileAttachment] = fileDao.getFileByStrippedId(id)

	override def afterPropertiesSet(): Unit = {
		if (!attachmentDir.isDirectory) {
			if (createMissingDirectories) {
				attachmentDir.mkdirs()
			} else {
				throw new IllegalStateException("Attachment store '" + attachmentDir + "' must be an existing directory")
			}
		}
	}
}

@Service("fileAttachmentService")
class FileAttachmentServiceImpl
	extends AbstractFileAttachmentService
		with AutowiringFileDaoComponent with SHAFileHasherComponent