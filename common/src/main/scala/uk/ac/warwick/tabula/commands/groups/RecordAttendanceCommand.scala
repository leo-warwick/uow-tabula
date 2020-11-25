package uk.ac.warwick.tabula.commands.groups

import org.joda.time.LocalDateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.groups.RecordAttendanceCommand._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroupEventAttendance, SmallGroupEventAttendanceNote, SmallGroupEventOccurrence}
import uk.ac.warwick.tabula.data.model.notifications.groups.reminders.SmallGroupEventAttendanceReminderNotification
import uk.ac.warwick.tabula.events.NotificationHandling
import uk.ac.warwick.tabula.helpers.{FoundUser, LazyMaps}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringEventAttendanceServiceComponent, AutowiringAttendanceMonitoringEventAttendanceServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.mutable
import scala.jdk.CollectionConverters._

object RecordAttendanceCommand {
  type UniversityId = String

  case class RecordAttendanceResult (
    occurrence: SmallGroupEventOccurrence,
    attendance: Seq[SmallGroupEventAttendance],
    deletions: Seq[UniversityId] // any students that had an attendance state set that have been returned to unrecorded
  )

  type Command = Appliable[RecordAttendanceResult]
    with RecordAttendanceState
    with SmallGroupEventInFutureCheck
    with PopulateOnForm
    with AddAdditionalStudent
    with RemoveAdditionalStudent
    with SelfValidating
    with CompletesNotifications[RecordAttendanceResult]

  def apply(event: SmallGroupEvent, week: Int, user: CurrentUser): Command =
    new RecordAttendanceCommand(event, week, user)
      with ComposableCommand[RecordAttendanceResult]
      with SmallGroupEventInFutureCheck
      with RecordAttendanceCommandPermissions
      with RecordAttendanceDescription
      with RecordAttendanceCommandValidation
      with AutowiringSmallGroupServiceComponent
      with AutowiringUserLookupComponent
      with AutowiringProfileServiceComponent
      with AutowiringAttendanceMonitoringEventAttendanceServiceComponent
      with RecordAttendanceNotificationCompletion
      with AutowiringFeaturesComponent
}

trait AddAdditionalStudent {
  self: SmallGroupServiceComponent with RecordAttendanceState =>
  def occurrence: SmallGroupEventOccurrence

  var additionalStudent: Member = _
  var replacedWeek: JInteger = _
  var replacedEvent: SmallGroupEvent = _

  lazy val manuallyAddedUniversityIds: mutable.Set[String] = occurrence.attendance.asScala.filter(_.addedManually).map(_.universityId)

  var linkedAttendance: SmallGroupEventAttendance = _

  def addAdditionalStudent(members: Seq[MemberOrUser]): Unit = {
    Option(additionalStudent)
      .filterNot { member => members.exists(_.universityId == member.universityId) }
      .foreach { member =>
        val attendance = transactional() {
          smallGroupService.saveOrUpdateAttendance(member.universityId, event, week, SmallGroupAttendanceState.NotRecorded, user)
        }

        attendance.addedManually = true
        Option(replacedEvent).foreach { event =>
          val replacedOccurrence = transactional() {
            smallGroupService.getOrCreateSmallGroupEventOccurrence(event, replacedWeek).getOrElse(throw new IllegalArgumentException(
              s"Week number $replacedWeek is not valid for event ${event.id}"
            ))
          }
          val replacedAttendance = transactional() {
            smallGroupService.getAttendance(member.universityId, replacedOccurrence) match {
              case Some(replaced) if replaced.state == AttendanceState.Attended =>
                replaced
              case Some(replaced) =>
                replaced.state = AttendanceState.MissedAuthorised
                smallGroupService.saveOrUpdate(replaced)
                replaced
              case None =>
                smallGroupService.saveOrUpdateAttendance(member.universityId, replacedEvent, replacedWeek, SmallGroupAttendanceState.MissedAuthorised, user)
            }
          }

          attendance.replacesAttendance = replacedAttendance
        }

        linkedAttendance = transactional() {
          smallGroupService.saveOrUpdate(attendance); attendance
        }

        studentsState.put(member.universityId, null)
      }
  }
}

