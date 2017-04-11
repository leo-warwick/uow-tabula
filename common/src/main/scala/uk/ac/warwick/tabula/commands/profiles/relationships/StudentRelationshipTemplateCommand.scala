package uk.ac.warwick.tabula.commands.profiles.relationships

import org.apache.poi.ss.util.{CellRangeAddressList, WorkbookUtil}
import org.apache.poi.xssf.usermodel._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Department, StudentRelationshipType}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringRelationshipServiceComponent, RelationshipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.views.ExcelView

import scala.collection.JavaConverters._

object StudentRelationshipTemplateCommand {

	val agentLookupSheetName = "AgentLookup"
	val sheetPassword = "roygbiv"

	def apply(department: Department, relationshipType: StudentRelationshipType) =
		new StudentRelationshipTemplateCommandInternal(department, relationshipType)
			with AutowiringRelationshipServiceComponent
			with ComposableCommand[ExcelView]
			with StudentRelationshipTemplatePermissions
			with StudentRelationshipTemplateCommandState
			with StudentRelationshipTemplateCommandRequest
			with ReadOnly with Unaudited
}


class StudentRelationshipTemplateCommandInternal(val department: Department, val relationshipType: StudentRelationshipType)
	extends CommandInternal[ExcelView] {

	self: RelationshipServiceComponent with StudentRelationshipTemplateCommandRequest =>

	override def applyInternal(): ExcelView = {
		val dbUnallocated = relationshipService.getStudentAssociationDataWithoutRelationship(department, relationshipType)
		val dbAllocated = relationshipService.getStudentAssociationEntityData(department, relationshipType, additionalEntities.asScala)

		val (unallocated, allocationsForTemplate) = {
			if (templateWithChanges) {
				val allocatedWithRemovals = dbAllocated.map(entityData => {
					if (removals.containsKey(entityData.entityId)) {
						val theseRemovals = removals.get(entityData.entityId).asScala
						entityData.updateStudents(entityData.students.filterNot(student => theseRemovals.contains(student.universityId)))
					} else {
						entityData
					}
				})

				val allocatedWithAdditionsAndRemovals = allocatedWithRemovals.map(entityData => {
					if (additions.containsKey(entityData.entityId)) {
						entityData.updateStudents(entityData.students ++ additions.get(entityData.entityId).asScala.flatMap(universityId => dbUnallocated.find(_.universityId == universityId)))
					} else {
						entityData
					}
				})

				val allDbAllocatedStudents = dbAllocated.flatMap(_.students).distinct
				val allAllocatedStudents = allocatedWithAdditionsAndRemovals.flatMap(_.students).distinct
				val newUnallocated = allDbAllocatedStudents.filterNot(allAllocatedStudents.contains)
				val newlyAllocatedUniIds = additions.asScala.mapValues(_.asScala).values.flatten.toSeq
				val unallocatedWithAdditionsAndRemovals =  newUnallocated ++ dbUnallocated.filterNot(student => newlyAllocatedUniIds.contains(student.universityId))
				(unallocatedWithAdditionsAndRemovals, allocatedWithAdditionsAndRemovals)
			} else {
				(dbUnallocated, dbAllocated)
			}
		}

		new ExcelView("Allocation for " + allocateSheetName + ".xlsx", generateWorkbook(unallocated, allocationsForTemplate))
	}

	private def generateWorkbook(unallocated: Seq[StudentAssociationData], allocations: Seq[StudentAssociationEntityData]) = {
		val workbook = new XSSFWorkbook()
		val sheet: XSSFSheet = generateAllocationSheet(workbook)
		generateAgentLookupSheet(workbook, allocations)
		generateAgentDropdowns(sheet, allocations)

		val agentLookupRange = StudentRelationshipTemplateCommand.agentLookupSheetName + "!$A2:$B" + (allocations.length + 1)

		allocations.foreach{ agent =>
			agent.students.foreach{ student =>
				val row = sheet.createRow(sheet.getLastRowNum + 1)

				row.createCell(0).setCellValue(student.universityId)
				row.createCell(1).setCellValue(s"${student.firstName} ${student.lastName}")

				val agentNameCell = createUnprotectedCell(workbook, row, 2) // unprotect cell for the dropdown agent name
				agentNameCell.setCellValue(agent.displayName)

				row.createCell(3).setCellFormula(
					"IF(ISTEXT($C" + (row.getRowNum + 1) + "), VLOOKUP($C" + (row.getRowNum + 1) + ", " + agentLookupRange + ", 2, FALSE), \" \")"
				)
			}
		}

		unallocated.foreach{ student =>
			val row = sheet.createRow(sheet.getLastRowNum + 1)

			row.createCell(0).setCellValue(student.universityId)
			row.createCell(1).setCellValue(s"${student.firstName} ${student.lastName}")

			createUnprotectedCell(workbook, row, 2) // unprotect cell for the dropdown agent name

			row.createCell(3).setCellFormula(
				"IF(ISTEXT($C" + (row.getRowNum + 1) + "), VLOOKUP($C" + (row.getRowNum + 1) + ", " + agentLookupRange + ", 2, FALSE), \" \")"
			)
		}

		formatWorkbook(workbook)
		workbook
	}

	private def generateAgentLookupSheet(workbook: XSSFWorkbook, allocations: Seq[StudentAssociationEntityData]) = {
		val agentSheet: XSSFSheet = workbook.createSheet(StudentRelationshipTemplateCommand.agentLookupSheetName)

		for (agent <- allocations) {
			val row = agentSheet.createRow(agentSheet.getLastRowNum + 1)
			row.createCell(0).setCellValue(agent.displayName)
			row.createCell(1).setCellValue(agent.entityId)
		}

		agentSheet.protectSheet(StudentRelationshipTemplateCommand.sheetPassword)
		agentSheet
	}

	// attaches the data validation to the sheet
	private def generateAgentDropdowns(sheet: XSSFSheet, allocations: Seq[StudentAssociationEntityData]) {
		if (allocations.nonEmpty) {
			val dropdownRange = new CellRangeAddressList(1, allocations.flatMap(_.students).length, 2, 2)
			val validation = getDataValidation(allocations, sheet, dropdownRange)

			sheet.addValidationData(validation)
		}
	}

	// Excel data validation - will only accept the values fed to this method, also puts a dropdown on each cell
	private def getDataValidation(allocations: Seq[StudentAssociationEntityData], sheet: XSSFSheet, addressList: CellRangeAddressList) = {
		val dvHelper = new XSSFDataValidationHelper(sheet)
		val dvConstraint = dvHelper.createFormulaListConstraint(
			StudentRelationshipTemplateCommand.agentLookupSheetName + "!$A$2:$A$" + (allocations.length + 1)
		).asInstanceOf[XSSFDataValidationConstraint]
		val validation = dvHelper.createValidation(dvConstraint, addressList).asInstanceOf[XSSFDataValidation]

		validation.setShowErrorBox(true)
		validation
	}

	private def createUnprotectedCell(workbook: XSSFWorkbook, row: XSSFRow, col: Int, value: String = "") = {
		val lockedCellStyle = workbook.createCellStyle()
		lockedCellStyle.setLocked(false)
		val cell = row.createCell(col)
		cell.setCellValue(value)
		cell.setCellStyle(lockedCellStyle)
		cell
	}

	private def formatWorkbook(workbook: XSSFWorkbook) = {
		val style = workbook.createCellStyle
		val format = workbook.createDataFormat

		// using an @ sets text format (from BuiltinFormats.class)
		style.setDataFormat(format.getFormat("@"))

		val sheet = workbook.getSheet(allocateSheetName)

		// set style on all columns
		0 to 3 foreach  { col =>
			sheet.setDefaultColumnStyle(col, style)
			sheet.autoSizeColumn(col)
		}

		// set ID column to be wider
		sheet.setColumnWidth(3, 7000)

	}

	private def generateAllocationSheet(workbook: XSSFWorkbook): XSSFSheet =  {
		val sheet = workbook.createSheet(allocateSheetName)

		// add header row
		val header = sheet.createRow(0)
		header.createCell(0).setCellValue("student_id")
		header.createCell(1).setCellValue(relationshipType.studentRole.capitalize + " name")
		header.createCell(2).setCellValue(relationshipType.agentRole.capitalize + " name")
		header.createCell(3).setCellValue("agent_id")

		// using apache-poi, we can't protect certain cells - rather we have to protect
		// the entire sheet and then unprotect the ones we want to remain editable
		sheet.protectSheet(StudentRelationshipTemplateCommand.sheetPassword)
		sheet
	}

	def allocateSheetName: String = trimmedSheetName(relationshipType.agentRole.capitalize + "s for " + department.name)

	// Excel sheet names must be 31 chars or less so
	private def trimmedSheetName(rawSheetName: String) = {
		val sheetName = WorkbookUtil.createSafeSheetName(rawSheetName)

		if (sheetName.length > 31) sheetName.substring(0, 31)
		else sheetName
	}

}

trait StudentRelationshipTemplatePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: StudentRelationshipTemplateCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Read(mandatory(relationshipType)), department)
	}

}

trait StudentRelationshipTemplateCommandState {
	def department: Department
	def relationshipType: StudentRelationshipType
}

trait StudentRelationshipTemplateCommandRequest {
	var additions: JMap[String, JList[String]] =
		LazyMaps.create{entityId: String => JArrayList(): JList[String] }.asJava

	var removals: JMap[String, JList[String]] =
		LazyMaps.create{entityId: String => JArrayList(): JList[String] }.asJava

	var additionalEntities: JList[String] = JArrayList()

	var templateWithChanges = false
}
