package uk.ac.warwick.tabula.services.groups.docconversion

import java.io.InputStream

import org.joda.time.LocalTime
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables.{LocationFetchingService, LocationFetchingServiceComponent}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.data.model.{Department, Location, Module, NamedLocation}
import uk.ac.warwick.userlookup.User

class SmallGroupSetSpreadsheetHandlerTest extends TestBase with Mockito {

	private trait Fixture {
		val moduleAndDepartmentService: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]
		val smallGroupService: SmallGroupService = smartMock[SmallGroupService]
		val userLookup = new MockUserLookup

		val handler = new SmallGroupSetSpreadsheetHandlerImpl with ModuleAndDepartmentServiceComponent with SmallGroupServiceComponent with UserLookupComponent with LocationFetchingServiceComponent {
			val moduleAndDepartmentService: ModuleAndDepartmentService = Fixture.this.moduleAndDepartmentService
			val smallGroupService: SmallGroupService = Fixture.this.smallGroupService
			val userLookup: MockUserLookup = Fixture.this.userLookup
			val locationFetchingService = new LocationFetchingService {
				def locationFor(name: String): Location = NamedLocation(name)
			}
		}

		val department: Department = Fixtures.department("in", "IT Services")
		val academicYear = AcademicYear(2015)

		val ch134: Module = Fixtures.module("ch134", "Introduction to Chemistry Stuff")
		moduleAndDepartmentService.getModuleByCode("CH134") returns Some(ch134)

		val linkedUgY1: DepartmentSmallGroupSet = Fixtures.departmentSmallGroupSet("UG Y1")
		linkedUgY1.groups.add(Fixtures.departmentSmallGroup("Alpha"))
		linkedUgY1.groups.add(Fixtures.departmentSmallGroup("Beta"))
		linkedUgY1.groups.add(Fixtures.departmentSmallGroup("Gamma"))
		linkedUgY1.groups.add(Fixtures.departmentSmallGroup("Delta"))
		linkedUgY1.groups.add(Fixtures.departmentSmallGroup("Epsilon"))
		linkedUgY1.groups.add(Fixtures.departmentSmallGroup("Zeta"))
		smallGroupService.getDepartmentSmallGroupSets(department, academicYear) returns Seq(linkedUgY1)

