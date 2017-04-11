package uk.ac.warwick.tabula.services.urkund

import org.joda.time.DateTime
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._

import scala.util.parsing.json.JSON

sealed abstract class UrkundSubmissionStatus(val status: String)
object UrkundSubmissionStatus {
	case object Submitted extends UrkundSubmissionStatus("Submitted")
	case object Rejected extends UrkundSubmissionStatus("Rejected")
	case object Accepted extends UrkundSubmissionStatus("Accepted")
	case object Analyzed extends UrkundSubmissionStatus("Analyzed")
	case object Error extends UrkundSubmissionStatus("Error")

	def fromStatus(status: String): UrkundSubmissionStatus = status match {
		case Submitted.status => Submitted
		case Rejected.status => Rejected
		case Accepted.status => Accepted
		case Analyzed.status => Analyzed
		case Error.status => Error
		case null => null
		case _ => throw new IllegalArgumentException()
	}
}

case class UrkundDocument(
	id: Int,
	acceptedDate: DateTime,
	downloadUrl: String,
	optOutUrl: String
)

case class UrkundReportWarning(
	warningType: String,
	excerpt: String,
	message: String
)

case class UrkundReport(
	id: Int,
	reportUrl: String,
	significance: Float,
	matchCount: Int,
	sourceCount: Int,
	warnings: Seq[UrkundReportWarning]
)

trait UrkundResponse {
	val statusCode: Int
	val response: String
}

case class UrkundSuccessResponse(
	statusCode: Int,
	response: String,
	submissionId: Option[Int],
	externalId: String,
	timestamp: DateTime,
	filename: String,
	contentType: String,
	status: UrkundSubmissionStatus,
	document: Option[UrkundDocument],
	report: Option[UrkundReport]

) extends UrkundResponse

case class UrkundErrorResponse(statusCode: Int, response: String) extends UrkundResponse

object UrkundResponse {
	private def parseIntOption(json: Map[String, Any], field: String): Option[Int] =
		json.get(field).map(_.asInstanceOf[Double].toInt)
	private def parseInt(json: Map[String, Any], field: String): Int =
		parseIntOption(json, field).getOrElse(
			throw new IllegalArgumentException(s"Cannot find Int with field name $field")
		)
	private def parseFloat(json: Map[String, Any], field: String): Float =
		json.get(field).map(_.asInstanceOf[Double].toFloat).getOrElse(
			throw new IllegalArgumentException(s"Cannot find Float with field name $field")
		)
	private def parseString(json: Map[String, Any], field: String): String =
		json.get(field).map(_.asInstanceOf[String]).getOrElse(
			throw new IllegalArgumentException(s"Cannot find String with field name $field")
		)
	private def parseDateTime(json: Map[String, Any], field: String): DateTime =
		new DateTime(parseString(json, field))

	private def parseResponse(statusCode: Int, response: String, submissionJson: Map[String, Any]): UrkundSuccessResponse = {
		UrkundSuccessResponse(
			statusCode = statusCode,
			response = response,
			submissionId = parseIntOption(submissionJson, "SubmissionId"),
			externalId = parseString(submissionJson, "ExternalId"),
			timestamp = parseDateTime(submissionJson, "Timestamp"),
			filename = parseString(submissionJson, "Filename"),
			contentType = parseString(submissionJson, "MimeType"),
			status = submissionJson.get("Status") match {
				case Some(statusJson: Map[String, Any] @unchecked) =>
					UrkundSubmissionStatus.fromStatus(parseString(statusJson, "State"))
				case _ => null
			},
			document = submissionJson.get("Document") match {
				case Some(documentJson: Map[String, Any] @unchecked) =>
					Some(UrkundDocument(
						id = parseInt(documentJson, "Id"),
						acceptedDate = parseDateTime(documentJson, "Date"),
						downloadUrl = parseString(documentJson, "DownloadUrl"),
						optOutUrl = documentJson.get("OptOutInfo") match {
							case Some(optOutJson: Map[String, Any] @unchecked) =>
								parseString(optOutJson, "Url")
							case _ => null
						}
					))
				case _ => None
			},
			report = submissionJson.get("Report") match {
				case Some(reportJson: Map[String, Any] @unchecked) =>
					Some(UrkundReport(
						id = parseInt(reportJson, "Id"),
						reportUrl = parseString(reportJson, "ReportUrl"),
						significance = parseFloat(reportJson, "Significance"),
						matchCount = parseInt(reportJson, "MatchCount"),
						sourceCount = parseInt(reportJson, "SourceCount"),
						warnings = reportJson.get("Warnings") match {
							case Some(warningsJson: Seq[Map[String, Any]] @unchecked) =>
								warningsJson.map(warningJson =>
									UrkundReportWarning(
										warningType = parseString(warningJson, "WarningType"),
										excerpt = parseString(warningJson, "Excerpt"),
										message = parseString(warningJson, "Message")
									)
								)
							case _ => null
						}
					))
				case _ => None
			}
		)
	}

	def fromSuccessJson(statusCode: Int, json: String): UrkundSuccessResponse = {
		JSON.parseFull(json) match {
			case Some(submissionJson: Map[String, Any] @unchecked) =>
				parseResponse(statusCode, json, submissionJson)
			case Some(submissionJsonSeq: Seq[Map[String, Any]] @unchecked) =>
				// If the same report has been submitted multiple times we get a list of responses
				// Use the last one
				submissionJsonSeq.map(submissionJson => parseResponse(statusCode, "", submissionJson)).sortBy(_.timestamp).last.copy(response = json)
			case unknownJson => throw new IllegalArgumentException(s"Cannot parse JSON; unexpected type: ${unknownJson.getClass.toString}")
		}
	}
}