package uk.ac.warwick.tabula.commands.coursework.turnitinlti

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.tabula.services.{AssessmentServiceComponent, AutowiringAssessmentServiceComponent}
import scala.xml.Elem

object TurnitinLtiConformanceOutcomesServiceCommand {
	def apply(assignment: Assignment) =
		new TurnitinLtiConformanceOutcomesServiceCommandInternal(assignment)
			with ComposableCommand[TurnitinLtiConformanceOutcomesServiceCommandResponse]
			with TurnitinLtiConformanceOutcomesServiceValidation
			with AutowiringAssessmentServiceComponent
			with Unaudited with Logging with PubliclyVisiblePermissions
			with TurnitinLtiConformanceOutcomesServiceCommandState
		}

abstract class TurnitinLtiConformanceOutcomesServiceCommandInternal(val assignment: Assignment)
	extends CommandInternal[TurnitinLtiConformanceOutcomesServiceCommandResponse]
		with TurnitinLtiConformanceOutcomesServiceRequest with AssessmentServiceComponent {

	self: Logging with TurnitinLtiConformanceOutcomesServiceCommandState =>

	def applyInternal(): TurnitinLtiConformanceOutcomesServiceCommandResponse = {

		val incomingXml = scala.xml.XML.loadString(body)

		val replaceResultRequest = (incomingXml \\ "replaceResultRequest").text
		val readResultRequest = (incomingXml \\ "readResultRequest").text

		if (replaceResultRequest.nonEmpty) applyReplaceResultRequest(incomingXml)
		else if (readResultRequest.nonEmpty) applyReadResultRequest(incomingXml)
		else applyDeleteResultRequest(incomingXml)

	}

	/*
		A deleteResultRequest callback will remove a turnitin id for an assignment
	 */
	private def applyDeleteResultRequest(incomingXml: Elem): TurnitinLtiConformanceOutcomesServiceCommandResponse = {
		val sourcedId = (incomingXml \\ "sourcedId").text

		val assignment = assessmentService.getAssignmentById(sourcedId).get

		val imsx_codeMajor = {
			if ((incomingXml \\ "deleteResultRequest").text.nonEmpty) {
				assignment.turnitinId = ""
				assessmentService.save(assignment)
				"success"
			}
			else "unsupported"
		}

		val messageIdentifier = (incomingXml \\ "imsx_messageIdentifier").text

		val xmlresponsestring = s"""<?xml version="1.0" encoding="utf-8"?>
			<imsx_POXEnvelopeResponse xmlns = "http://www.imsglobal.org/services/ltiv1p1/xsd/imsoms_v1p0">
				<imsx_POXHeader>
					<imsx_POXResponseHeaderInfo>
						<imsx_version>V1.0</imsx_version>
						<imsx_messageIdentifier>$messageIdentifier-message</imsx_messageIdentifier>
						<imsx_statusInfo>
							<imsx_codeMajor>$imsx_codeMajor</imsx_codeMajor>
							<imsx_severity>status</imsx_severity>
							<imsx_messageRefIdentifier>$messageIdentifier</imsx_messageRefIdentifier>
							<imsx_operationRefIdentifier>deleteResult</imsx_operationRefIdentifier>
						</imsx_statusInfo>
					</imsx_POXResponseHeaderInfo>
				</imsx_POXHeader>
				<imsx_POXBody>
					<deleteResultResponse/>
				</imsx_POXBody>
			</imsx_POXEnvelopeResponse>
			"""

		TurnitinLtiConformanceOutcomesServiceCommandResponse(xmlresponsestring)
	}

	/*
	A readResultRequest callback will return the current turnitin id for an assignment
 */
	private def applyReadResultRequest(incomingXml: Elem): TurnitinLtiConformanceOutcomesServiceCommandResponse = {
		val sourcedId = (incomingXml \\ "sourcedId").text

		val assignment = assessmentService.getAssignmentById(sourcedId).get
		val score = {
			if (assignment.turnitinId !=null && assignment.turnitinId.nonEmpty) assignment.turnitinId
			else ""
		}

		val imsx_codeMajor = {
			if ((incomingXml \\ "readResultRequest").text.nonEmpty) "success"
			else "unsupported"
		}

		val messageIdentifier = (incomingXml \\ "imsx_messageIdentifier").text
		val version = (incomingXml \\ "imsx_version").text

		val xmlresponsestring = s"""<?xml version="1.0" encoding="utf-8"?>
			<imsx_POXEnvelopeResponse xmlns = "http://www.imsglobal.org/services/ltiv1p1/xsd/imsoms_v1p0">
				<imsx_POXHeader>
					<imsx_POXResponseHeaderInfo>
						<imsx_version>$version</imsx_version>
						<imsx_messageIdentifier>$messageIdentifier-message</imsx_messageIdentifier>
						<imsx_statusInfo>
							<imsx_codeMajor>$imsx_codeMajor</imsx_codeMajor>
							<imsx_severity>status</imsx_severity>
							<imsx_description>Result read</imsx_description>
							<imsx_messageRefIdentifier>$messageIdentifier</imsx_messageRefIdentifier>
							<imsx_operationRefIdentifier>readResult</imsx_operationRefIdentifier>
						</imsx_statusInfo>
					</imsx_POXResponseHeaderInfo>
				</imsx_POXHeader>
				<imsx_POXBody>
					<readResultResponse>
						<result>
							<resultScore>
								<language>en</language>
								<textString>$score</textString>
							</resultScore>
						</result>
					</readResultResponse>
				</imsx_POXBody>
			</imsx_POXEnvelopeResponse>
			"""

		TurnitinLtiConformanceOutcomesServiceCommandResponse(xmlresponsestring)
	}

	/*
	A replaceResultRequest callback will update a turnitin id for an assignment
 */
	private def applyReplaceResultRequest(incomingXml: Elem): TurnitinLtiConformanceOutcomesServiceCommandResponse = {
		val sourcedId = (incomingXml \\ "sourcedId").text

		val assignment = assessmentService.getAssignmentById(sourcedId).get

		val score = (incomingXml \\ "textString").text
		val messageIdentifier = (incomingXml \\ "imsx_messageIdentifier").text
		val version = (incomingXml \\ "imsx_version").text

		val (imsx_codeMajor, score_double) =
			try {
				score.toDouble match {
					case s if 0<=s && s<= 1 => {
						assignment.turnitinId = score
						assessmentService.save(assignment)
						("success", score)
					}
					case s if 0>s || s>1  => ("failure", -1)
					case _ => ("unsupported", -1)
				}} catch {
				case e: NumberFormatException => ("failure", -1)
			}

		val xmlresponsestring = s"""<?xml version="1.0" encoding="utf-8"?>
			<imsx_POXEnvelopeResponse xmlns = "http://www.imsglobal.org/services/ltiv1p1/xsd/imsoms_v1p0">
				<imsx_POXHeader>
					<imsx_POXResponseHeaderInfo>
						<imsx_version>${version}</imsx_version>
						<imsx_messageIdentifier>${messageIdentifier}-message</imsx_messageIdentifier>
						<imsx_statusInfo>
							<imsx_codeMajor>${imsx_codeMajor}</imsx_codeMajor>
							<imsx_severity>status</imsx_severity>
							<imsx_description>Score for ${sourcedId} is now ${score_double}</imsx_description>
							<imsx_messageRefIdentifier>${messageIdentifier}</imsx_messageRefIdentifier>
							<imsx_operationRefIdentifier>replaceResult</imsx_operationRefIdentifier>
						</imsx_statusInfo>
					</imsx_POXResponseHeaderInfo>
				</imsx_POXHeader>
				<imsx_POXBody>
					<replaceResultResponse/>
				</imsx_POXBody>
			</imsx_POXEnvelopeResponse>
			"""

		TurnitinLtiConformanceOutcomesServiceCommandResponse(xmlresponsestring)
	}

}

trait TurnitinLtiConformanceOutcomesServiceState {
	val assignment: Assignment
}

trait TurnitinLtiConformanceOutcomesServiceRequest extends TurnitinLtiConformanceOutcomesServiceState
	with CurrentSITSAcademicYear


trait TurnitinLtiConformanceOutcomesServiceValidation extends SelfValidating {
	self: TurnitinLtiConformanceOutcomesServiceRequest =>

	override def validate(errors: Errors) {
		if (assignment == null) {
			errors.rejectValue("assignment", "NotEmpty")
		}
	}
}

trait TurnitinLtiConformanceOutcomesServiceCommandState {
	var body: String = _
}

case class TurnitinLtiConformanceOutcomesServiceCommandResponse(xmlString: String)