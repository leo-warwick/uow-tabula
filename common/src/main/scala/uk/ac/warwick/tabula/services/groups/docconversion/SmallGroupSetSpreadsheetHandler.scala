package uk.ac.warwick.tabula.services.groups.docconversion

import java.io.InputStream

import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler
import org.apache.poi.xssf.eventusermodel.{ReadOnlySharedStringsTable, XSSFReader, XSSFSheetXMLHandler}
import org.apache.poi.xssf.usermodel.XSSFComment
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, LocalTime}
import org.springframework.stereotype.Service
import org.springframework.validation.BindingResult
import org.xml.sax.InputSource
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod.Linked
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.XmlUtils
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.groups.docconversion.SmallGroupSetSpreadsheetContentsHandler._
import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.{AcademicYear, UniversityId}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._
import scala.collection.mutable
import scala.util.{Try, Using}

case class ExtractedSmallGroupSet(
  module: Module,
  format: SmallGroupFormat,
  name: String,
  allocationMethod: SmallGroupAllocationMethod,
  studentsSeeTutor: Boolean,
  studentsSeeStudents: Boolean,
  studentsCanSwitchGroup: Boolean,
  linkedSmallGroupSet: Option[DepartmentSmallGroupSet],
  collectAttendance: Boolean,
  groups: Seq[ExtractedSmallGroup]
)

object ExtractedSmallGroupSet {
  val SheetName = "Sets"

  val ModuleColumn = "Module"
  val FormatColumn = "Type"
  val NameColumn = "Name"
  val AllocationMethodColumn = "Allocation method"
  val StudentsSeeTutorColumn = "Students see tutor"
  val StudentsSeeStudentsColumn = "Students see other students"
  val StudentsCanSwitchGroupColumn = "Students can switch groups"
  val LinkedGroupNameColumn = "Linked group name"
  val CollectAttendanceColumn = "Collect attendance"

  val AllColumns = Seq(ModuleColumn, FormatColumn, NameColumn, AllocationMethodColumn, StudentsSeeTutorColumn, StudentsSeeStudentsColumn, StudentsCanSwitchGroupColumn, LinkedGroupNameColumn, CollectAttendanceColumn)
  val RequiredColumns = Seq(ModuleColumn, FormatColumn, NameColumn, AllocationMethodColumn, StudentsSeeTutorColumn, StudentsSeeStudentsColumn, StudentsCanSwitchGroupColumn, CollectAttendanceColumn)
}

case class ExtractedSmallGroup(
  name: String,
  limit: Option[Int],
  events: Seq[ExtractedSmallGroupEvent]
)

object ExtractedSmallGroup {
  val SheetName = "Groups"

  val ModuleColumn = "Module"
  val SetNameColumn = "Set name"
  val GroupNameColumn = "Group name"
  val LimitColumn = "Limit"

  val AllColumns = Seq(ModuleColumn, SetNameColumn, GroupNameColumn, LimitColumn)
  val RequiredColumns = Seq(ModuleColumn, SetNameColumn, GroupNameColumn)
}

case class ExtractedSmallGroupEvent(
  title: Option[String],
  tutors: Seq[User],
  weekRanges: Seq[WeekRange],
  dayOfWeek: Option[DayOfWeek],
  startTime: Option[LocalTime],
  endTime: Option[LocalTime],
  location: Option[Location],
  possibleMapLocations: Seq[WAI2GoLocation],
  relatedUrl: Option[String],
  relatedUrlTitle: Option[String]
)

object ExtractedSmallGroupEvent {
  val SheetName = "Events"

  val ModuleColumn = "Module"
  val SetNameColumn = "Set name"
  val GroupNameColumn = "Group name"
  val TitleColumn = "Title"
  val TutorsColumn = "Tutors"
  val WeekRangesColumn = "Week ranges"
  val DayColumn = "Day"
  val StartTimeColumn = "Start time"
  val EndTimeColumn = "End time"
  val LocationColumn = "Location"
  val LocationDisplayNameColumn = "Location display name"
  val RelatedUrlColumn = "Link"
  val RelatedUrlTitleColumn = "Link text"

