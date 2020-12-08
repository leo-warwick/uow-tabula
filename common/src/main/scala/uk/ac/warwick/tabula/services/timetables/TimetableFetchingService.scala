package uk.ac.warwick.tabula.services.timetables

import org.joda.time.DateTime
import uk.ac.warwick.tabula.helpers.ExecutionContexts.timetable
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.{Futures, Logging}
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.timetables.{EventOccurrence, TimetableEvent}

import scala.concurrent.Future

object TimetableFetchingService {

  /**
    * Yukkiness Ahead.
    *
    * Caches requre their payload to be serializable, but Scala's Seq trait isn't marked as serializable. Most of
    * the Seq implementations (Vector and LinkedList in particular, which are the defaults returned by Seq() and List() )
    * _are_ serializable, so it should be safe to match the Seqs to "Seq with Serializable" and assume it will never fail.
    */
  case class EventList(events: Seq[TimetableEvent] with java.io.Serializable, lastUpdated: Option[DateTime]) {
    def map(fn: Seq[TimetableEvent] with java.io.Serializable => Seq[TimetableEvent]): EventList =
      copy(events = fn(events).asInstanceOf[Seq[TimetableEvent] with java.io.Serializable])

    def filter(p: TimetableEvent => Boolean): EventList =
      copy(events = events.filter(p).asInstanceOf[Seq[TimetableEvent] with java.io.Serializable])

    def filterNot(p: TimetableEvent => Boolean): EventList =
      copy(events = events.filterNot(p).asInstanceOf[Seq[TimetableEvent] with java.io.Serializable])
  }

  object EventList {
    val empty: EventList = EventList(Nil, None)

    def fresh(events: Seq[TimetableEvent]): EventList = forDate(events, Some(DateTime.now))

    def forDate(events: Seq[TimetableEvent], lastUpdated: Option[DateTime]): EventList = events match {
      // can't use "case v: EventList" because the type inference engine in 2.10 can't cope.
      case v: Seq[TimetableEvent] with java.io.Serializable => EventList(v, lastUpdated)
      case _ => throw new RuntimeException("Unserializable collection returned from TimetableFetchingService")
    }

    def combine(eventLists: Seq[EventList]): EventList =
      EventList.forDate(
        eventLists.flatMap(_.events),
        eventLists.flatMap(_.lastUpdated).sortBy(_.getMillis).headOption // Earliest
      )
  }

  case class EventOccurrenceList(events: Seq[EventOccurrence], lastUpdated: Option[DateTime]) {
    def map(fn: Seq[EventOccurrence] => Seq[EventOccurrence]): EventOccurrenceList =
      copy(events = fn(events))

    def filter(p: EventOccurrence => Boolean): EventOccurrenceList =
      copy(events = events.filter(p))

    def filterNot(p: EventOccurrence => Boolean): EventOccurrenceList =
      copy(events = events.filterNot(p))

    def ++(other: EventOccurrenceList): EventOccurrenceList = copy(
      events = events ++ other.events,
      lastUpdated = Seq(lastUpdated, other.lastUpdated).flatten.sortBy(_.getMillis).headOption
    )
  }

  object EventOccurrenceList {
    val empty: EventOccurrenceList = EventOccurrenceList(Nil, None)

    def fresh(events: Seq[EventOccurrence]): EventOccurrenceList = apply(events, Some(DateTime.now))

    def combine(eventLists: Seq[EventOccurrenceList]): EventOccurrenceList =
      EventOccurrenceList(
        eventLists.flatMap(_.events),
        eventLists.flatMap(_.lastUpdated).sortBy(_.getMillis).headOption // Earliest
      )
  }

}

trait PartialTimetableFetchingService

trait StudentTimetableFetchingService extends PartialTimetableFetchingService {
  def getTimetableForStudent(universityId: String): Future[EventList]
}

trait ModuleTimetableFetchingService extends PartialTimetableFetchingService {
  def getTimetableForModule(moduleCode: String, includeStudents: Boolean): Future[EventList]
}

trait CourseTimetableFetchingService extends PartialTimetableFetchingService {
  def getTimetableForCourse(courseCode: String): Future[EventList]
}

trait RoomTimetableFetchingService extends PartialTimetableFetchingService {
  def getTimetableForRoom(roomName: String): Future[EventList]
}

trait StaffTimetableFetchingService extends PartialTimetableFetchingService {
  def getTimetableForStaff(universityId: String): Future[EventList]
}

trait CompleteTimetableFetchingService
  extends StudentTimetableFetchingService
    with ModuleTimetableFetchingService
    with CourseTimetableFetchingService
    with RoomTimetableFetchingService
    with StaffTimetableFetchingService

trait StudentTimetableFetchingServiceComponent {
  def timetableFetchingService: StudentTimetableFetchingService
}

trait ModuleTimetableFetchingServiceComponent {
  def timetableFetchingService: ModuleTimetableFetchingService
}

trait CourseTimetableFetchingServiceComponent {
  def timetableFetchingService: CourseTimetableFetchingService
}

trait RoomTimetableFetchingServiceComponent {
  def timetableFetchingService: RoomTimetableFetchingService
}

