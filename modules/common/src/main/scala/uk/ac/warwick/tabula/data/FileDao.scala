package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.data.model.{FileAttachmentToken, FileAttachment}
import org.springframework.stereotype.Repository
import org.joda.time.DateTime
import org.hibernate.criterion.{ Restrictions => Is }
import org.hibernate.criterion.Order._
import uk.ac.warwick.tabula.services.AutowiringMaintenanceModeServiceComponent
import collection.JavaConversions._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.helpers.Logging
import org.hibernate.criterion.Projections
import org.hibernate.`type`.StringType
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.spring.Wire

trait FileDaoComponent {
	def fileDao: FileDao
}

trait AutowiringFileDaoComponent extends FileDaoComponent {
	val fileDao = Wire[FileDao]
}

@Repository
class FileDao extends Daoisms with Logging {

	def saveOrUpdate(file: FileAttachment) = {
		session.saveOrUpdate(file)
		file
	}

	def getFileById(id: String) = session.getById[FileAttachment](id)

	def getFileByStrippedId(id: String) = transactional(readOnly = true) {
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

	def deleteAttachments(files: Seq[FileAttachment]) = files.foreach(session.delete)

	def saveOrUpdate(token: FileAttachmentToken): Unit = session.saveOrUpdate(token)

	def findOldTemporaryFiles(maxAgeInDays: Int, batchSize: Int) = transactional() {
		session.newCriteria[FileAttachment]
			.add(is("temporary", true))
			.add(Is.lt("dateUploaded", DateTime.now.minusDays(maxAgeInDays)))
			.setMaxResults(batchSize)
			.seq
	}
}