  val AllColumns = Seq(ModuleColumn, SetNameColumn, GroupNameColumn, TitleColumn, TutorsColumn, WeekRangesColumn, DayColumn,
    StartTimeColumn, EndTimeColumn, LocationColumn, LocationDisplayNameColumn, RelatedUrlColumn, RelatedUrlTitleColumn)
  val RequiredColumns = Seq(ModuleColumn, SetNameColumn, GroupNameColumn)
}

object SmallGroupSetSpreadsheetHandler {
  val AcceptedFileExtensions = Seq(".xlsx")
}

trait SmallGroupSetSpreadsheetHandler {
  def readXSSFExcelFile(department: Department, academicYear: AcademicYear, file: InputStream, result: BindingResult): Seq[ExtractedSmallGroupSet]
}

object SmallGroupSetSpreadsheetContentsHandler {
  type Sheet = Seq[Row]

  case class Row(rowNumber: Int, values: Map[String, Cell])

  case class Cell(cellReference: String, formattedValue: String)

}

abstract class SmallGroupSetSpreadsheetContentsHandler extends SheetContentsHandler with SyllabusPlusLocationServiceComponent {

  var parsedSheets: mutable.Map[String, Sheet] = mutable.Map.empty
  var parsedRows: mutable.ListBuffer[Row] = _
  var currentItem: mutable.Map[String, Cell] = _
  var columns: mutable.Map[Short, String] = _

  def startSheet(name: String): Unit = {
    parsedRows = mutable.ListBuffer.empty
    columns = mutable.Map.empty
  }

  def endSheet(name: String): Unit = {
    parsedSheets(name) = parsedRows.toList // Not strictly necessary but enforces immutability
  }

  override def headerFooter(text: String, isHeader: Boolean, tagName: String): Unit = {}

  override def startRow(rowNum: Int): Unit = {
    currentItem = mutable.Map.empty
  }

  override def endRow(rowNum: Int): Unit = {
    if (rowNum > 0 && currentItem.nonEmpty) parsedRows += Row(rowNum + 1, currentItem.toMap)
  }

  override def cell(cellReference: String, formattedValue: String, comment: XSSFComment): Unit = {
    val cell = new CellReference(cellReference)
    val col = new CellReference(cellReference).getCol

    if (cell.getRow == 0) {
      // Header
      columns(col) = formattedValue
    } else if (columns.contains(col)) { // We ignore anything outside of the header columns
      currentItem(columns(col)) = Cell(cellReference, formattedValue)
    }
  }
}

abstract class SmallGroupSetSpreadsheetHandlerImpl extends SmallGroupSetSpreadsheetHandler {
  self: ModuleAndDepartmentServiceComponent
    with SmallGroupServiceComponent
    with UserLookupComponent
    with LocationFetchingServiceComponent
    with SyllabusPlusLocationServiceComponent =>

  private def readSpreadsheet(file: InputStream, result: BindingResult): Map[String, Seq[Row]] = Using.resource(file) { stream =>
    val pkg = OPCPackage.open(stream)
    val sst = new ReadOnlySharedStringsTable(pkg)
    val reader = new XSSFReader(pkg)
    val styles = reader.getStylesTable

    val locationService = syllabusPlusLocationService
    val handler = new SmallGroupSetSpreadsheetContentsHandler {
      override def syllabusPlusLocationService: SyllabusPlusLocationService = locationService
    }
    val parser = {
      val p = XmlUtils.getXmlReader
      p.setContentHandler(new XSSFSheetXMLHandler(styles, sst, handler, false))
      p
    }

    val sheets = reader.getSheetsData.asInstanceOf[XSSFReader.SheetIterator]
    while (sheets.hasNext) {
      Using.resource(sheets.next()) { sheet =>
        val currentSheetName = sheets.getSheetName
        handler.startSheet(currentSheetName)
        parser.parse(new InputSource(sheet))
        handler.endSheet(currentSheetName)
      }
    }

    handler.parsedSheets.toMap
  }

