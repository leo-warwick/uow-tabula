package uk.ac.warwick.tabula.commands.groups.admin

import org.apache.poi.ss.usermodel.Row.MissingCellPolicy
import org.apache.poi.ss.usermodel.{DateUtil, Sheet}
import org.apache.poi.ss.util.CellRangeAddressList
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.groups.admin.SmallGroupSetsSpreadsheetTemplateCommand._
import uk.ac.warwick.tabula.data.model.{AliasedMapLocation, Department, Location, MapLocation}
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, EventDeliveryMethod, OnlinePlatform, SmallGroupAllocationMethod, SmallGroupFormat, SmallGroupSet}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.groups.docconversion.{ExtractedSmallGroup, ExtractedSmallGroupEvent, ExtractedSmallGroupSet}
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.views.ExcelView

import scala.jdk.CollectionConverters._

object SmallGroupSetsSpreadsheetTemplateCommand {
  val RequiredPermission = Permissions.SmallGroups.ImportFromExternalSystem
  type CommandType = Appliable[ExcelView]

  def apply(department: Department, academicYear: AcademicYear): CommandType =
    new SmallGroupSetsSpreadsheetTemplateCommandInternal(department, academicYear)
      with ComposableCommand[ExcelView]
      with SmallGroupSetsSpreadsheetTemplatePermissions
      with AutowiringSmallGroupServiceComponent
      with ReadOnly with Unaudited
}