trait StaffTimetableFetchingServiceComponent {
  def timetableFetchingService: StaffTimetableFetchingService
}

trait StaffAndStudentTimetableFetchingServiceComponent extends StudentTimetableFetchingServiceComponent with StaffTimetableFetchingServiceComponent {
  def timetableFetchingService: StudentTimetableFetchingService with StaffTimetableFetchingService
}

trait CompleteTimetableFetchingServiceComponent
  extends StaffAndStudentTimetableFetchingServiceComponent
    with ModuleTimetableFetchingServiceComponent
    with CourseTimetableFetchingServiceComponent
    with RoomTimetableFetchingServiceComponent {
  def timetableFetchingService: CompleteTimetableFetchingService
}

trait CombinedHttpTimetableFetchingServiceComponent extends CompleteTimetableFetchingServiceComponent {
  self: ScientiaHttpTimetableFetchingServiceComponent
    with CelcatConfigurationComponent
    with ExamTimetableConfigurationComponent =>

  lazy val timetableFetchingService = new CombinedTimetableFetchingService(
    scientiaHttpTimetableFetchingService,
    CelcatHttpTimetableFetchingService(celcatConfiguration),
    ExamTimetableHttpTimetableFetchingService(examTimetableConfiguration)
  )

}

object CombinedTimetableFetchingService {
  def mergeDuplicates(events: EventList): EventList = {
    // If an event runs on the same day, between the same times, in the same weeks, of the same type, on the same module,
    // in the same location, with the same online URL, with the same tutors, it is the same
    events.map(events => events.groupBy { event =>
      (event.year, event.day, event.startTime, event.endTime, event.weekRanges,
        event.eventType, event.parent.shortName, event.location.map(_.name), event.onlineDeliveryUrl, event.staff)
    }
      .view.mapValues {
        // values are Seq so List cons (::) never matches
        case event +: Nil => event
        case groupedEvents =>
          val event = groupedEvents.head
          TimetableEvent(
            event.uid,
            groupedEvents.flatMap(_.name.maybeText).headOption.getOrElse(""),
            groupedEvents.flatMap(_.title.maybeText).headOption.getOrElse(""),
            groupedEvents.flatMap(_.description.maybeText).headOption.getOrElse(""),
            event.eventType,
            event.weekRanges,
            event.day,
            event.startTime,
            event.endTime,
            groupedEvents.flatMap(_.location).headOption,
            groupedEvents.flatMap(_.onlineDeliveryUrl).headOption,
            groupedEvents.flatMap(_.deliveryMethod).headOption,
            event.parent,
            groupedEvents.flatMap(_.comments).headOption,
            groupedEvents.flatMap(_.staff).distinct,
            groupedEvents.flatMap(_.students).distinct,
            event.year,
            event.relatedUrl,
            event.attendance
          )
      }
      .values.toSeq)
  }
}

class CombinedTimetableFetchingService(services: PartialTimetableFetchingService*) extends CompleteTimetableFetchingService with Logging {

  import CombinedTimetableFetchingService._

  def getTimetableForStudent(universityId: String): Future[EventList] =
    Futures.combine(
      services
        .collect { case service: StudentTimetableFetchingService => service }
        .map {
          // On downstream failures, just return Nil
          _.getTimetableForStudent(universityId).recover { case t => logger.warn("Error fetching timetable", t); EventList.empty }
        },
      EventList.combine
    ).map(mergeDuplicates)

  def getTimetableForModule(moduleCode: String, includeStudents: Boolean): Future[EventList] =
    Futures.combine(
      services
        .collect { case service: ModuleTimetableFetchingService => service }
        .map {
          // On downstream failures, just return Nil
          _.getTimetableForModule(moduleCode, includeStudents).recover { case t => logger.warn("Error fetching timetable", t); EventList.empty }
        },
      EventList.combine
    ).map(mergeDuplicates)

  def getTimetableForCourse(courseCode: String): Future[EventList] =
    Futures.combine(
      services
        .collect { case service: CourseTimetableFetchingService => service }
        .map {
          // On downstream failures, just return Nil
          _.getTimetableForCourse(courseCode).recover { case t => logger.warn("Error fetching timetable", t); EventList.empty }
        },
      EventList.combine
    ).map(mergeDuplicates)

  def getTimetableForStaff(universityId: String): Future[EventList] =
    Futures.combine(
      services
        .collect { case service: StaffTimetableFetchingService => service }
        .map {
          // On downstream failures, just return Nil
          _.getTimetableForStaff(universityId).recover { case t => logger.warn("Error fetching timetable", t); EventList.empty }
        },
      EventList.combine
    ).map(mergeDuplicates)

  def getTimetableForRoom(roomName: String): Future[EventList] =
    Futures.combine(
      services
        .collect { case service: RoomTimetableFetchingService => service }
        .map {
          // On downstream failures, just return Nil
          _.getTimetableForRoom(roomName).recover { case t => logger.warn("Error fetching timetable", t); EventList.empty }
        },
      EventList.combine
    ).map(mergeDuplicates)
}
