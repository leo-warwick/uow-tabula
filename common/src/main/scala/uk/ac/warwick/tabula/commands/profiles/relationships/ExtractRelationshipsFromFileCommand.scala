package uk.ac.warwick.tabula.commands.profiles.relationships

import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.{LazyMaps, SpreadsheetHelpers}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, AutowiringRelationshipServiceComponent, ProfileServiceComponent, RelationshipServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._
import scala.collection.mutable

case class ExtractRelationshipsFromFileCommandRow(
	studentId: String,
	studentName: String,
	agentId: String,
	agentName: String,
	error: String
)

object ExtractRelationshipsFromFileCommand {

	val AcceptedFileExtensions = Seq(".xlsx")
	val MaxUploadRows = 5000
	val StudentColumnHeader = "student_id"
	val EntityColumnHeader = "agent_id"

	object AllocationTypes {
		val Replace = "Replace"
		val Add = "Add"
	}

	def apply(department: Department, relationshipType: StudentRelationshipType) =
		new ExtractRelationshipsFromFileCommandInternal(department, relationshipType)
			with AutowiringRelationshipServiceComponent
			with AutowiringProfileServiceComponent
			with ComposableCommand[Seq[ExtractRelationshipsFromFileCommandRow]]
			with ExtractRelationshipsFromFilePermissions
			with ExtractRelationshipsFromFileCommandState
			with ExtractRelationshipsFromFileCommandRequest
			with ExtractRelationshipsFromFileCommandBindListener
			with ReadOnly with Unaudited
}


class ExtractRelationshipsFromFileCommandInternal(val department: Department, val relationshipType: StudentRelationshipType)
	extends CommandInternal[Seq[ExtractRelationshipsFromFileCommandRow]] {

	self: ExtractRelationshipsFromFileCommandRequest with RelationshipServiceComponent with ProfileServiceComponent =>

	override def applyInternal(): Seq[ExtractRelationshipsFromFileCommandRow] = {
		val dbUnallocated = relationshipService.getStudentAssociationDataWithoutRelationship(department, relationshipType)
		val dbAllocated = relationshipService.getStudentAssociationEntityData(department, relationshipType, Seq())
		val allStudents = dbUnallocated ++ dbAllocated.flatMap(_.students).distinct
		val allIDs = fileData.flatMap{case(studentId, agents) => Seq(studentId) ++ agents}.toSeq.distinct
		val memberMap = profileService.getAllMembersWithUniversityIdsStaleOrFresh(allIDs).map(m => m.universityId -> m).toMap
		def isStudent(m: Member) = m match {
			case s: StudentMember => true
			case _ => false
		}
		val result = fileData.flatMap{case(studentId, agents) => agents.map{agentId =>
			if (memberMap.get(studentId).isEmpty) {
				ExtractRelationshipsFromFileCommandRow(studentId, "", agentId, "", "profiles.relationship.allocate.universityId.notStudent")
			} else if (memberMap.get(agentId).isEmpty) {
				ExtractRelationshipsFromFileCommandRow(studentId, "", agentId, "", "profiles.relationship.allocate.universityId.notMember")
			} else if (!isStudent(memberMap(studentId))) {
				ExtractRelationshipsFromFileCommandRow(studentId, "", agentId, "", "profiles.relationship.allocate.universityId.notStudent")
			} else {
				val studentMember = memberMap(studentId)
				val agentMember = memberMap(agentId)
				val studentName = studentMember.fullName.getOrElse("")
				val agentName = agentMember.fullName.getOrElse("")
				if (!allStudents.exists(_.universityId == studentId)) {
					ExtractRelationshipsFromFileCommandRow(studentId, studentName, agentId, agentName, "profiles.relationship.allocate.student.wrongDepartment")
				} else {
					ExtractRelationshipsFromFileCommandRow(studentId, studentName, agentId, agentName, "")
				}
			}
		}}.toSeq
		additions.putAll(result.filter(r => !r.error.hasText).groupBy(_.agentId).mapValues(_.map(_.studentId).asJava).asJava)
		additionalEntities.addAll(additions.keySet().asScala.filterNot(id => dbAllocated.exists(_.entityId == id)).asJava)
		result
	}

}

trait ExtractRelationshipsFromFileCommandBindListener extends BindListener {

	self: ExtractRelationshipsFromFileCommandRequest =>

	override def onBind(result: BindingResult): Unit = {
		val fileNames = file.fileNames map (_.toLowerCase)
		val invalidFiles = fileNames.filter(s => !ExtractRelationshipsFromFileCommand.AcceptedFileExtensions.exists(s.endsWith))

		if (invalidFiles.nonEmpty) {
			result.reject("file.wrongtype", Array(invalidFiles.mkString(", ")), "")
		}

		if (!result.hasErrors) {
			transactional() {
				file.onBind(result)
				if (!file.attached.isEmpty) {
					file.attached.asScala.filter(_.hasData).foreach{attachment =>
						extractDataFromFile(attachment, result).foreach{case(studentId, agents) =>
							fileData.update(studentId, fileData.getOrElse(studentId, mutable.Buffer()) ++ agents)
						}
					}
				}
			}
		}
	}

	private def extractDataFromFile(attachment: FileAttachment, result: BindingResult): Map[String, Seq[String]] = {
		val rows = SpreadsheetHelpers.parseXSSFExcelFile(attachment.dataStream)
		if (rows.size > ExtractRelationshipsFromFileCommand.MaxUploadRows) {
			result.rejectValue("file", "file.tooManyRows", Array(ExtractRelationshipsFromFileCommand.MaxUploadRows.toString), "")
			Map()
		} else {
			rows.filter(row =>
				// Throw away any rows that don't have the required headers and that have empty cells
				row.keySet.contains(ExtractRelationshipsFromFileCommand.StudentColumnHeader) &&
					row.keySet.contains(ExtractRelationshipsFromFileCommand.EntityColumnHeader) &&
					row(ExtractRelationshipsFromFileCommand.StudentColumnHeader).hasText &&
					row(ExtractRelationshipsFromFileCommand.EntityColumnHeader).hasText
			).map(row =>
				(row(ExtractRelationshipsFromFileCommand.StudentColumnHeader), row(ExtractRelationshipsFromFileCommand.EntityColumnHeader))
			).groupBy(_._1).mapValues(_.map(_._2))
		}
	}
}

trait ExtractRelationshipsFromFilePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ExtractRelationshipsFromFileCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		// throw this request out if this relationship can't be edited in Tabula for this department
		if (relationshipType.readOnly(department)) {
			logger.info("Denying access to ExtractRelationshipsFromFileCommand since relationshipType %s is read-only".format(relationshipType))
			throw new ItemNotFoundException()
		}
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Manage(mandatory(relationshipType)), mandatory(department))
	}

}

trait ExtractRelationshipsFromFileCommandState {
	def department: Department
	def relationshipType: StudentRelationshipType
}

trait ExtractRelationshipsFromFileCommandRequest {
	var file: UploadedFile = new UploadedFile
	val fileData: mutable.Map[String, mutable.Buffer[String]] = mutable.Map()
	val additions: JMap[String, JList[String]] =
		LazyMaps.create{entityId: String => JArrayList(): JList[String] }.asJava
	var additionalEntities: JList[String] = JArrayList()
	var allocationType: String = ""
}