abstract class SmallGroupSetsSpreadsheetTemplateCommandInternal(val department: Department, val academicYear: AcademicYear)
  extends CommandInternal[ExcelView] with SmallGroupSetsSpreadsheetTemplateState {
  self: SmallGroupServiceComponent =>

  override def applyInternal(): ExcelView = {
    val workbook = generateWorkbook()
    new ExcelView(s"Small groups for ${department.name} - ${academicYear.startYear}-${academicYear.endYear}.xlsx", workbook)
  }

  // Publicly visible for testing
  def generateWorkbook(): SXSSFWorkbook = {
    val workbook = new SXSSFWorkbook
    workbook.setMissingCellPolicy(MissingCellPolicy.CREATE_NULL_AS_BLANK)

    val sets = smallGroupService.getSmallGroupSets(department, academicYear).sorted

    addSetsSheet(workbook, sets)
    addGroupsSheet(workbook, sets)
    addEventsSheet(workbook, sets)

    addLookupsSheet(workbook)

    workbook
  }

  private def addLookupsSheet(workbook: SXSSFWorkbook): Sheet = {
    val style = workbook.createCellStyle
    val format = workbook.createDataFormat

    style.setDataFormat(format.getFormat("@"))
    style.setFont({
      val font = workbook.createFont()
      font.setFontHeightInPoints(11)
      font.setFontName("Calibri")
      font
    })

    val sheet = workbook.createSheet("Lookups")
    sheet.trackAllColumnsForAutoSizing()

    // set style on all columns
    0 to 4 foreach { col =>
      sheet.setDefaultColumnStyle(col, style)
    }

    val header = sheet.createRow(0)

    header.setRowStyle({
      val bold = workbook.createCellStyle()
      bold.cloneStyleFrom(style)

      val font = workbook.createFont()
      font.setFontHeightInPoints(11)
      font.setFontName("Calibri")
      font.setBold(true)
      bold.setFont(font)

      bold
    })

    header.getCell(0).setCellValue("Small group set types")
    header.getCell(1).setCellValue("Allocation methods")
    header.getCell(2).setCellValue("Days of the week")
    header.getCell(3).setCellValue("Delivery types")
    header.getCell(4).setCellValue("Online delivery methods")

    (0 to 4).foreach { col =>
      header.getCell(col).setCellStyle(header.getRowStyle)
    }

    val rows = 1 to Seq(SmallGroupFormat.members.size, SmallGroupAllocationMethod.members.size, DayOfWeek.members.size, EventDeliveryMethod.values.size, OnlinePlatform.values.size).max + 1 map sheet.createRow

    SmallGroupFormat.members.zipWithIndex.foreach { case (f, index) =>
      val row = rows(index)
      row.getCell(0).setCellValue(f.description)
    }

    SmallGroupAllocationMethod.members.zipWithIndex.foreach { case (am, index) =>
      val row = rows(index)
      row.getCell(1).setCellValue(am.description)
    }

    DayOfWeek.members.zipWithIndex.foreach { case (day, index) =>
      val row = rows(index)
      row.getCell(2).setCellValue(day.name)
    }

    EventDeliveryMethod.values.zipWithIndex.foreach { case (method, index) =>
      val row = rows(index)
      row.getCell(3).setCellValue(method.entryName)
    }

    OnlinePlatform.values.zipWithIndex.foreach { case (platform, index) =>
      val row = rows(index)
      row.getCell(4).setCellValue(platform.entryName)
    }

    0 to 4 foreach { col =>
      sheet.autoSizeColumn(col)
    }

    sheet
  }

  private def addSetsSheet(workbook: SXSSFWorkbook, sets: Seq[SmallGroupSet]): Sheet = {
    val style = workbook.createCellStyle
    val format = workbook.createDataFormat

    style.setDataFormat(format.getFormat("@"))
    style.setFont({
      val font = workbook.createFont()
      font.setFontHeightInPoints(11)
      font.setFontName("Calibri")
      font
    })

    val sheet = workbook.createSheet("Sets")
    sheet.trackAllColumnsForAutoSizing()

    val header = sheet.createRow(0)

    header.setRowStyle({
      val bold = workbook.createCellStyle()
      bold.cloneStyleFrom(style)

      val font = workbook.createFont()
      font.setFontHeightInPoints(11)
      font.setFontName("Calibri")
      font.setBold(true)
      bold.setFont(font)

      bold
    })

    // set style on all columns
    ExtractedSmallGroupSet.AllColumns.zipWithIndex foreach { case (column, index) =>
      header.getCell(index).setCellValue(column)
      header.getCell(index).setCellStyle(header.getRowStyle)
    }

    sets.zipWithIndex.foreach { case (set, index) =>
      val row = sheet.createRow(index + 1)
      row.getCell(0).setCellValue(set.module.code.toUpperCase)
      row.getCell(1).setCellValue(set.format.description)
      row.getCell(2).setCellValue(set.name)
      row.getCell(3).setCellValue(set.allocationMethod.description)
      row.getCell(4).setCellValue(set.studentsCanSeeTutorName)
      row.getCell(5).setCellValue(set.studentsCanSeeOtherMembers)
      row.getCell(6).setCellValue(set.allowSelfGroupSwitching)
      row.getCell(7).setCellValue(Option(set.linkedDepartmentSmallGroupSet).map(_.name).orNull)
      row.getCell(8).setCellValue(set.collectAttendance)
    }

    if (sets.nonEmpty) {
      // Small group format validation
      {
        val dropdownRange = new CellRangeAddressList(1, sheet.getLastRowNum + 100, 1, 1)
        val dvHelper = new XSSFDataValidationHelper(null)
        val dvConstraint =
          dvHelper.createFormulaListConstraint("Lookups!$A$2:$A$" + (SmallGroupFormat.members.size + 1))
        val validation = dvHelper.createValidation(dvConstraint, dropdownRange)
        validation.setShowErrorBox(true)
        sheet.addValidationData(validation)
      }

      // Small group allocation method validation
      {
        val dropdownRange = new CellRangeAddressList(1, sheet.getLastRowNum + 100, 3, 3)
        val dvHelper = new XSSFDataValidationHelper(null)
        val dvConstraint =
          dvHelper.createFormulaListConstraint("Lookups!$B$2:$B$" + (SmallGroupAllocationMethod.members.size + 1))
        val validation = dvHelper.createValidation(dvConstraint, dropdownRange)
        validation.setShowErrorBox(true)
        sheet.addValidationData(validation)
      }
    }

    // set style on all columns
    0 to ExtractedSmallGroupSet.AllColumns.size foreach { index =>
      sheet.setDefaultColumnStyle(index, style)
      sheet.autoSizeColumn(index)
    }

    sheet
  }

  private def addGroupsSheet(workbook: SXSSFWorkbook, sets: Seq[SmallGroupSet]): Sheet = {
    val style = workbook.createCellStyle
    val format = workbook.createDataFormat

    style.setDataFormat(format.getFormat("@"))
    style.setFont({
      val font = workbook.createFont()
      font.setFontHeightInPoints(11)
      font.setFontName("Calibri")
      font
    })

    val sheet = workbook.createSheet("Groups")
    sheet.trackAllColumnsForAutoSizing()

    val header = sheet.createRow(0)

    header.setRowStyle({
      val bold = workbook.createCellStyle()
      bold.cloneStyleFrom(style)

      val font = workbook.createFont()
      font.setFontHeightInPoints(11)
      font.setFontName("Calibri")
      font.setBold(true)
      bold.setFont(font)

      bold
    })

    // set style on all columns
    ExtractedSmallGroup.AllColumns.zipWithIndex foreach { case (column, index) =>
      header.getCell(index).setCellValue(column)
      header.getCell(index).setCellStyle(header.getRowStyle)
    }

    // Limit column
    val intStyle = workbook.createCellStyle
    intStyle.cloneStyleFrom(style)
    intStyle.setDataFormat(format.getFormat("0"))

    sets.flatMap(_.groups.asScala.sorted).zipWithIndex.foreach { case (group, index) =>
      val row = sheet.createRow(index + 1)
      row.getCell(0).setCellValue(group.groupSet.module.code.toUpperCase)
      row.getCell(1).setCellValue(group.groupSet.name)
      row.getCell(2).setCellValue(group.name)

      if (group.maxGroupSize != null) row.getCell(3).setCellValue(group.maxGroupSize.toInt)
      row.getCell(3).setCellStyle(intStyle)
    }

    // set style on all columns
    0 to ExtractedSmallGroup.AllColumns.size foreach { index =>
      sheet.setDefaultColumnStyle(index, style)
      sheet.autoSizeColumn(index)
    }
    sheet.setDefaultColumnStyle(3, intStyle)

    sheet
  }

  private def addEventsSheet(workbook: SXSSFWorkbook, sets: Seq[SmallGroupSet]): Sheet = {
    val style = workbook.createCellStyle
    val format = workbook.createDataFormat

    style.setDataFormat(format.getFormat("@"))
    style.setFont({
      val font = workbook.createFont()
      font.setFontHeightInPoints(11)
      font.setFontName("Calibri")
      font
    })

    val sheet = workbook.createSheet("Events")
    sheet.trackAllColumnsForAutoSizing()

    val header = sheet.createRow(0)

    header.setRowStyle({
      val bold = workbook.createCellStyle()
      bold.cloneStyleFrom(style)

      val font = workbook.createFont()
      font.setFontHeightInPoints(11)
      font.setFontName("Calibri")
      font.setBold(true)
      bold.setFont(font)

      bold
    })

    // set style on all columns
    ExtractedSmallGroupEvent.AllColumns.zipWithIndex foreach { case (column, index) =>
      header.getCell(index).setCellValue(column)
      header.getCell(index).setCellStyle(header.getRowStyle)
    }

    // Time columns
    val timeStyle = workbook.createCellStyle
    timeStyle.cloneStyleFrom(style)
    timeStyle.setDataFormat(format.getFormat("hh:mm"))

    sets.flatMap(_.groups.asScala.sorted).flatMap(_.events.sorted).zipWithIndex.foreach { case (event, index) =>
      val row = sheet.createRow(index + 1)
      row.getCell(0).setCellValue(event.group.groupSet.module.code.toUpperCase)
      row.getCell(1).setCellValue(event.group.groupSet.name)
      row.getCell(2).setCellValue(event.group.name)
      row.getCell(3).setCellValue(event.title)
      row.getCell(4).setCellValue(event.tutors.knownType.members.mkString(","))
      row.getCell(5).setCellValue(event.weekRanges.map(_.toString).mkString(","))
      if (event.day != null) row.getCell(6).setCellValue(event.day.name)

      if (event.startTime != null) row.getCell(7).setCellValue(DateUtil.convertTime(event.startTime.toString("HH:mm")))
      row.getCell(7).setCellStyle(timeStyle)

      if (event.endTime != null) row.getCell(8).setCellValue(DateUtil.convertTime(event.endTime.toString("HH:mm")))
      row.getCell(8).setCellStyle(timeStyle)

      event.location match {
        case AliasedMapLocation(alias: String, MapLocation(name: String, _, _)) =>
          row.getCell(9).setCellValue(name)
          row.getCell(10).setCellValue(alias)
        case location: Location =>
          row.getCell(9).setCellValue(location.name)
        case _ =>
      }

      row.getCell(11).setCellValue(event.relatedUrl)
      row.getCell(12).setCellValue(event.relatedUrlTitle)

      row.getCell(13).setCellValue(event.deliveryMethod.entryName)
      row.getCell(14).setCellValue(event.onlineDeliveryUrl)
      row.getCell(15).setCellValue(event.onlinePlatform.map(_.entryName).orNull)
    }

    if (sets.flatMap(_.groups.asScala.sorted).flatMap(_.events.sorted).nonEmpty) {
      // Day of week validation
      val dropdownRange = new CellRangeAddressList(1, sheet.getLastRowNum + 1000, 6, 6)
      val dvHelper = new XSSFDataValidationHelper(null)
      val dvConstraint =
        dvHelper.createFormulaListConstraint("Lookups!$C$2:$C$" + (DayOfWeek.members.size + 1))
      val validation = dvHelper.createValidation(dvConstraint, dropdownRange)
      validation.setShowErrorBox(true)
      sheet.addValidationData(validation)

      // Delivery method validation
      val deliveryMethodRange = new CellRangeAddressList(1, sheet.getLastRowNum + 1000, 13, 13)
      val deliveryMethodHelper = new XSSFDataValidationHelper(null)
      val deliveryMethodConstraint =
        deliveryMethodHelper.createFormulaListConstraint("Lookups!$D$2:$D$" + (EventDeliveryMethod.values.size + 1))
      val deliveryMethodValidation = deliveryMethodHelper.createValidation(deliveryMethodConstraint, deliveryMethodRange)
      deliveryMethodValidation.setShowErrorBox(true)
      sheet.addValidationData(deliveryMethodValidation)

      // Delivery method validation
      val onlinePlatformRange = new CellRangeAddressList(1, sheet.getLastRowNum + 1000, 15, 15)
      val onlinePlatformHelper = new XSSFDataValidationHelper(null)
      val onlinePlatformConstraint =
        onlinePlatformHelper.createFormulaListConstraint("Lookups!$E$2:$E$" + (OnlinePlatform.values.size + 1))
      val onlinePlatformValidation = onlinePlatformHelper.createValidation(onlinePlatformConstraint, onlinePlatformRange)
      onlinePlatformValidation.setShowErrorBox(true)
      sheet.addValidationData(onlinePlatformValidation)
    }

    // set style on all columns
    0 to ExtractedSmallGroupEvent.AllColumns.size foreach { index =>
      sheet.setDefaultColumnStyle(index, style)
      sheet.autoSizeColumn(index)
    }
    sheet.setDefaultColumnStyle(7, timeStyle)
    sheet.setDefaultColumnStyle(8, timeStyle)

    sheet
  }

}

trait SmallGroupSetsSpreadsheetTemplateState {
  def department: Department

  def academicYear: AcademicYear
}

trait SmallGroupSetsSpreadsheetTemplatePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: SmallGroupSetsSpreadsheetTemplateState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = p.PermissionCheck(RequiredPermission, mandatory(department))
}
