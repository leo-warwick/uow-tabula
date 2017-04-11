package uk.ac.warwick.tabula.services.profiles.docconversion

import java.io.InputStream

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.UniversityId
import uk.ac.warwick.tabula.data.model.{Department, Member, StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.helpers.SpreadsheetHelpers
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.services.profiles.docconversion.RawStudentRelationshipExtractor.{ErrorCode, ParsedRow}

import scala.util.Left

object RawStudentRelationshipExtractor {
	type RowData = Map[String, String]
	type RawStudentRelationship = (Member, Option[Member])
	type ErrorCode = (String, String)

	type ParsedRow = (RowData, Option[RawStudentRelationship], Seq[ErrorCode])

	val AcceptedFileExtensions = Seq(".xlsx")
}

class RawStudentRelationshipRow(relationshipType: StudentRelationshipType, val rowData: Map[String, String]) {

	def extractStudent(department: Department, memberMap: Map[String, Member]): (Option[StudentMember], Option[ErrorCode]) = {
		def validateCourseAndDepartmentDetails(student: StudentMember): Option[ErrorCode] = {
			student.mostSignificantCourseDetails match {
				case Some(scd) if scd.department == null =>
					Some("student_id" -> "profiles.relationship.allocate.student.noDepartment")
				case Some(scd) if !student.affiliatedDepartments.contains(department) =>
					Some("student_id" -> "profiles.relationship.allocate.student.wrongDepartment")
				case Some(scd) if relationshipType.readOnly(scd.department) || relationshipType.readOnly(department) =>
					Some("student_id" -> "profiles.relationship.allocate.student.readOnlyDepartment")
				case Some(scd) => None
				case None => Some("student_id" -> "profiles.relationship.allocate.student.noCourseDetails")
			}
		}

		rowData("student_id") match {
			case strStudentId if strStudentId.matches("\\d+") =>
				val studentId = UniversityId.zeroPad(strStudentId)

				memberMap.get(studentId) match {
					case Some(student: StudentMember) =>
						(Some(student), validateCourseAndDepartmentDetails(student))

					case Some(member) => // non-student member
						(None, Some("student_id" -> "profiles.relationship.allocate.universityId.notStudent"))

					case _ => (None, Some("student_id" -> "profiles.relationship.allocate.universityId.notMember"))
				}
			case _ => (None, Some("student_id" -> "profiles.relationship.allocate.universityId.badFormat"))
		}
	}

	def extractAgent(memberMap: Map[String, Member]): Either[Option[Member], ErrorCode] = {
		rowData.get("agent_id") match {
			case Some(strAgentId) if strAgentId.hasText && strAgentId.matches("\\d+") =>
				val agentId = UniversityId.zeroPad(strAgentId)

				memberMap.get(agentId) match {
					case Some(member) => Left(Some(member))
					case _ => Right("agent_id" -> "profiles.relationship.allocate.universityId.notMember")
				}
			case Some("ERROR:#N/A") | None => Left(None)
			case _ => Right("agent_id" -> "profiles.relationship.allocate.universityId.badFormat")
		}
	}

	// Only if there is a student ID in the row
	def isValid: Boolean = rowData.contains("student_id") && rowData("student_id").hasText
}

@Service
class RawStudentRelationshipExtractor {

	var profileService: ProfileService = Wire[ProfileService]

	/**
	 * Method for reading in a xlsx spreadsheet and converting it into a list of relationships
	 */
	def readXSSFExcelFile(file: InputStream, relationshipType: StudentRelationshipType, department: Department): Seq[ParsedRow] = {
		val validRows = SpreadsheetHelpers.parseXSSFExcelFile(file)
			.map { rowData => new RawStudentRelationshipRow(relationshipType, rowData)}
			.filter {	_.isValid } // Ignore blank rows

		val memberMap = profileService.getAllMembersWithUniversityIds(
			validRows.flatMap(r => Seq(r.rowData("student_id"), r.rowData("agent_id")))
				.filter(s => s.hasText && s.matches("\\d+"))
				.map(UniversityId.zeroPad)
		).map(m => m.universityId -> m).toMap

		validRows.map { row =>
			val (student, studentError) = row.extractStudent(department, memberMap)
			val agentOrError = row.extractAgent(memberMap)

			val relationship = student.map { student =>
				agentOrError match {
					case Left(agent) => student -> agent
					case _ => student -> None
				}
			}

			val errors = agentOrError match {
				case Right(agentError) => Seq(studentError).flatten :+ agentError
				case _ => Seq(studentError).flatten
			}

			(row.rowData, relationship, errors)
		}
	}
}
