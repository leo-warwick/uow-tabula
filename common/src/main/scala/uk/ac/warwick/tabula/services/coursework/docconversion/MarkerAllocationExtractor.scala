package uk.ac.warwick.tabula.services.coursework.docconversion

import java.io.InputStream

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.coursework.docconversion.MarkerAllocationExtractor._
import uk.ac.warwick.tabula.data.model.MarkingWorkflow
import uk.ac.warwick.tabula.helpers.{FoundUser, SpreadsheetHelpers}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.userlookup.User

object MarkerAllocationExtractor {
	case class Error(field: String, rowData: Map[String, String], code: String, codeArgument: Array[Object] = Array())
	val AcceptedFileExtensions = Seq(".xlsx")

	sealed trait MarkerPosition
	case object FirstMarker extends MarkerPosition
	case object SecondMarker extends MarkerPosition
	case object NoMarker extends MarkerPosition

	case class ParsedRow (
		marker: Option[User],
		student: Option[User],
		errors: Seq[Error],
		position: MarkerPosition
	)

	case class Markers(firstMarkers: Seq[User], secondMarkers: Seq[User]) {
		def allMarkers: Seq[User] = firstMarkers ++ secondMarkers
	}
}

@Service
class MarkerAllocationExtractor() {

	@transient var userLookup: UserLookupService = Wire[UserLookupService]

	def extractMarkersFromSpreadsheet(file: InputStream, workflow: MarkingWorkflow): Map[MarkerPosition, Seq[ParsedRow]] = {

		val firstMarkers = workflow.firstMarkers.users
		val secondMarkers = workflow.secondMarkers.users

		val rowData = SpreadsheetHelpers.parseXSSFExcelFile(file)
		rowData
			.map(parseRow(_, workflow, Markers(firstMarkers, secondMarkers)))
			.groupBy(_.position)
	}

	def parseRow(rowData: Map[String, String],
							 workflow: MarkingWorkflow,
							 markers: Markers): ParsedRow = {

		def getUser(id: String): Option[User] = {
			val user = userLookup.getUserByWarwickUniId(id)
			if (user.isFoundUser) Some(user)
			else None
		}

		def parseStudent: Either[Error, User] = {
			rowData.get("student_id") match {
				case Some(studentId) if studentId.matches("\\d+") =>
					getUser(studentId)
						.map(Right(_))
						.getOrElse(Left(Error("student_id", rowData, "workflow.allocateMarkers.universityId.notFound")))
				case Some(studentId) => Left(Error("student_id", rowData,"workflow.allocateMarkers.universityId.badFormat"))
				case None => Left(Error("student_id", rowData, "workflow.allocateMarkers.universityId.missing"))
			}
		}

		def parseMarker(validMarkers: Seq[User], allMarkers: Seq[User], roleName: String) = {
			rowData.get("agent_id") match {
				case Some(markerId) if markerId.hasText =>
					validMarkers.find { user => user.getWarwickId == markerId && getUser(markerId).exists(_.getUserId == user.getUserId) } match {
						case Some(FoundUser(user)) => Right(Some(user))
						case _ if validMarkers.exists(_.getWarwickId == markerId) =>
							Left(Error("marker_id", rowData, "workflow.allocateMarkers.nonPrimary",
								Array(
									validMarkers.find(_.getWarwickId == markerId).get.getUserId, // usercode in marking workflow
									getUser(markerId).get.getUserId // actual primary usercode
								)
							))
						case _ if allMarkers.exists(_.getWarwickId == markerId) =>
							Left(Error("marker_id", rowData, "workflow.allocateMarkers.wrongRole", Array(roleName)))
						case _ if getUser(markerId).nonEmpty =>
							Left(Error("marker_id", rowData, "workflow.allocateMarkers.notMarker"))
						case _ =>
							Left(Error("marker_id", rowData, "workflow.allocateMarkers.universityId.notFound"))
					}
				case _ =>
					Left(Error("marker_id", rowData, "workflow.allocateMarkers.universityId.notFound"))
			}
		}

		def parsePosition = {
			def sanitiseRoleName(roleName: String) = roleName.toLowerCase.replace(" ","_")
			if (rowData.contains(s"${sanitiseRoleName(workflow.firstMarkerRoleName)}_name")) FirstMarker
			else if (rowData.contains(s"${workflow.secondMarkerRoleName.map(sanitiseRoleName).getOrElse("")}_name")) SecondMarker
			else NoMarker
		}

		val student = parseStudent

		val position = parsePosition
		val marker = position match {
			case FirstMarker => parseMarker(markers.firstMarkers, markers.allMarkers, workflow.firstMarkerRoleName)
			case SecondMarker => parseMarker(markers.secondMarkers, markers.allMarkers, workflow.secondMarkerRoleName.getOrElse(""))
			case _ if !rowData.get("agent_id").exists(_.hasText) => Right(None)
			case _ => Left(Error("marker_id", rowData, "workflow.allocateMarkers.unableToWorkOutRole"))
		}

		val errors = Seq(student, marker).flatMap(_.left.toOption)
		ParsedRow(marker.right.toOption.flatten, student.right.toOption, errors, position)
	}
}