		userLookup.registerUsers("u0000001", "u0000002", "cuscav", "curef", "u1234567", "u2382344", "u1823774", "u2372372", "u1915121", "u1784383")
		val u0000001: User = userLookup.getUserByUserId("u0000001")
		val u0000002: User = userLookup.getUserByUserId("u0000002")
		val cuscav: User = userLookup.getUserByUserId("cuscav")
		val curef: User = userLookup.getUserByUserId("curef")
		val u1234567: User = userLookup.getUserByUserId("u1234567")
		val u2382344: User = userLookup.getUserByUserId("u2382344")
		val u1823774: User = userLookup.getUserByUserId("u1823774")
		val u2372372: User = userLookup.getUserByUserId("u2372372")
		val u1915121: User = userLookup.getUserByUserId("u1915121")
		val u1784383: User = userLookup.getUserByUserId("u1784383")
	}

	@Test def itWorks(): Unit = new Fixture {
		val is: InputStream = resourceAsStream("/sgt-import.xlsx")
		val bindingResult = new BindException(new Object, "command")

		val results: Seq[ExtractedSmallGroupSet] = handler.readXSSFExcelFile(department, academicYear, is, bindingResult)
		results should be (Seq(
			ExtractedSmallGroupSet(
				module = ch134,
				format = SmallGroupFormat.Seminar,
				name = "CH134 Seminars",
				allocationMethod = SmallGroupAllocationMethod.Linked,
				studentsSeeTutor = true,
				studentsSeeStudents = false,
				studentsCanSwitchGroup = false,
				linkedSmallGroupSet = Some(linkedUgY1),
				collectAttendance = true,
				groups = Seq(
					ExtractedSmallGroup(
						name = "Alpha",
						limit = None,
						events = Seq(
							ExtractedSmallGroupEvent(None, Seq(u0000001, u0000002), Seq(WeekRange(1, 6), WeekRange(8, 10)), Some(DayOfWeek.Monday), Some(new LocalTime(11, 0)), Some(new LocalTime(12, 0)), Some(NamedLocation("S0.27"))),
							ExtractedSmallGroupEvent(Some("Class Test"), Seq(cuscav), Seq(WeekRange(7)), Some(DayOfWeek.Monday), Some(new LocalTime(11, 0)), Some(new LocalTime(12, 0)), Some(NamedLocation("S0.27")))
						)
					),
					ExtractedSmallGroup(
						name = "Beta",
						limit = None,
						events = Seq(
							ExtractedSmallGroupEvent(None, Seq(curef), Seq(WeekRange(1, 6), WeekRange(8, 10)), Some(DayOfWeek.Monday), Some(new LocalTime(11, 0)), Some(new LocalTime(12, 0)), Some(NamedLocation("S0.28"))),
							ExtractedSmallGroupEvent(Some("Class Test"), Seq(curef), Seq(WeekRange(7)), Some(DayOfWeek.Monday), Some(new LocalTime(11, 0)), Some(new LocalTime(12, 0)), Some(NamedLocation("S0.28")))
						)
					),
					ExtractedSmallGroup(
						name = "Gamma",
						limit = None,
						events = Seq(
							ExtractedSmallGroupEvent(None, Seq(u1234567), Seq(WeekRange(1, 6), WeekRange(8, 10)), Some(DayOfWeek.Monday), Some(new LocalTime(11, 0)), Some(new LocalTime(12, 0)), Some(NamedLocation("S0.29"))),
							ExtractedSmallGroupEvent(Some("Class Test"), Seq(u2382344), Seq(WeekRange(7)), Some(DayOfWeek.Monday), Some(new LocalTime(11, 0)), Some(new LocalTime(12, 0)), Some(NamedLocation("S0.29")))
						)
					),
					ExtractedSmallGroup(
						name = "Delta",
						limit = None,
						events = Seq(
							ExtractedSmallGroupEvent(None, Seq(u1823774, u2372372, u1915121), Seq(WeekRange(1, 6), WeekRange(8, 10)), Some(DayOfWeek.Monday), Some(new LocalTime(11, 0)), Some(new LocalTime(12, 0)), Some(NamedLocation("S0.30"))),
							ExtractedSmallGroupEvent(Some("Class Test"), Seq(u1784383), Seq(WeekRange(7)), Some(DayOfWeek.Monday), Some(new LocalTime(11, 0)), Some(new LocalTime(12, 0)), Some(NamedLocation("S0.30")))
						)
					),
					ExtractedSmallGroup(
						name = "Epsilon",
						limit = None,
						events = Seq(
							ExtractedSmallGroupEvent(None, Nil, Seq(WeekRange(1, 6), WeekRange(8, 10)), Some(DayOfWeek.Monday), Some(new LocalTime(11, 0)), Some(new LocalTime(12, 0)), Some(NamedLocation("S0.31"))),
							ExtractedSmallGroupEvent(Some("Class Test"), Nil, Seq(WeekRange(7)), Some(DayOfWeek.Monday), Some(new LocalTime(11, 0)), Some(new LocalTime(12, 0)), Some(NamedLocation("S0.31")))
						)
					),
					ExtractedSmallGroup(
						name = "Zeta",
						limit = None,
						events = Seq(
							ExtractedSmallGroupEvent(None, Nil, Seq(WeekRange(1, 6), WeekRange(8, 10)), Some(DayOfWeek.Monday), Some(new LocalTime(11, 0)), Some(new LocalTime(12, 0)), Some(NamedLocation("B192"))),
							ExtractedSmallGroupEvent(Some("Class Test"), Nil, Seq(WeekRange(7)), Some(DayOfWeek.Monday), Some(new LocalTime(11, 0)), Some(new LocalTime(12, 0)), Some(NamedLocation("B192")))
						)
					)
				)
			),

			ExtractedSmallGroupSet(
				module = ch134,
				format = SmallGroupFormat.Lab,
				name = "CH134 Labs",
				allocationMethod = SmallGroupAllocationMethod.Manual,
				studentsSeeTutor = true,
				studentsSeeStudents = true,
				studentsCanSwitchGroup = false,
				linkedSmallGroupSet = None,
				collectAttendance = true,
				groups = Seq(
					ExtractedSmallGroup(
						name = "Group 1",
						limit = Some(15),
						events = Seq(
							ExtractedSmallGroupEvent(None, Seq(cuscav), Seq(WeekRange(15, 24)), Some(DayOfWeek.Monday), Some(new LocalTime(14, 0)), Some(new LocalTime(16, 0)), Some(NamedLocation("S0.27")))
						)
					),
					ExtractedSmallGroup(
						name = "Group 2",
						limit = Some(20),
						events = Seq(
							ExtractedSmallGroupEvent(None, Seq(cuscav), Seq(WeekRange(15, 24)), Some(DayOfWeek.Tuesday), Some(new LocalTime(14, 0)), Some(new LocalTime(16, 0)), Some(NamedLocation("S0.27")))
						)
					),
					ExtractedSmallGroup(
						name = "Group 3",
						limit = Some(9),
						events = Seq(
							ExtractedSmallGroupEvent(None, Seq(cuscav), Seq(WeekRange(15, 24)), Some(DayOfWeek.Wednesday), Some(new LocalTime(14, 0)), Some(new LocalTime(16, 0)), Some(NamedLocation("S0.27")))
						)
					),
					ExtractedSmallGroup(
						name = "Group 4",
						limit = Some(15),
						events = Seq(
							ExtractedSmallGroupEvent(None, Seq(cuscav), Seq(WeekRange(15, 24)), Some(DayOfWeek.Thursday), Some(new LocalTime(14, 0)), Some(new LocalTime(16, 0)), Some(NamedLocation("S0.27")))
						)
					)
				)
			)
		))

		bindingResult.hasErrors should be (false)
	}

}