package uk.ac.warwick.tabula.commands.groups.admin

import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook}
import org.joda.time.{DateTime, LocalTime}
import uk.ac.warwick.tabula.data.model.{Department, MapLocation, Module, NamedLocation}
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.services.{SmallGroupService, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

import scala.collection.immutable.IndexedSeq

class SmallGroupSetsSpreadsheetTemplateCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends SmallGroupServiceComponent {
		val smallGroupService: SmallGroupService = smartMock[SmallGroupService]
	}

	private trait CommandFixture {
		val department: Department = Fixtures.department("in", "IT Services")
		val academicYear = AcademicYear(2015)

		val command = new SmallGroupSetsSpreadsheetTemplateCommandInternal(department, academicYear) with CommandTestSupport
	}

	@Test def emptySpreadsheet(): Unit = new CommandFixture {
		command.smallGroupService.getSmallGroupSets(department, academicYear) returns Nil

		val workbook: XSSFWorkbook = command.generateWorkbook()
		workbook.getSheet("Sets").getLastRowNum should be (0)
		workbook.getSheet("Groups").getLastRowNum should be (0)
		workbook.getSheet("Events").getLastRowNum should be (0)

		val lookups: XSSFSheet = workbook.getSheet("Lookups")

		val formats: IndexedSeq[String] = (1 to 9).map { i => lookups.getRow(i).getCell(0).toString }
		val allocationMethods: IndexedSeq[String] = (1 to 4).map { i => lookups.getRow(i).getCell(1).toString }
		val daysOfWeek: IndexedSeq[String] = (1 to 7).map { i => lookups.getRow(i).getCell(2).toString }

		formats should be (Seq("Seminar", "Lab", "Tutorial", "Project group", "Example Class", "Workshop", "Lecture", "Exam", "Meeting"))
		allocationMethods should be (Seq("Manual", "Self sign-up", "Linked", "Random"))
		daysOfWeek should be (Seq("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"))
	}

	@Test def itWorks(): Unit = new CommandFixture {
		val in101: Module = Fixtures.module("in101", "Introduction to Programming")

		val set: SmallGroupSet = Fixtures.smallGroupSet("IN101 Labs")
		set.module = in101
		set.format = SmallGroupFormat.Lab
		set.allocationMethod = SmallGroupAllocationMethod.StudentSignUp
		set.studentsCanSeeTutorName = true
		set.studentsCanSeeOtherMembers = false
		set.allowSelfGroupSwitching = true
		set.collectAttendance = true

		val group1: SmallGroup = Fixtures.smallGroup("Alpha")
		group1.groupSet = set
		set.groups.add(group1)

		val event1: SmallGroupEvent = Fixtures.smallGroupEvent("")
		event1.group = group1
		event1.tutors.knownType.addUserId("cuscav")
		event1.weekRanges = Seq(WeekRange(1, 6), WeekRange(8, 10))
		event1.day = DayOfWeek.Monday
		event1.startTime = new LocalTime(11, 0)
		event1.endTime = new LocalTime(12, 30)
		event1.location = MapLocation("S0.27", "123")
		group1.addEvent(event1)

		val event2: SmallGroupEvent = Fixtures.smallGroupEvent("Class test")
		event2.group = group1
		event2.tutors.knownType.addUserId("cusfal")
		event2.tutors.knownType.addUserId("curef")
		event2.weekRanges = Seq(WeekRange(7))
		event2.day = DayOfWeek.Tuesday
		event2.startTime = new LocalTime(9, 0)
		event2.endTime = new LocalTime(11, 0)
		event2.location = NamedLocation("Student break-out area")
		group1.addEvent(event2)

		val group2: SmallGroup = Fixtures.smallGroup("Beta")
		group2.groupSet = set
		group2.maxGroupSize = 10
		set.groups.add(group2)

		command.smallGroupService.getSmallGroupSets(department, academicYear) returns Seq(set)

		val workbook: XSSFWorkbook = command.generateWorkbook()

		val setsSheet: XSSFSheet = workbook.getSheet("Sets")
		setsSheet.getLastRowNum should be (1)

		val setRow: IndexedSeq[String] = (0 to 8).map { col => setsSheet.getRow(1).getCell(col).toString }
		setRow should be (Seq("IN101", "Lab", "IN101 Labs", "Self sign-up", "TRUE", "FALSE", "TRUE", "", "TRUE"))

		val groupsSheet: XSSFSheet = workbook.getSheet("Groups")
		groupsSheet.getLastRowNum should be (2)

		val groupRow1: IndexedSeq[String] = (0 to 3).map { col => groupsSheet.getRow(1).getCell(col).toString }
		val groupRow2: IndexedSeq[String] = (0 to 3).map { col => groupsSheet.getRow(2).getCell(col).toString }

		groupRow1 should be (Seq("IN101", "IN101 Labs", "Alpha", ""))
		groupRow2 should be (Seq("IN101", "IN101 Labs", "Beta", "10.0"))

		val eventsSheet: XSSFSheet = workbook.getSheet("Events")
		eventsSheet.getLastRowNum should be (2)

		val eventRow1: IndexedSeq[String] = (0 to 9).map { col =>
			if (col == 7 || col == 8) {
				new DateTime(eventsSheet.getRow(1).getCell(col).getDateCellValue).toLocalTime.toString("HH:mm")
			} else {
				eventsSheet.getRow(1).getCell(col).toString
			}
		}
		val eventRow2: IndexedSeq[String] = (0 to 9).map { col =>
			if (col == 7 || col == 8) {
				new DateTime(eventsSheet.getRow(2).getCell(col).getDateCellValue).toLocalTime.toString("HH:mm")
			} else {
				eventsSheet.getRow(2).getCell(col).toString
			}
		}

		eventRow1 should be (Seq("IN101", "IN101 Labs", "Alpha", "", "cuscav", "1-6,8-10", "Monday", "11:00", "12:30", "S0.27"))
		eventRow2 should be (Seq("IN101", "IN101 Labs", "Alpha", "Class test", "cusfal,curef", "7", "Tuesday", "09:00", "11:00", "Student break-out area"))
	}

}