  override def readXSSFExcelFile(department: Department, academicYear: AcademicYear, file: InputStream, result: BindingResult): Seq[ExtractedSmallGroupSet] = {
    // Read the spreadsheet into a Map of sheet names to data rows
    val parsed = readSpreadsheet(file, result)

    // Validate that there are correct sheets passed and that the correct columns have been passed
    def validateSheet(sheetName: String, requiredColumns: Seq[String]): Unit = {
      if (!parsed.contains(sheetName)) {
        result.reject("smallGroups.importSpreadsheet.missingSheet", Array(sheetName), s"Could not find the $sheetName sheet")
      } else {
        parsed(sheetName).foreach { row =>
          requiredColumns.foreach { colName =>
            if (!row.values.contains(colName)) {
              result.reject(
                "smallGroups.importSpreadsheet.missingColumn",
                Array(sheetName, row.rowNumber: java.lang.Integer, colName),
                s"Sheet $sheetName row ${row.rowNumber} doesn't contain a value for '$colName'"
              )
            }
          }
        }
      }
    }

    validateSheet("Sets", ExtractedSmallGroupSet.RequiredColumns)
    validateSheet("Groups", ExtractedSmallGroup.RequiredColumns)
    validateSheet("Events", ExtractedSmallGroupEvent.RequiredColumns)

    if (!result.hasErrors) {
      // Now the fun starts
      val setRows = parsed("Sets")
      val (groupRows, orphanedGroupRows) = parsed("Groups").partition { groupRow =>
        setRows.exists { setRow =>
          setRow.values(ExtractedSmallGroupSet.ModuleColumn).formattedValue == groupRow.values(ExtractedSmallGroup.ModuleColumn).formattedValue &&
            setRow.values(ExtractedSmallGroupSet.NameColumn).formattedValue == groupRow.values(ExtractedSmallGroup.SetNameColumn).formattedValue
        }
      }

      orphanedGroupRows.foreach { row =>
        val moduleCode = row.values(ExtractedSmallGroup.ModuleColumn).formattedValue
        val setName = row.values(ExtractedSmallGroup.SetNameColumn).formattedValue

        result.reject(
          "smallGroups.importSpreadsheet.orphanedGroup",
          Array("Groups", row.rowNumber: java.lang.Integer, moduleCode, setName, "Sets"),
          s"Sheet Groups row ${row.rowNumber}: couldn't find small group set $setName ($moduleCode) in Sets sheet"
        )
      }

      val (eventRows, orphanedEventRows) = parsed("Events").partition { eventRow =>
        groupRows.exists { groupRow =>
          groupRow.values(ExtractedSmallGroup.ModuleColumn).formattedValue == eventRow.values(ExtractedSmallGroupEvent.ModuleColumn).formattedValue &&
            groupRow.values(ExtractedSmallGroup.SetNameColumn).formattedValue == eventRow.values(ExtractedSmallGroupEvent.SetNameColumn).formattedValue &&
            groupRow.values(ExtractedSmallGroup.GroupNameColumn).formattedValue == eventRow.values(ExtractedSmallGroupEvent.GroupNameColumn).formattedValue
        }
      }

      orphanedEventRows.foreach { row =>
        val moduleCode = row.values(ExtractedSmallGroupEvent.ModuleColumn).formattedValue
        val setName = row.values(ExtractedSmallGroupEvent.SetNameColumn).formattedValue
        val groupName = row.values(ExtractedSmallGroupEvent.GroupNameColumn).formattedValue

        result.reject(
          "smallGroups.importSpreadsheet.orphanedEvent",
          Array("Events", row.rowNumber: java.lang.Integer, moduleCode, setName, groupName, "Groups"),
          s"Sheet Events row ${row.rowNumber}: couldn't find small group $groupName for $setName ($moduleCode) in the Groups sheet"
        )
      }

      val sets = buildEmptySets(department, academicYear, "Sets", setRows, result).map { set =>
        val linkedGroups = set.linkedSmallGroupSet.toSeq.flatMap(_.groups.asScala)
        set.copy(
          groups = buildEmptyGroups(set, "Groups", groupRows, result).map { group =>
            // reject linked groups when the groups on the spreadsheet don't have the same names
            if (set.allocationMethod == Linked && !linkedGroups.exists(_.name == group.name)) {
              result.reject(
                "smallGroups.importSpreadsheet.invalidGroupName",
                Array(group.name, set.linkedSmallGroupSet.map(_.name).getOrElse("")),
                s"No group called ${group.name} was found in the reusable small group ${set.linkedSmallGroupSet.map(_.name).getOrElse("")}"
              )
            }
            group.copy(
              events = buildEvents(set, group, "Events", eventRows, result)
            )
          }
        )
      }
      sets
    } else Nil
  }

