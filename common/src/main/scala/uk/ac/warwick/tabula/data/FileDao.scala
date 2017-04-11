package uk.ac.warwick.tabula.data

import com.google.common.net.MediaType
import org.hibernate.`type`.StringType
import org.hibernate.criterion.Order._
import org.hibernate.criterion.{Projections, Restrictions => Is}
import org.joda.time.DateTime
import org.joda.time.DateTime.now
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{FileAttachment, FileAttachmentToken}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.objectstore.ObjectStorageService
import uk.ac.warwick.util.files.hash.FileHasher
import uk.ac.warwick.util.files.hash.impl.SHAFileHasher

import scala.collection.JavaConversions._

trait FileDaoComponent {
	def fileDao: FileDao
}

trait AutowiringFileDaoComponent extends FileDaoComponent {
	val fileDao: FileDao = Wire[FileDao]
}

trait FileHasherComponent {
	def fileHasher: FileHasher
}

trait SHAFileHasherComponent extends FileHasherComponent {
	val fileHasher = new SHAFileHasher
}


@Repository
class FileDao extends Daoisms with Logging with SHAFileHasherComponent {

	@Autowired var objectStorageService: ObjectStorageService = _

	val TemporaryFileBatch = 1000 // query for this many each time
	val TemporaryFileSubBatch = 50 // run a separate transaction for each one
	val TemporaryFileMaxAgeInDays = 14 // TAB-2109

	private def saveAttachment(file: FileAttachment) = {
		session.saveOrUpdate(file)

		if (!file.hasData && file.uploadedData != null) {
			file.hash = fileHasher.hash(file.uploadedData.openStream())
			session.saveOrUpdate(file)

			val metadata = ObjectStorageService.Metadata(
				contentLength = file.uploadedData.size(),
				contentType = MediaType.OCTET_STREAM.toString, // TODO start storing content types
				fileHash = Some(file.hash)
			)

			objectStorageService.push(file.id, file.uploadedData, metadata)
		}

		file
	}

	def saveTemporary(file: FileAttachment): FileAttachment = {
		file.temporary = true
		saveAttachment(file)
	}

	def savePermanent(file: FileAttachment): FileAttachment = {
		file.temporary = false
		saveAttachment(file)
	}

	def saveOrUpdate(file: FileAttachment): FileAttachment = {
		session.saveOrUpdate(file)
		file
	}

	def getFileById(id: String): Option[FileAttachment] = getById[FileAttachment](id)

	def getFileByStrippedId(id: String): Option[FileAttachment] = transactional(readOnly = true) {
		session.newCriteria[FileAttachment]
				.add(Is.sqlRestriction("replace({alias}.id, '-', '') = ?", id, StringType.INSTANCE))
				.setMaxResults(1)
				.uniqueResult
	}

	def getFilesCreatedSince(createdSince: DateTime, maxResults: Int): Seq[FileAttachment] = transactional(readOnly = true) {
		session.newCriteria[FileAttachment]
				.add(Is.ge("dateUploaded", createdSince))
				.setMaxResults(maxResults)
				.addOrder(asc("dateUploaded"))
				.addOrder(asc("id"))
				.list
	}

	def getFilesCreatedOn(createdOn: DateTime, maxResults: Int, startingId: String): Seq[FileAttachment] = transactional(readOnly = true) {
		val criteria =
			session.newCriteria[FileAttachment]
				.add(Is.eq("dateUploaded", createdOn))

		if (startingId.hasText)
			criteria.add(Is.gt("id", startingId))

		criteria
			.setMaxResults(maxResults)
			.addOrder(asc("id"))
			.list
	}

	def getAllFileIds(createdBefore: Option[DateTime] = None): Set[String] = transactional(readOnly = true) {
		val criteria =
			session.newCriteria[FileAttachment]

		createdBefore.map { date =>
			criteria.add(Is.lt("dateUploaded", date))
		}
		criteria.project[String](Projections.id()).seq.toSet
	}

	def deleteAttachments(files: Seq[FileAttachment]): Unit = files.foreach(session.delete(_))

	def saveOrUpdate(token: FileAttachmentToken): Unit = session.saveOrUpdate(token)

	/**
	 * Delete any temporary blobs that are more than 2 days old.
	 */
	def deleteOldTemporaryFiles(): Int = {
		val oldFiles = findOldTemporaryFiles

		/*
		 * This is a fun time for getting out of sync.
		 * Trying to run a few at a time in a separate transaction so that if something
		 * goes rubbish, there isn't too much out of sync.
		 */
		for (files <- oldFiles.grouped(TemporaryFileSubBatch)) deleteSomeFiles(files)

		oldFiles.size
	}

	private def findOldTemporaryFiles = transactional() {
		session.newCriteria[FileAttachment]
			.add(is("temporary", true))
			.add(Is.lt("dateUploaded", now.minusDays(TemporaryFileMaxAgeInDays)))
			.setMaxResults(TemporaryFileBatch)
			.list
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

			session.newQuery[FileAttachment]("delete FileAttachment f where f.id in :ids")
				.setParameterList("ids", okayToDelete.map(_.id))
				.run()

			// TODO Should we be deleting from the Object Store? Maybe one day, if capacity becomes an issue
		}
	}

	def getValidToken(attachment: FileAttachment): Option[FileAttachmentToken] = {
		session.newCriteria[FileAttachmentToken]
			.add(is("fileAttachmentId", attachment.id))
			.add(Is.isNull("dateUsed"))
			.add(Is.gt("expires", DateTime.now))
			.addOrder(desc("expires"))
			.setMaxResults(1)
			.uniqueResult
	}

}
