package uk.ac.warwick.tabula.commands.groups

import org.joda.time.{DateTime, DateTimeConstants, LocalDateTime, LocalTime}
import uk.ac.warwick.tabula.commands.groups.SmallGroupAttendanceState._
import uk.ac.warwick.tabula.commands.groups.ViewSmallGroupAttendanceCommand.SmallGroupAttendanceInformation
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventOccurrence.WeekNumber
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, SmallGroup, SmallGroupEvent, SmallGroupEventAttendance, SmallGroupEventOccurrence, SmallGroupSet, WeekRange}
import uk.ac.warwick.tabula.data.model.{UnspecifiedTypeUserGroup, UserGroup}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{AcademicYear, MockUserLookup, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class ViewSmallGroupAttendanceCommandTest extends TestBase with Mockito {

	val baseLocalDateTime = new DateTime(2014, DateTimeConstants.OCTOBER, 19, 9, 18, 33, 0)

	trait BaseFixture {
		val mockUserLookup = new MockUserLookup

		def wireUserLookup(userGroup: UnspecifiedTypeUserGroup): Unit = userGroup match {
			case cm: UserGroupCacheManager => wireUserLookup(cm.underlying)
			case ug: UserGroup => ug.userLookup = mockUserLookup
		}

		trait CommandTestSupport extends SmallGroupServiceComponent with UserLookupComponent with WeekToDateConverterComponent {
			val smallGroupService: SmallGroupService = mock[SmallGroupService]
			val weekToDateConverter: WeekToDateConverter = smartMock[WeekToDateConverter]
			val userLookup: MockUserLookup = mockUserLookup
		}
	}

	trait Fixture extends BaseFixture {
		val set = new SmallGroupSet
		set.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

		val group = new SmallGroup(set)
		wireUserLookup(group.students)

		// The group has two events. Event 1 runs at Monday 11am on week 2, 3 and 4; Event 2 runs at Monday 3pm on weeks 1, 3 and 7
		val event1 = new SmallGroupEvent(group)
		event1.day = DayOfWeek.Monday
		event1.startTime = new LocalTime(11, 0)
		event1.endTime = new LocalTime(12, 0)
		event1.weekRanges = Seq(WeekRange(2, 4))

		val event2 = new SmallGroupEvent(group)
		event2.day = DayOfWeek.Monday
		event2.startTime = new LocalTime(15, 0)
		event2.endTime = new LocalTime(16, 0)
		event2.weekRanges = Seq(WeekRange(1), WeekRange(3), WeekRange(7))

		group.addEvent(event1)
		group.addEvent(event2)
	}

	@Test
	def commandApplyNoData() = withFakeTime(baseLocalDateTime) { new Fixture() {
		val command = new ViewSmallGroupAttendanceCommand(group) with CommandTestSupport

		command.smallGroupService.findAttendanceByGroup(group) returns Seq()
		command.smallGroupService.findAttendanceNotes(Seq(), Seq()) returns Seq()

		val info: SmallGroupAttendanceInformation = command.applyInternal()

		info.instances should be (Seq(
			(event2, 1),
			(event1, 2),
			(event1, 3),
			(event2, 3),
			(event1, 4),
			(event2, 7)
		))
		info.attendance should be ('empty)
	}}

	@Test
	def commandApply() = withFakeTime(baseLocalDateTime) { new Fixture() {
		mockUserLookup.registerUsers("user1", "user2", "user3", "user4", "user5")

		val user1: User = mockUserLookup.getUserByUserId("user1")
		val user2: User = mockUserLookup.getUserByUserId("user2")
		val user3: User = mockUserLookup.getUserByUserId("user3")
		val user4: User = mockUserLookup.getUserByUserId("user4")
		val user5: User = mockUserLookup.getUserByUserId("user5")

		group.students.add(user1)
		group.students.add(user2)
		group.students.add(user3)
		group.students.add(user4)

		// user5 turned up to the first occurrence and then left

		// Recorded attendance for week 1 and both in 3 - rest haven't happened yet, 2 is missing

		// Everyone turned up for week 1
		val occurrence1 = new SmallGroupEventOccurrence
		occurrence1.event = event2
		occurrence1.week = 1
		attendance(occurrence1, user1, AttendanceState.Attended)
		attendance(occurrence1, user2, AttendanceState.Attended)
		attendance(occurrence1, user3, AttendanceState.Attended)
		attendance(occurrence1, user4, AttendanceState.Attended)
		attendance(occurrence1, user5, AttendanceState.Attended)

		// User3 missed the first seminar in week 3, user4 missed the second
		val occurrence2 = new SmallGroupEventOccurrence
		occurrence2.event = event1
		occurrence2.week = 3
		attendance(occurrence2, user1, AttendanceState.Attended)
		attendance(occurrence2, user2, AttendanceState.Attended)
		attendance(occurrence2, user3, AttendanceState.MissedUnauthorised)
		attendance(occurrence2, user4, AttendanceState.Attended)
		attendance(occurrence2, user5, AttendanceState.MissedAuthorised)

		val occurrence3 = new SmallGroupEventOccurrence
		occurrence3.event = event2
		occurrence3.week = 3
		attendance(occurrence3, user1, AttendanceState.Attended)
		attendance(occurrence3, user2, AttendanceState.Attended)
		attendance(occurrence3, user3, AttendanceState.Attended)
		attendance(occurrence3, user4, AttendanceState.MissedUnauthorised)
		attendance(occurrence3, user5, AttendanceState.MissedUnauthorised)

		val command = new ViewSmallGroupAttendanceCommand(group) with CommandTestSupport
		command.smallGroupService.findAttendanceByGroup(group) returns Seq(occurrence1, occurrence2, occurrence3)
		command.smallGroupService.findAttendanceNotes(
			Seq(user1, user2, user3, user4, user5).map(_.getWarwickId),
			Seq(occurrence1, occurrence2, occurrence3)
		) returns Seq()

		command.weekToDateConverter.toLocalDatetime(1, DayOfWeek.Monday, new LocalTime(16, 0), set.academicYear) returns Some(new LocalDateTime(2014, DateTimeConstants.SEPTEMBER, 29, 16, 0))
		command.weekToDateConverter.toLocalDatetime(2, DayOfWeek.Monday, new LocalTime(12, 0), set.academicYear) returns Some(new LocalDateTime(2014, DateTimeConstants.OCTOBER, 6, 16, 0))
		command.weekToDateConverter.toLocalDatetime(3, DayOfWeek.Monday, new LocalTime(12, 0), set.academicYear) returns Some(new LocalDateTime(2014, DateTimeConstants.OCTOBER, 13, 12, 0))
		command.weekToDateConverter.toLocalDatetime(3, DayOfWeek.Monday, new LocalTime(16, 0), set.academicYear) returns Some(new LocalDateTime(2014, DateTimeConstants.OCTOBER, 13, 16, 0))
		command.weekToDateConverter.toLocalDatetime(4, DayOfWeek.Monday, new LocalTime(12, 0), set.academicYear) returns Some(new LocalDateTime(2014, DateTimeConstants.OCTOBER, 20, 12, 0))
		command.weekToDateConverter.toLocalDatetime(7, DayOfWeek.Monday, new LocalTime(16, 0), set.academicYear) returns Some(new LocalDateTime(2014, DateTimeConstants.OCTOBER, 29, 16, 0))

		val info: SmallGroupAttendanceInformation = command.applyInternal()

		info.instances should be (Seq(
			(event2, 1),
			(event1, 2),
			(event1, 3),
			(event2, 3),
			(event1, 4),
			(event2, 7)
		))

		// Map all the SortedMaps to Seqs to preserve the order they've been set as
		val userAttendanceSeqs: Seq[(User, Seq[((SmallGroupEvent, WeekNumber), SmallGroupAttendanceState)])] = info.attendance.toSeq.map { case (user, attendance) =>
			user -> attendance.toSeq
		}

		userAttendanceSeqs should be (Seq(
			(user1, Seq(
				((event2, 1), Attended),
				((event1, 2), Late),
				((event1, 3), Attended),
				((event2, 3), Attended),
				((event1, 4), NotRecorded),
				((event2, 7), NotRecorded)
			)),

			(user2, Seq(
				((event2, 1), Attended),
				((event1, 2), Late),
				((event1, 3), Attended),
				((event2, 3), Attended),
				((event1, 4), NotRecorded),
				((event2, 7), NotRecorded)
			)),

			(user3, Seq(
				((event2, 1), Attended),
				((event1, 2), Late),
				((event1, 3), MissedUnauthorised),
				((event2, 3), Attended),
				((event1, 4), NotRecorded),
				((event2, 7), NotRecorded)
			)),

			(user4, Seq(
				((event2, 1), Attended),
				((event1, 2), Late),
				((event1, 3), Attended),
				((event2, 3), MissedUnauthorised),
				((event1, 4), NotRecorded),
				((event2, 7), NotRecorded)
			)),

			(user5, Seq(
				((event2, 1), Attended),
				((event1, 2), NotExpected),
				((event1, 3), MissedAuthorised),
				((event2, 3), MissedUnauthorised),
				((event1, 4), NotExpected),
				((event2, 7), NotExpected)
			))
		))
	}}

	@Test
	def tab1534() = withFakeTime(baseLocalDateTime) { new BaseFixture {
		// An intricacy of the way that SortedMap works means that if you have two
		// keys with an identical sort order, they'll get merged into a single key.
		// This means that we risk students with the same name getting merged into one.
		// Bad!
		val set = new SmallGroupSet
		set.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

		val group = new SmallGroup(set)
		wireUserLookup(group.students)

		val event = new SmallGroupEvent(group)
		event.day = DayOfWeek.Monday
		event.startTime = new LocalTime(11, 0)
		event.endTime = new LocalTime(12, 0)
		event.weekRanges = Seq(WeekRange(2, 4))

		group.addEvent(event)

		mockUserLookup.registerUsers("user1", "user2", "user3")

		val user1: User = mockUserLookup.getUserByUserId("user1")
		val user2: User = mockUserLookup.getUserByUserId("user2")
		val user3: User = mockUserLookup.getUserByUserId("user3")

		// Give user2 and user3 the same name
		user2.setFirstName("Billy")
		user2.setLastName("Sameson")
		user2.setFullName("Billy Sameson")

		user3.setFirstName("Billy")
		user3.setLastName("Sameson")
		user3.setFullName("Billy Sameson")

		val occurrence = new SmallGroupEventOccurrence
		occurrence.event = event
		occurrence.week = 1
		attendance(occurrence, user1, AttendanceState.Attended)
		attendance(occurrence, user2, AttendanceState.Attended)
		attendance(occurrence, user3, AttendanceState.Attended)

		val command = new ViewSmallGroupAttendanceCommand(group) with CommandTestSupport
		command.smallGroupService.findAttendanceByGroup(group) returns Seq(occurrence)
		command.smallGroupService.findAttendanceNotes(
			any[Seq[String]],
			any[Seq[SmallGroupEventOccurrence]]
		) returns Seq()

		command.weekToDateConverter.toLocalDatetime(2, DayOfWeek.Monday, new LocalTime(12, 0), set.academicYear) returns Some(new LocalDateTime(2014, DateTimeConstants.OCTOBER, 6, 12, 0))
		command.weekToDateConverter.toLocalDatetime(3, DayOfWeek.Monday, new LocalTime(12, 0), set.academicYear) returns Some(new LocalDateTime(2014, DateTimeConstants.OCTOBER, 13, 12, 0))
		command.weekToDateConverter.toLocalDatetime(4, DayOfWeek.Monday, new LocalTime(12, 0), set.academicYear) returns Some(new LocalDateTime(2014, DateTimeConstants.OCTOBER, 20, 12, 0))

		val info: SmallGroupAttendanceInformation = command.applyInternal()
		info.attendance.keySet.size should be (3) // If it's 2, we're bad
	}}

	private def attendance(occurrence: SmallGroupEventOccurrence, user: User, state: AttendanceState) {
		val attendance = new SmallGroupEventAttendance
		attendance.occurrence = occurrence
		attendance.universityId = user.getWarwickId
		attendance.state = state
		occurrence.attendance.add(attendance)
	}

}