  private def extractModule(sheetName: String, cell: Cell, result: BindingResult): Option[Module] = {
    val module = moduleAndDepartmentService.getModuleByCode(cell.formattedValue)
    if (module.isEmpty) {
      result.reject(
        "smallGroups.importSpreadsheet.invalidModule",
        Array(sheetName, cell.cellReference, cell.formattedValue),
        s"Sheet $sheetName cell ${cell.cellReference} - invalid module code ${cell.formattedValue}"
      )
    }

    module
  }

  private def extractFormat(sheetName: String, cell: Cell, result: BindingResult): Option[SmallGroupFormat] = {
    val format = SmallGroupFormat.members.find { f => f.code.equalsIgnoreCase(cell.formattedValue) || f.description.equalsIgnoreCase(cell.formattedValue) }
    if (format.isEmpty) {
      result.reject(
        "smallGroups.importSpreadsheet.invalidFormat",
        Array(sheetName, cell.cellReference, cell.formattedValue),
        s"Sheet $sheetName cell ${cell.cellReference} - invalid small group type ${cell.formattedValue}"
      )
    }

    format
  }

  private def extractAllocationMethod(sheetName: String, cell: Cell, result: BindingResult): Option[SmallGroupAllocationMethod] = {
    val allocationMethod = SmallGroupAllocationMethod.members.find { a => a.dbValue.equalsIgnoreCase(cell.formattedValue) || a.description.equalsIgnoreCase(cell.formattedValue) }
    if (allocationMethod.isEmpty) {
      result.reject(
        "smallGroups.importSpreadsheet.invalidAllocationMethod",
        Array(sheetName, cell.cellReference, cell.formattedValue),
        s"Sheet $sheetName cell ${cell.cellReference} - invalid small group allocation method ${cell.formattedValue}"
      )
    }

    allocationMethod
  }

  private def extractInt(sheetName: String, cell: Cell, result: BindingResult): Option[Int] =
    Try(cell.formattedValue.trim.toInt)
      .map(Some.apply)
      .getOrElse {
        result.reject(
          "smallGroups.importSpreadsheet.invalidInt",
          Array(sheetName, cell.cellReference, cell.formattedValue),
          s"Sheet $sheetName cell ${cell.cellReference} - invalid integer ${cell.formattedValue}"
        )

        None
      }

  private def extractBoolean(sheetName: String, cell: Cell, result: BindingResult): Option[Boolean] =
    cell.formattedValue.toLowerCase.trim match {
      case "1" | "1.0" | "true" | "yes" | "on" => Some(true)
      case "0" | "0.0" | "false" | "no" | "off" => Some(false)
      case v =>
        result.reject(
          "smallGroups.importSpreadsheet.invalidBoolean",
          Array(sheetName, cell.cellReference, cell.formattedValue),
          s"Sheet $sheetName cell ${cell.cellReference} - invalid small group allocation method ${cell.formattedValue}"
        )

        None
    }

