package uk.ac.warwick.tabula.commands.groups

import enumeratum.{Enum, EnumEntry}
import org.joda.time.LocalDateTime
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventOccurrence.WeekNumber
import uk.ac.warwick.tabula.data.model.groups.WeekRange.Week
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEventAttendance, _}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.EnumTwoWayConverter
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.immutable.SortedMap
import scala.jdk.CollectionConverters._

sealed abstract class SmallGroupAttendanceState(val code:String, val description: String, val attendanceState: AttendanceState) extends EnumEntry {
  override val entryName: String = code
  def getName: String = this.toString
}

object SmallGroupAttendanceState extends Enum[SmallGroupAttendanceState] {
  case object Attended extends SmallGroupAttendanceState(AttendanceState.Attended.dbValue, AttendanceState.Attended.description, AttendanceState.Attended)
  case object AttendedRemotely extends SmallGroupAttendanceState("attended-remotely", AttendanceState.Attended.description, AttendanceState.Attended)
  case object MissedAuthorised extends SmallGroupAttendanceState(AttendanceState.MissedAuthorised.dbValue, AttendanceState.MissedAuthorised.description, AttendanceState.MissedAuthorised)
  case object MissedUnauthorised extends SmallGroupAttendanceState(AttendanceState.MissedUnauthorised.dbValue, AttendanceState.MissedUnauthorised.description, AttendanceState.MissedUnauthorised)
  case object NotRecorded extends SmallGroupAttendanceState(AttendanceState.NotRecorded.dbValue, AttendanceState.NotRecorded.description, AttendanceState.NotRecorded)
  case object Late extends SmallGroupAttendanceState(AttendanceState.NotRecorded.dbValue, AttendanceState.NotRecorded.description, AttendanceState.NotRecorded)
  case object NotExpected extends SmallGroupAttendanceState(AttendanceState.NotRecorded.dbValue, AttendanceState.NotRecorded.description, AttendanceState.NotRecorded) // The user is no longer in the group so is not expected to attend
  case object NotExpectedPast extends SmallGroupAttendanceState(AttendanceState.NotRecorded.dbValue, AttendanceState.NotRecorded.description, AttendanceState.NotRecorded) // The user wasn't in the group when this event took place

  override def values: IndexedSeq[SmallGroupAttendanceState] = findValues

  def from(attendance: Option[SmallGroupEventAttendance]): SmallGroupAttendanceState = attendance.map(_.state) match {
    case Some(AttendanceState.Attended) if !attendance.get.onlineAttendance => Attended
    case Some(AttendanceState.Attended) => AttendedRemotely
    case Some(AttendanceState.MissedAuthorised) => MissedAuthorised
    case Some(AttendanceState.MissedUnauthorised) => MissedUnauthorised
    case Some(AttendanceState.NotRecorded) if attendance.exists(a => !a.expectedToAttend) => NotExpectedPast
    case _ => NotRecorded // null
  }
}

class SmallGroupAttendanceStateConverter extends EnumTwoWayConverter(SmallGroupAttendanceState)

object ViewSmallGroupAttendanceCommand {
  type EventInstance = (SmallGroupEvent, SmallGroupEventOccurrence.WeekNumber)
  type PerUserAttendance = SortedMap[User, SortedMap[EventInstance, (SmallGroupAttendanceState, Option[SmallGroupEventAttendance])]]
  type PerUserAttendanceNotes = Map[User, Map[EventInstance, SmallGroupEventAttendanceNote]]

  case class SmallGroupAttendanceInformation(
    instances: Seq[EventInstance],
    attendance: PerUserAttendance,
    notes: PerUserAttendanceNotes
  )

  type Command = Appliable[SmallGroupAttendanceInformation]

  def apply(group: SmallGroup): Command =
    new ViewSmallGroupAttendanceCommand(group)
      with ComposableCommand[SmallGroupAttendanceInformation]
      with ViewSmallGroupAttendancePermissions
      with AutowiringSmallGroupServiceComponent
      with AutowiringUserLookupComponent
      with ReadOnly with Unaudited {
      override lazy val eventName = "ViewSmallGroupAttendance"
    }

  // Sort users by last name, first name
  implicit val defaultOrderingForUser: Ordering[User] = Ordering.by { user: User => (user.getLastName, user.getFirstName, user.getUserId) }

  implicit val defaultOrderingForEventInstance: Ordering[(SmallGroupEvent, WeekNumber)] = Ordering.by { instance: EventInstance =>
    instance match {
      case (event, week) =>
        val weekValue = week * 7 * 24
        val dayValue = (event.day.getAsInt - 1) * 24
        val hourValue = event.startTime.getHourOfDay

        (weekValue + dayValue + hourValue, week, event.id)
    }
  }

