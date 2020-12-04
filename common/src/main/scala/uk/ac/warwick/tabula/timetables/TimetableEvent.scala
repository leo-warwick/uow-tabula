package uk.ac.warwick.tabula.timetables

import org.joda.time.{LocalDateTime, LocalTime}
import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.data.model.{Location, StudentRelationshipType}
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.timetables.TimetableEvent.Parent
import uk.ac.warwick.userlookup.User

case class TimetableEvent(
  uid: String,
  name: String,
  title: String,
  description: String,
  eventType: TimetableEventType,
  weekRanges: Seq[WeekRange],
  day: DayOfWeek,
  startTime: LocalTime,
  endTime: LocalTime,
  location: Option[Location],
  onlineDeliveryUrl: Option[String],
  parent: Parent,
  comments: Option[String],
  staff: Seq[User],
  students: Seq[User],
  year: AcademicYear,
  relatedUrl: Option[RelatedUrl],
  attendance: Map[WeekRange.Week, AttendanceState]
)

case class RelatedUrl(urlString: String, title: Option[String])

object TimetableEvent {

  sealed trait Context

  object Context {

    case object Student extends Context

    case object Staff extends Context

  }

  sealed trait Parent {
    val shortName: Option[String]
    val fullName: Option[String]
  }

  case class Empty(override val shortName: Option[String], override val fullName: Option[String]) extends Parent

  case class Department(override val shortName: Option[String], override val fullName: Option[String]) extends Parent

  case class Module(override val shortName: Option[String], override val fullName: Option[String]) extends Parent

  case class Relationship(override val shortName: Option[String], override val fullName: Option[String]) extends Parent

  object Parent {
    def apply(): Empty = {
      Empty(None, None)
    }

    def apply(department: model.Department): Department = {
      Department(Option(department).map(_.code.toUpperCase), Option(department).map(_.name))
    }

    def apply(module: Option[model.Module]): Module = {
      Module(module.map(_.code.toUpperCase), module.map(_.name))
    }

    def apply(relationship: StudentRelationshipType): Relationship = {
      Relationship(Option(relationship.description), Option(relationship.description))
    }

    def apply(relationshipTypes: Seq[StudentRelationshipType]): Relationship = {
      val string = relationshipTypes.map(_.description).distinct.mkString(", ")
      Relationship(Option(string), Option(string))
    }
  }

  def apply(sge: SmallGroupEvent, attendance: Map[WeekRange.Week, AttendanceState], withoutWeeks: Seq[SmallGroupEventOccurrence.WeekNumber] = Seq()): TimetableEvent =
    eventForSmallGroupEventInWeeks(sge, WeekRange.combine(sge.weekRanges.flatMap(_.toWeeks).diff(withoutWeeks)), attendance)

  def apply(sgo: SmallGroupEventOccurrence, attendance: AttendanceState): TimetableEvent = eventForSmallGroupEventInWeeks(sgo.event, Seq(WeekRange(sgo.week)), Map(sgo.week -> attendance))

  private def eventForSmallGroupEventInWeeks(sge: SmallGroupEvent, weekRanges: Seq[WeekRange], attendance: Map[WeekRange.Week, AttendanceState]): TimetableEvent =
    TimetableEvent(
      uid = sge.id,
      name = s"${sge.group.groupSet.name}: ${sge.group.name}",
      title = Option(sge.title).getOrElse(""),
      description = s"${sge.group.groupSet.name}: ${sge.group.name}",
      eventType = smallGroupFormatToTimetableEventType(sge.group.groupSet.format),
      weekRanges = weekRanges,
      day = sge.day,
      startTime = sge.startTime,
      endTime = sge.endTime,
      location = Option(sge.location),
      onlineDeliveryUrl = Option(sge.onlineDeliveryUrl),
      parent = TimetableEvent.Parent(Option(sge.group.groupSet.module)),
      comments = None,
      staff = sge.tutors.users.toSeq,
      students = sge.group.students.users.toSeq,
      year = sge.group.groupSet.academicYear,
      relatedUrl = Option(RelatedUrl(sge.relatedUrl, Option(sge.relatedUrlTitle))),
      attendance = attendance
    )