  private def extractDepartmentSmallGroupSet(department: Department, academicYear: AcademicYear, sheetName: String, cell: Cell, result: BindingResult): Option[DepartmentSmallGroupSet] = {
    val linkedSmallGroupSet = smallGroupService.getDepartmentSmallGroupSets(department, academicYear).find(_.name.equalsIgnoreCase(cell.formattedValue))
    if (linkedSmallGroupSet.isEmpty) {
      result.reject(
        "smallGroups.importSpreadsheet.invalidDepartmentSmallGroupSet",
        Array(sheetName, cell.cellReference, cell.formattedValue, academicYear.toString, department.name),
        s"Sheet $sheetName cell ${cell.cellReference} - invalid linked small group set ${cell.formattedValue} for ${academicYear.toString} in ${department.name}"
      )
    }

    linkedSmallGroupSet
  }

  private def extractUsers(sheetName: String, cell: Cell, result: BindingResult): Seq[User] =
    cell.formattedValue.trim.split("\\s*,\\s*").filter(_.hasText).flatMap {
      case universityId if UniversityId.isValid(universityId) =>
        val user = userLookup.getUserByWarwickUniId(universityId)

        if (!user.isFoundUser) {
          result.reject(
            "smallGroups.importSpreadsheet.invalidUniversityId",
            Array(sheetName, cell.cellReference, universityId),
            s"Sheet $sheetName cell ${cell.cellReference} - couldn't find a user for university ID $universityId"
          )

          Nil
        } else Seq(user)
      case userId =>
        val user = userLookup.getUserByUserId(userId)

        if (!user.isFoundUser) {
          result.reject(
            "smallGroups.importSpreadsheet.invalidUserId",
            Array(sheetName, cell.cellReference, userId),
            s"Sheet $sheetName cell ${cell.cellReference} - couldn't find a user for usercode $userId"
          )

          Nil
        } else Seq(user)
    }

  private def extractWeekRanges(sheetName: String, cell: Cell, result: BindingResult): Seq[WeekRange] =
    cell.formattedValue.trim.split("\\s*,\\s*").filter(_.hasText).flatMap { range =>
      Try(WeekRange.fromString(range))
        .map(range => Seq(range))
        .getOrElse {
          result.reject(
            "smallGroups.importSpreadsheet.invalidWeekRange",
            Array(sheetName, cell.cellReference, range),
            s"Sheet $sheetName cell ${cell.cellReference} - invalid week range $range"
          )

          Nil
        }
    }


  private def extractDayOfWeek(sheetName: String, cell: Cell, result: BindingResult): Option[DayOfWeek] = {
    val dayOfWeek = DayOfWeek.members.find { day =>
      day.name.equalsIgnoreCase(cell.formattedValue) ||
        (cell.formattedValue.length > 2 && day.name.toLowerCase.startsWith(cell.formattedValue.toLowerCase)) ||
        day.jodaDayOfWeek.toString == cell.formattedValue
    }

    if (dayOfWeek.isEmpty) {
      result.reject(
        "smallGroups.importSpreadsheet.invalidDayOfWeek",
        Array(sheetName, cell.cellReference, cell.formattedValue),
        s"Sheet $sheetName cell ${cell.cellReference} - invalid day ${cell.formattedValue}"
      )
    }

    dayOfWeek
  }

  private def extractLocalTime(sheetName: String, cell: Cell, result: BindingResult): Option[LocalTime] = {
    val value = cell.formattedValue.trim

    Try(LocalTime.parse(value, DateTimeFormat.forPattern("HH:mm:ss")))
      .orElse(Try(LocalTime.parse(value, DateTimeFormat.forPattern("HH:mm"))))
      .orElse(Try(LocalTime.parse(value, DateTimeFormat.forPattern("h:mm:ssa"))))
      .orElse(Try(LocalTime.parse(value, DateTimeFormat.forPattern("h:mma"))))
      .orElse(Try(new DateTime(DateUtil.getJavaDate(value.toDouble)).toLocalTime))
      .map(Some.apply)
      .getOrElse {
        result.reject(
          "smallGroups.importSpreadsheet.invalidLocalTime",
          Array(sheetName, cell.cellReference, cell.formattedValue),
          s"Sheet $sheetName cell ${cell.cellReference} - invalid time ${cell.formattedValue}"
        )

        None
      }
  }

