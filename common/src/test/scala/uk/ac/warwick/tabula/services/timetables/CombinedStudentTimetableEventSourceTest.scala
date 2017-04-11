package uk.ac.warwick.tabula.services.timetables

import org.joda.time.LocalTime
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}
import uk.ac.warwick.tabula.{AcademicYear, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

import scala.concurrent.Future

class CombinedStudentTimetableEventSourceTest extends TestBase with Mockito{

	val student = new StudentMember
	student.universityId = "university ID"
	val user = new User()

	val ttEvent= TimetableEvent("", "From Timetable","", "",TimetableEventType.Induction,Nil,DayOfWeek.Monday,LocalTime.now, LocalTime.now,None,TimetableEvent.Parent(),None,Nil,Nil,AcademicYear(2013),None,Map())
	val timetableEvents = Seq(ttEvent)

	val sgEvent= TimetableEvent("", "From Group","", "",TimetableEventType.Induction,Nil,DayOfWeek.Monday,LocalTime.now, LocalTime.now,None,TimetableEvent.Parent(),None,Nil,Nil,AcademicYear(2013),None,Map())
	val groupEvents = Seq(sgEvent)

	val source = new CombinedStudentTimetableEventSourceComponent
		with StaffAndStudentTimetableFetchingServiceComponent
		with SmallGroupEventTimetableEventSourceComponent{
		val staffGroupEventSource: StaffTimetableEventSource = mock[StaffTimetableEventSource]
		val studentGroupEventSource: StudentTimetableEventSource = mock[StudentTimetableEventSource]
		val moduleGroupEventSource: ModuleTimetableEventSource = mock[ModuleTimetableEventSource]
		val timetableFetchingService: CompleteTimetableFetchingService = mock[CompleteTimetableFetchingService]
	}

	source.timetableFetchingService.getTimetableForStudent(student.universityId) returns Future.successful(EventList.fresh(timetableEvents))
	source.studentGroupEventSource.eventsFor(student, currentUser, TimetableEvent.Context.Student) returns Future.successful(EventList.fresh(groupEvents))

	@Test
	def callsBothServicesAndAggregatesTheResult(){
		val result = source.studentTimetableEventSource.eventsFor(student, currentUser, TimetableEvent.Context.Student).futureValue
		result.events should be (timetableEvents ++ groupEvents)
	}


}
