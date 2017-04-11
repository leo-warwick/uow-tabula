package uk.ac.warwick.tabula.commands.exams.exams

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model.{Assessment, Assignment, Exam}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.views.ExcelView

object BulkAdjustmentTemplateCommand {
	def apply(assessment: Assessment) =
		new BulkAdjustmentTemplateCommandInternal(assessment)
			with AutowiringAssessmentMembershipServiceComponent
			with ComposableCommand[ExcelView]
			with BulkAdjustmentTemplatePermissions
			with BulkAdjustmentTemplateCommandState
			with ReadOnly with Unaudited
}


class BulkAdjustmentTemplateCommandInternal(val assessment: Assessment) extends CommandInternal[ExcelView] {

	self: AssessmentMembershipServiceComponent =>
	override def applyInternal(): ExcelView = {
		val workbook = generateWorkbook
		new ExcelView("Adjustments for " + assessment.name +  ".xlsx", workbook)
	}

	private def generateWorkbook: XSSFWorkbook = {
		val workbook = new XSSFWorkbook()
		val sheet = workbook.createSheet("Marks")

		// add header row
		val header = sheet.createRow(0)
		header.createCell(0).setCellValue(BulkAdjustmentCommand.StudentIdHeader)
		header.createCell(1).setCellValue("Original mark")
		header.createCell(2).setCellValue("Original grade")
		header.createCell(3).setCellValue("Previous adjusted mark (if any)")
		header.createCell(4).setCellValue("Previous adjusted grade (if any)")
		header.createCell(5).setCellValue(BulkAdjustmentCommand.MarkHeader)
		header.createCell(6).setCellValue(BulkAdjustmentCommand.GradeHeader)
		header.createCell(7).setCellValue(BulkAdjustmentCommand.ReasonHeader)
		header.createCell(8).setCellValue(BulkAdjustmentCommand.CommentsHeader)

		val memberOrder = assessmentMembershipService.determineMembershipUsers(assessment)
			.zipWithIndex.toMap.map{case(user, order) => user.getWarwickId -> order}

		assessment.fullFeedback
			.sortBy(f => {
				val order = for(uniId <- f.universityId; o <- memberOrder.get(uniId)) yield o
				order.getOrElse(10000)
			})
			.foreach(f => {
				val row = sheet.createRow(sheet.getLastRowNum + 1)
				row.createCell(0).setCellValue(f.studentIdentifier)
				row.createCell(1).setCellValue(f.actualMark.map(_.toString).getOrElse(""))
				row.createCell(2).setCellValue(f.actualGrade.getOrElse(""))
				row.createCell(3).setCellValue(f.latestPrivateOrNonPrivateAdjustment.map(_.mark.toString).getOrElse(""))
				row.createCell(4).setCellValue(
					f.latestPrivateOrNonPrivateAdjustment.flatMap(_.grade.map(_.toString)).getOrElse("")
				)
			})

		val style = workbook.createCellStyle
		// using an @ sets text format (from BuiltinFormats.class)
		style.setDataFormat(workbook.createDataFormat.getFormat("@"))
		sheet.setDefaultColumnStyle(0, style)

		0 to 8 foreach sheet.autoSizeColumn

		workbook
	}

}

trait BulkAdjustmentTemplatePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: BulkAdjustmentTemplateCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		HibernateHelpers.initialiseAndUnproxy(mandatory(assessment)) match {
			case assignment: Assignment =>
				p.PermissionCheck(Permissions.AssignmentFeedback.Manage, assignment)
			case exam: Exam =>
				p.PermissionCheck(Permissions.ExamFeedback.Manage, exam)
		}
	}

}

trait BulkAdjustmentTemplateCommandState {
	def assessment: Assessment
}