  private def buildEmptySets(department: Department, academicYear: AcademicYear, sheetName: String, rows: Seq[Row], result: BindingResult): Seq[ExtractedSmallGroupSet] = rows.flatMap { row =>
    val module = extractModule(sheetName, row.values(ExtractedSmallGroupSet.ModuleColumn), result)
    val format = extractFormat(sheetName, row.values(ExtractedSmallGroupSet.FormatColumn), result)
    val name = row.values(ExtractedSmallGroupSet.NameColumn).formattedValue
    val allocationMethod = extractAllocationMethod(sheetName, row.values(ExtractedSmallGroupSet.AllocationMethodColumn), result)
    val studentsSeeTutor = extractBoolean(sheetName, row.values(ExtractedSmallGroupSet.StudentsSeeTutorColumn), result)
    val studentsSeeStudents = extractBoolean(sheetName, row.values(ExtractedSmallGroupSet.StudentsSeeStudentsColumn), result)
    val studentsCanSwitchGroup = extractBoolean(sheetName, row.values(ExtractedSmallGroupSet.StudentsCanSwitchGroupColumn), result)
    val linkedSmallGroupSet =
      if (allocationMethod.contains(SmallGroupAllocationMethod.Linked) && row.values.contains(ExtractedSmallGroupSet.LinkedGroupNameColumn))
        extractDepartmentSmallGroupSet(department, academicYear, sheetName, row.values(ExtractedSmallGroupSet.LinkedGroupNameColumn), result)
      else
        None
    val collectAttendance = extractBoolean(sheetName, row.values(ExtractedSmallGroupSet.CollectAttendanceColumn), result)

    if (module.nonEmpty && format.nonEmpty && allocationMethod.nonEmpty && studentsSeeTutor.nonEmpty && studentsSeeStudents.nonEmpty && studentsCanSwitchGroup.nonEmpty && collectAttendance.nonEmpty) {
      Seq(ExtractedSmallGroupSet(
        module.get,
        format.get,
        name,
        allocationMethod.get,
        studentsSeeTutor.get,
        studentsSeeStudents.get,
        studentsCanSwitchGroup.get,
        linkedSmallGroupSet,
        collectAttendance.get,
        Nil
      ))
    } else {
      Nil
    }
  }

  private def buildEmptyGroups(set: ExtractedSmallGroupSet, sheetName: String, rows: Seq[Row], result: BindingResult): Seq[ExtractedSmallGroup] =
    rows.filter { row =>
      val moduleCode = row.values(ExtractedSmallGroup.ModuleColumn).formattedValue.trim
      val setName = row.values(ExtractedSmallGroup.SetNameColumn).formattedValue.trim

      set.module.code.equalsIgnoreCase(moduleCode) && set.name.equalsIgnoreCase(setName)
    }.map { row =>
      val name = row.values(ExtractedSmallGroup.GroupNameColumn).formattedValue
      val limit =
        if (row.values.contains(ExtractedSmallGroup.LimitColumn) && row.values(ExtractedSmallGroup.LimitColumn).formattedValue.hasText)
          extractInt(sheetName, row.values(ExtractedSmallGroup.LimitColumn), result)
        else
          None

      ExtractedSmallGroup(
        name,
        limit,
        Nil
      )
    }