  def allEventInstances(group: SmallGroup, occurrences: Seq[SmallGroupEventOccurrence]): Seq[((SmallGroupEvent, Week), Option[SmallGroupEventOccurrence])] =
    group.events.filter {
      !_.isUnscheduled
    }.flatMap { event =>
      val allWeeks = event.weekRanges.flatMap(_.toWeeks)
      allWeeks.map { week =>
        val occurrence = occurrences.find { o =>
          o.event == event && o.week == week
        }

        ((event, week), occurrence)
      }
    }

  def attendanceForStudent(
    allEventInstances: Seq[(EventInstance, Option[SmallGroupEventOccurrence])],
    isLate: EventInstance => Boolean
  )(user: User): SortedMap[(SmallGroupEvent, WeekNumber), (SmallGroupAttendanceState, Option[SmallGroupEventAttendance])] = {
    val userAttendance = allEventInstances.map { case ((event, week), occurrence) =>
      val instance = (event, week)
      val attendance = occurrence.flatMap(_.attendance.asScala.find(_.universityId == user.getWarwickId))
      val attendanceState = SmallGroupAttendanceState.from(attendance)

      val state =
        if (attendanceState == SmallGroupAttendanceState.NotRecorded)
          if (!event.group.students.includesUser(user))
            SmallGroupAttendanceState.NotExpected
          else if (isLate(event, week))
            SmallGroupAttendanceState.Late
          else
            attendanceState
        else attendanceState

      instance -> (state, attendance)
    }

    SortedMap(userAttendance: _*)
  }
}

class ViewSmallGroupAttendanceCommand(val group: SmallGroup)
  extends CommandInternal[ViewSmallGroupAttendanceCommand.SmallGroupAttendanceInformation] with ViewSmallGroupAttendanceState with TaskBenchmarking {
  self: SmallGroupServiceComponent with UserLookupComponent =>

  import uk.ac.warwick.tabula.commands.groups.ViewSmallGroupAttendanceCommand._

  if (!group.groupSet.collectAttendance) throw new ItemNotFoundException()

  override def applyInternal(): SmallGroupAttendanceInformation = {
    val occurrences = benchmarkTask("Get all small group event occurrences for the group") {
      smallGroupService.findAttendanceByGroup(group)
    }

    // Build a list of all the events and week information, with an optional register
    val instances = benchmarkTask("Translate small group events into instances") {
      allEventInstances(group, occurrences)
    }

    // Build the list of all users who are in the group, or have attended one or more occurrences of the group
    val allStudents = benchmarkTask("Get a list of all registered or attended users") {
      (group.students.users ++
        userLookup.usersByWarwickUniIds(occurrences.flatMap(_.attendance.asScala).map(_.universityId)).values.toSeq)
        .toSeq
    }

    val attendance = benchmarkTask("For each student, build an attended list for each instance") {
      val attendance = allStudents.map { user => user -> attendanceForStudent(instances, isLate(user))(user) }

      SortedMap(attendance: _*)
    }

    val existingAttendanceNotes = benchmarkTask("Get attendance notes") {
      smallGroupService.findAttendanceNotes(allStudents.map(_.getWarwickId), occurrences).groupBy(_.student).map {
        case (student, notes) =>
          MemberOrUser(student).asUser -> notes.groupBy(n => (n.occurrence.event, n.occurrence.week)).view.mapValues(_.head).toMap
      }.withDefaultValue(Map())
    }
    val attendanceNotes = allStudents.map { student => student -> existingAttendanceNotes.getOrElse(student, Map()) }.toMap

    SmallGroupAttendanceInformation(
      instances = instances.map { case ((event, week), _) => (event, week) }.sorted,
      attendance = attendance,
      attendanceNotes
    )
  }

  private def isLate(user: User)(instance: EventInstance): Boolean = instance match {
    case (event, week: SmallGroupEventOccurrence.WeekNumber) =>
      // Can't be late if the student is no longer in that group
      event.group.students.includesUser(user) &&
        // Get the actual end date of the event in this week
        event.endDateTimeForWeek(week).exists(_.isBefore(LocalDateTime.now))
  }

}

trait ViewSmallGroupAttendancePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ViewSmallGroupAttendanceState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.SmallGroupEvents.ViewRegister, group)
  }
}

trait ViewSmallGroupAttendanceState {
  def group: SmallGroup
}
