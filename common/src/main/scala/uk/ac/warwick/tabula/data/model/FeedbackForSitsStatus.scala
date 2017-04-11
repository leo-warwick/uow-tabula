package uk.ac.warwick.tabula.data.model

import java.sql.Types

sealed abstract class FeedbackForSitsStatus(val code: String, val description: String) {
	// for Spring
	def getCode: String = code
	def getDescription: String = description

	override def toString: String = description
}

object FeedbackForSitsStatus {
	case object UploadNotAttempted extends FeedbackForSitsStatus("uploadNotAttempted", "Queued for SITS upload")
	case object Failed extends FeedbackForSitsStatus("failed", "SITS Upload Failed")
	case object Successful extends FeedbackForSitsStatus("successful", "Uploaded to SITS")

	// manual collection - keep in sync with the case objects above
	val members = Seq(UploadNotAttempted, Successful, Failed)

	def fromCode(code: String): FeedbackForSitsStatus =
		if (code == null) null
		else members.find{_.code == code} match {
			case Some(caseObject) => caseObject
			case None => throw new IllegalArgumentException()
		}

	def fromDescription(description: String): FeedbackForSitsStatus =
		if (description == null) null
		else members.find{_.description == description} match {
			case Some(caseObject) => caseObject
			case None => throw new IllegalArgumentException()
		}
}

class FeedbackForSitsStatusUserType extends AbstractStringUserType[FeedbackForSitsStatus] {

	override def sqlTypes = Array(Types.VARCHAR)
	override def convertToObject(string: String): FeedbackForSitsStatus = FeedbackForSitsStatus.fromCode(string)
	override def convertToValue(format: FeedbackForSitsStatus): String = format.code
}