  private def buildEvents(set: ExtractedSmallGroupSet, group: ExtractedSmallGroup, sheetName: String, rows: Seq[Row], result: BindingResult): Seq[ExtractedSmallGroupEvent] =
    rows.filter { row =>
      val moduleCode = row.values(ExtractedSmallGroupEvent.ModuleColumn).formattedValue.trim
      val setName = row.values(ExtractedSmallGroupEvent.SetNameColumn).formattedValue.trim
      val groupName = row.values(ExtractedSmallGroupEvent.GroupNameColumn).formattedValue.trim

      set.module.code.equalsIgnoreCase(moduleCode) && set.name.equalsIgnoreCase(setName) && group.name.equalsIgnoreCase(groupName)
    }.flatMap { row =>
      val title = row.values.get(ExtractedSmallGroupEvent.TitleColumn).flatMap(_.formattedValue.maybeText)
      val tutors =
        if (row.values.contains(ExtractedSmallGroupEvent.TutorsColumn))
          extractUsers(sheetName, row.values(ExtractedSmallGroupEvent.TutorsColumn), result)
        else
          Nil

      val weekRanges =
        if (row.values.contains(ExtractedSmallGroupEvent.WeekRangesColumn))
          extractWeekRanges(sheetName, row.values(ExtractedSmallGroupEvent.WeekRangesColumn), result)
        else Nil
      val dayOfWeek =
        if (row.values.contains(ExtractedSmallGroupEvent.DayColumn))
          extractDayOfWeek(sheetName, row.values(ExtractedSmallGroupEvent.DayColumn), result)
        else None
      val startTime =
        if (row.values.contains(ExtractedSmallGroupEvent.StartTimeColumn))
          extractLocalTime(sheetName, row.values(ExtractedSmallGroupEvent.StartTimeColumn), result)
        else None
      val endTime =
        if (row.values.contains(ExtractedSmallGroupEvent.EndTimeColumn))
          extractLocalTime(sheetName, row.values(ExtractedSmallGroupEvent.EndTimeColumn), result)
        else None

      val location = row.values.get(ExtractedSmallGroupEvent.LocationColumn)
        .map(_.formattedValue)
        .flatMap { name =>

          syllabusPlusLocationService.getByUpstreamName(name).map(_.asMapLocation)
            .orElse(Some(locationFetchingService.locationFor(name)))
        }
        .map {
          case loc: MapLocation =>
            row.values.get(ExtractedSmallGroupEvent.LocationDisplayNameColumn)
              .map(_.formattedValue)
              .map(alias => AliasedMapLocation(alias, loc))
              .getOrElse(loc)
          case x => x
        }

      val possibleMapLocations: Seq[WAI2GoLocation] = row.values.get(ExtractedSmallGroupEvent.LocationColumn)
        .map(_.formattedValue)
        .toSeq
        .flatMap { name =>
          locationFetchingService.mapLocationsFor(name).getOrElse(Nil)
        }

      val relatedUrl = row.values.get(ExtractedSmallGroupEvent.RelatedUrlColumn).flatMap(_.formattedValue.maybeText)
      val relatedUrlTitle = row.values.get(ExtractedSmallGroupEvent.RelatedUrlTitleColumn).flatMap(_.formattedValue.maybeText)

      if (title.nonEmpty || tutors.nonEmpty || weekRanges.nonEmpty || dayOfWeek.nonEmpty || startTime.nonEmpty || endTime.nonEmpty || location.nonEmpty) {
        Seq(ExtractedSmallGroupEvent(
          title,
          tutors,
          weekRanges,
          dayOfWeek,
          startTime,
          endTime,
          location,
          possibleMapLocations,
          relatedUrl,
          relatedUrlTitle
        ))
      } else {
        Nil
      }
    }
}

@Service
class AutowiringSmallGroupSetSpreadsheetHandler
  extends SmallGroupSetSpreadsheetHandlerImpl
    with AutowiringModuleAndDepartmentServiceComponent
    with AutowiringSmallGroupServiceComponent
    with AutowiringUserLookupComponent
    with AutowiringWAI2GoConfigurationComponent
    with WAI2GoHttpLocationFetchingServiceComponent
    with AutowiringSyllabusPlusLocationServiceComponent

trait SmallGroupSetSpreadsheetHandlerComponent {
  def smallGroupSetSpreadsheetHandler: SmallGroupSetSpreadsheetHandler
}

trait AutowiringSmallGroupSetSpreadsheetHandlerComponent extends SmallGroupSetSpreadsheetHandlerComponent {
  val smallGroupSetSpreadsheetHandler: SmallGroupSetSpreadsheetHandler = Wire[SmallGroupSetSpreadsheetHandler]
}