trait RemoveAdditionalStudent {
  self: SmallGroupServiceComponent with RecordAttendanceState =>

  var removeAdditionalStudent: Member = _

  def doRemoveAdditionalStudent(members: Seq[MemberOrUser]): Unit = {
    Option(removeAdditionalStudent)
      .filter { member => members.exists(_.universityId == member.universityId) }
      .foreach { member =>
        transactional() {
          smallGroupService.deleteAttendance(member.universityId, event, week, isPermanent = true)
        }

        studentsState.remove(member.universityId)
      }
  }
}

abstract class RecordAttendanceCommand(val event: SmallGroupEvent, val week: Int, val user: CurrentUser)
  extends CommandInternal[RecordAttendanceResult]
    with RecordAttendanceState
    with AddAdditionalStudent
    with RemoveAdditionalStudent
    with PopulateOnForm
    with TaskBenchmarking {

  self: SmallGroupServiceComponent with UserLookupComponent with ProfileServiceComponent with FeaturesComponent
    with AttendanceMonitoringEventAttendanceServiceComponent =>

  if (!event.group.groupSet.collectAttendance) throw new ItemNotFoundException

  lazy val occurrence: SmallGroupEventOccurrence = transactional() {
    smallGroupService.getOrCreateSmallGroupEventOccurrence(event, week).getOrElse(
      throw new ItemNotFoundException
    )
  }

  lazy val initialState: Map[String, SmallGroupAttendanceState] = members.map { member =>
    member.universityId ->
      occurrence.attendance.asScala
        .find(_.universityId == member.universityId)
        .flatMap { a => Option(SmallGroupAttendanceState.from(Option(a))) }.orNull
  }.toMap

  var studentsState: JMap[UniversityId, SmallGroupAttendanceState] =
    LazyMaps.create { member: UniversityId => null: SmallGroupAttendanceState }.asJava

  lazy val members: Seq[MemberOrUser] = {
    (event.group.students.users.map { user =>
      val member = profileService.getMemberByUniversityId(user.getWarwickId)
      (false, MemberOrUser(member, user))
    } ++ occurrence.attendance.asScala.toSeq.map { a =>
      val member = profileService.getMemberByUniversityId(a.universityId)
      val user = userLookup.getUserByWarwickUniId(a.universityId)
      (a.addedManually, MemberOrUser(member, user))
    }).toSeq.distinct.sortBy { case (addedManually, mou) => (!addedManually, mou.lastName, mou.firstName, mou.universityId) }
      .map { case (_, mou) => mou }
  }

  lazy val attendanceNotes: Map[MemberOrUser, Map[SmallGroupEventOccurrence, SmallGroupEventAttendanceNote]] = benchmarkTask("Get attendance notes") {
    smallGroupService.findAttendanceNotes(members.map(_.universityId), Seq(occurrence)).groupBy(_.student).map {
      case (student, noteMap) => MemberOrUser(student) -> noteMap.groupBy(_.occurrence).map {
        case (o, notes) => o -> notes.head
      }
    }
  }

  lazy val attendances: Map[MemberOrUser, Option[SmallGroupEventAttendance]] = benchmarkTask("Get attendances") {
    val all = occurrence.attendance.asScala
    members.map { m => (m, all.find { a => a.universityId == m.universityId }) }.toMap
  }

  def attendanceMetadata(uniId: UniversityId): Option[String] = {
    occurrence.attendance.asScala
      .find(_.universityId == uniId)
      .map(attendance => {
        val userString = userLookup.getUserByUserId(attendance.updatedBy) match {
          case FoundUser(u) => s"by ${u.getFullName}, "
          case _ => ""
        }

        s"Recorded $userString${DateFormats.CSVDateTime.print(attendance.updatedDate)}"
      })
  }

  def populate(): Unit = {
    studentsState = initialState.asJava
  }

  def applyInternal(): RecordAttendanceResult = {

    val changes = studentsState.asScala.filter { case (studentId, state) =>
      initialState.get(studentId).orNull != state
    }

    val (deletes, updates) = changes.partition { case (_, state) => state == null }
    deletes.foreach { case (studentId, _) => smallGroupService.deleteAttendance(studentId, event, week) }
    val attendances = updates.map { case (studentId, state) => smallGroupService.saveOrUpdateAttendance(studentId, event, week, state, user) }.toSeq

    attendanceMonitoringEventAttendanceService.updateCheckpoints(attendances)
    if (occurrence.department.autoMarkMissedMonitoringPoints) {
      attendanceMonitoringEventAttendanceService.updateMissedCheckpoints(attendances, user)
    }

    RecordAttendanceResult(occurrence, attendances, deletes.keys.toSeq)
  }
}

