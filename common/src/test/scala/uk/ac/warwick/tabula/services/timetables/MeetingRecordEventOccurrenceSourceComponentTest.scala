package uk.ac.warwick.tabula.services.timetables

import org.joda.time.{DateTime, LocalDate, LocalDateTime}
import uk.ac.warwick.tabula.data.model.{AbstractMeetingRecord, StudentMember, StudentRelationship, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.timetables.{EventOccurrence, TimetableEvent, TimetableEventType}
import uk.ac.warwick.tabula.{CurrentUser, Fixtures, Mockito, TestBase}

class MeetingRecordEventOccurrenceSourceComponentTest extends TestBase with Mockito {
  val student = new StudentMember
  student.universityId = "university ID"

  val user: CurrentUser = mock[CurrentUser]
  user.profile returns Some(student)

  val occurrence = EventOccurrence("", "", "", "", TimetableEventType.Meeting, LocalDateTime.now, LocalDateTime.now, None, None, None, TimetableEvent.Parent(), None, Nil, None, None, None, None, None)

  val relationshipType = StudentRelationshipType("t", "t", "t", "t")
  val relationships = Seq(StudentRelationship(Fixtures.staff(), relationshipType, student, DateTime.now))
  val meetings = Seq(new AbstractMeetingRecord {
    override def toEventOccurrence(context: TimetableEvent.Context) = Some(occurrence)
  })
  meetings.head.relationships = relationships

  val source = new MeetingRecordEventOccurrenceSourceComponent
    with RelationshipServiceComponent
    with MeetingRecordServiceComponent
    with SecurityServiceComponent {

    val relationshipService: RelationshipService = mock[RelationshipService]
    val meetingRecordService: MeetingRecordService = mock[MeetingRecordService]
    val securityService: SecurityService = mock[SecurityService]

  }

  val start = LocalDate.now().minusDays(1)
  val end = LocalDate.now().plusDays(1)

  source.relationshipService.getAllPastAndPresentRelationships(student) returns relationships
  source.relationshipService.listCurrentStudentRelationshipsWithMember(student) returns Nil
  source.securityService.can(user, Permissions.Profiles.MeetingRecord.Read(relationshipType), student) returns true
  source.meetingRecordService.listAll(relationships.toSet, Some(student)) returns meetings

  @Test
  def callsBothServicesAndGeneratesOccurrence(): Unit = {
    source.eventOccurrenceSource.occurrencesFor(student, user, TimetableEvent.Context.Staff, start, end).futureValue.events should be(Seq(occurrence))
  }


}
