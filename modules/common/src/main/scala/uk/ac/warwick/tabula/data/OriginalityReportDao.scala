package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{OriginalityReport, FileAttachment}

trait OriginalityReportDao {
	def saveOriginalityReport(attachment: FileAttachment): OriginalityReport
	def deleteOriginalityReport(attachment: FileAttachment): Unit
	def getOriginalityReportByFileId(fileId: String): Option[OriginalityReport]
}

abstract class AbstractOriginalityReportDao extends OriginalityReportDao with HelperRestrictions {
	self: SessionComponent =>

	/**
		* Deletes the OriginalityReport attached to this Submission if one
		* exists. It flushes the session straight away because otherwise deletes
		* don't always happen until after some insert operation that assumes
		* we've deleted it.
		*/
	def deleteOriginalityReport(attachment: FileAttachment) {
		if (attachment.originalityReport != null) {
			val report = attachment.originalityReport
			attachment.originalityReport = null
			session.delete(report)
			session.flush()
		}
	}

	def saveOriginalityReport(attachment: FileAttachment) = {
		attachment.originalityReport.attachment = attachment
		session.saveOrUpdate(attachment.originalityReport)
		attachment.originalityReport
	}

	def getOriginalityReportByFileId(fileId: String): Option[OriginalityReport] = {
		session.newCriteria[OriginalityReport]
			.createAlias("attachment", "attachment")
			.add(is("attachment.id", fileId))
			.seq.headOption
	}

}

@Repository("originalityReportDao")
class OriginalityReportDaoImpl extends AbstractOriginalityReportDao with Daoisms

trait OriginalityReportDaoComponent {
	def originalityReportDao: OriginalityReportDao
}

trait AutowiringOriginalityReportDaoComponent extends OriginalityReportDaoComponent {
	override val originalityReportDao = Wire[OriginalityReportDao]
}