trait RecordAttendanceCommandValidation extends SelfValidating {
  self: RecordAttendanceState with UserLookupComponent with SmallGroupEventInFutureCheck =>

  def validate(errors: Errors): Unit = {
    val invalidUsers: Seq[UniversityId] = studentsState.asScala.map {
      case (studentId, _) => studentId
    }.filter(s => !userLookup.getUserByWarwickUniId(s).isFoundUser).toSeq

    if (invalidUsers.nonEmpty) {
      errors.rejectValue("studentsState", "smallGroup.attendees.invalid", Array(invalidUsers), "")
    }

    // TAB-1791 Allow attendance to be recorded for users not in the group, they were in the group in the past or submitting would be a pain
    /*else {
      val missingUsers: Seq[UniversityId] = studentsState.asScala.map {
        case (studentId, _) => studentId
      }.filter(s => event.group.students.users.filter(u => u.getWarwickId() == s).length == 0).toSeq
      if (missingUsers.length > 0) {
        errors.rejectValue("studentsState", "smallGroup.attendees.missing", Array(missingUsers), "")
      }
    }*/

    studentsState.asScala.foreach { case (studentId, state) =>
      errors.pushNestedPath(s"studentsState[$studentId]")

      if (isFutureEvent && !(state == null || state == SmallGroupAttendanceState.MissedAuthorised || state == SmallGroupAttendanceState.NotRecorded)) {
        errors.rejectValue("", "smallGroup.attendance.beforeEvent")
      }

      errors.popNestedPath()
    }
  }

}

trait RecordAttendanceCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: RecordAttendanceState =>
  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.SmallGroupEvents.Register, mandatory(event))
  }
}

trait RecordAttendanceState {
  val event: SmallGroupEvent
  val week: Int
  val user: CurrentUser

  def studentsState: JMap[UniversityId, SmallGroupAttendanceState]

  def members: Seq[MemberOrUser]
}

trait SmallGroupEventInFutureCheck {
  self: RecordAttendanceState =>

  // TAB-3791
  private val StartTimeOffset = 15

  lazy val isFutureEvent: Boolean = {
    // Get the actual start date of the event in this week
    event.startDateTimeForWeek(week).exists(_.minusMinutes(StartTimeOffset).isAfter(LocalDateTime.now))
  }
}

trait RecordAttendanceDescription extends Describable[RecordAttendanceResult] {
  self: RecordAttendanceState =>

  override lazy val eventName = "RecordAttendance"

  def describe(d: Description): Unit = {
    d.smallGroupEvent(event)
     .property("week", week)
  }

  override def describeResult(d: Description, result: RecordAttendanceResult): Unit = {
    d.smallGroupAttendanceState(result.attendance, result.deletions)
     .studentIds(result.attendance.map(_.universityId) ++ result.deletions)
  }
}

trait RecordAttendanceNotificationCompletion extends CompletesNotifications[RecordAttendanceResult] {

  self: RecordAttendanceState with NotificationHandling =>

  def notificationsToComplete(commandResult: RecordAttendanceResult): CompletesNotificationsResult = {
    val event = commandResult.occurrence.event
    val attendanceIds = commandResult.occurrence.attendance.asScala.map(_.universityId)
    if (event.group.students.isEmpty || event.group.students.users.map(_.getWarwickId).forall(attendanceIds.contains)) {
      CompletesNotificationsResult(
        notificationService.findActionRequiredNotificationsByEntityAndType[SmallGroupEventAttendanceReminderNotification](commandResult.occurrence),
        user.apparentUser
      )
    } else {
      EmptyCompletesNotificationsResult
    }
  }
}