  private def smallGroupFormatToTimetableEventType(sgf: SmallGroupFormat): TimetableEventType = sgf match {
    case SmallGroupFormat.Seminar => TimetableEventType.Seminar
    case SmallGroupFormat.Lab => TimetableEventType.Practical
    case SmallGroupFormat.Tutorial => TimetableEventType.Other("Tutorial")
    case SmallGroupFormat.Project => TimetableEventType.Other("Project")
    case SmallGroupFormat.Example => TimetableEventType.Other("Example")
    case SmallGroupFormat.Workshop => TimetableEventType.Other("Workshop")
    case SmallGroupFormat.Lecture => TimetableEventType.Lecture
    case SmallGroupFormat.Exam => TimetableEventType.Other("Exam")
    case SmallGroupFormat.Meeting => TimetableEventType.Meeting
  }

  // Companion object is one of the places searched for an implicit Ordering, so
  // this will be the default when ordering a list of timetable events.
  implicit val defaultOrdering: Ordering[TimetableEvent] = Ordering.by { event: TimetableEvent => (Option(event.weekRanges).filter(_.nonEmpty).map {
    _.minBy {
      _.minWeek
    }.minWeek
  }, Option(event.day).map(_.jodaDayOfWeek), Option(event.startTime).map(_.getMillisOfDay), Option(event.endTime).map(_.getMillisOfDay), event.name, event.title, event.uid)
  }

}

@SerialVersionUID(2903326840601345835L) sealed abstract class TimetableEventType(val code: String, val displayName: String, val core: Boolean = true) extends Serializable

object TimetableEventType {

  case object Lecture extends TimetableEventType("LEC", "Lecture")
  case object Practical extends TimetableEventType("PRA", "Practical")
  case object Seminar extends TimetableEventType("SEM", "Seminar")
  case object Induction extends TimetableEventType("IND", "Induction")
  case object Meeting extends TimetableEventType("MEE", "Meeting")
  case object Exam extends TimetableEventType("EXA", "Exam")
  case class Other(c: String) extends TimetableEventType(c, c, false)

  // lame manual collection. Keep in sync with the case objects above
  // Don't change this to a val https://warwick.slack.com/archives/C029QTGBN/p1493995125972397
  def members = Seq(Lecture, Practical, Seminar, Induction, Meeting, Exam)

  def unapply(code: String): Option[TimetableEventType] = code match {
    case Lecture.code | Lecture.displayName | "LEC ALLOCATE IN TABULA" => Some(Lecture)
    case Practical.code | Practical.displayName | "PRA ALLOCATE IN TABULA" => Some(Practical)
    case Seminar.code | Seminar.displayName | "SEM ALLOCATE IN TABULA" | "SEM CAPTURE ALLOCATE IN TABULA" => Some(Seminar)
    case Induction.code | Induction.displayName => Some(Induction)
    case Meeting.code | Meeting.displayName => Some(Meeting)
    case Exam.code | Exam.displayName | "EXAMS ALLOCATE IN TABULA" => Some(Exam)
    case _ => None
  }

  def apply(code: String): TimetableEventType = code match {
    case TimetableEventType(t) => t
    case _ => Other(code)
  }
}

case class EventOccurrence(
  uid: String,
  name: String,
  title: String,
  description: String,
  eventType: TimetableEventType,
  start: LocalDateTime,
  end: LocalDateTime,
  location: Option[Location],
  onlineDeliveryUrl: Option[String],
  parent: TimetableEvent.Parent,
  comments: Option[String],
  staff: Seq[User],
  relatedUrl: Option[RelatedUrl],
  attendance: Option[AttendanceState]
)

object EventOccurrence {
  def busy(occurrence: EventOccurrence): EventOccurrence = {
    EventOccurrence(
      occurrence.uid,
      "",
      "",
      "",
      TimetableEventType.Other("Busy"),
      occurrence.start,
      occurrence.end,
      None,
      None,
      TimetableEvent.Parent(),
      None,
      Nil,
      None,
      None
    )
  }
}
