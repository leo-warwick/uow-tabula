package uk.ac.warwick.tabula.web.controllers.groups

import javax.validation.Valid
import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.groups._
import uk.ac.warwick.tabula.commands.{MemberOrUser, SelfValidating}
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.EventDeliveryMethod.{FaceToFaceOnly, OnlineOnly}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroupEventAttendance, SmallGroupEventOccurrence}
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.services.AutowiringSmallGroupServiceComponent
import uk.ac.warwick.tabula.services.elasticsearch.AutowiringAuditEventQueryServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.groups.RecordAttendanceController._
import uk.ac.warwick.userlookup.User

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, TimeoutException}
import scala.jdk.CollectionConverters._
import scala.util.Try

object RecordAttendanceController {
  case class RecordAttendanceHistoryChange(student: MemberOrUser, state: AttendanceState)

  case class RecordAttendanceHistory(
    recorded: DateTime,
    wasRecordedLate: Boolean, // Whether the recorded attendance was outside of the 24 hour period that allows it to be used as evidence
    user: User,
    changes: Option[Seq[RecordAttendanceHistoryChange]]
  )
}

@RequestMapping(Array("/groups/event/{event}/register"))
@Controller
class RecordAttendanceController extends GroupsController with AutowiringAuditEventQueryServiceComponent {

  validatesSelf[SelfValidating]

  @ModelAttribute("command")
  def command(@PathVariable event: SmallGroupEvent, @RequestParam week: Int, user: CurrentUser): RecordAttendanceCommand.Command =
    RecordAttendanceCommand(mandatory(event), week, user)

  @ModelAttribute("recordAttendanceHistory")
  def recordAttendanceHistory(@PathVariable event: SmallGroupEvent, @RequestParam week: Int, @ModelAttribute("command") command: RecordAttendanceCommand.Command): Seq[RecordAttendanceHistory] = {
    val events =
      Try(Await.result(auditEventQueryService.smallGroupEventAttendanceRegisterEvents(event, week), 15.seconds))
        .recover { case _: TimeoutException => Nil }
        .get

    if (events.isEmpty) Nil
    else {
      // Update the changes map so it only includes members and only ones that haven't changed
      var state: mutable.Map[MemberOrUser, AttendanceState] = mutable.Map(command.members.map(_ -> AttendanceState.NotRecorded): _*)

      events.map { auditEvent =>
        val changes = auditEvent.attendance.map { a =>

          val recorded: Map[MemberOrUser, AttendanceState] =
            a.flatMap { case (universityId, s) =>
              command.members.find(_.universityId == universityId).map { student =>
                student -> s
              }
            }

          // For events created before TAB-8131 was implemented we didn't log when something's NotRecorded - add those in so we know the transition
          val notRecorded: Map[MemberOrUser, AttendanceState] = if (!auditEvent.onlyIncludesChanges) {
            command.members.filterNot(recorded.contains)
              .map(_ -> AttendanceState.NotRecorded)
              .toMap
          } else Map()

          (recorded ++ notRecorded).filter { case (student, s) => !state.get(student).contains(s) }
            .toSeq
            .sortBy { case (student, _) => (student.lastName, student.firstName, student.universityId) }
        }

        changes.foreach(_.foreach { case (student, s) =>
          state += student -> s
        })

        RecordAttendanceHistory(
          recorded = auditEvent.dateTime,
          wasRecordedLate = event.endDateTimeForWeek(week).exists { endTime =>
            endTime.toLocalDate.isBefore(auditEvent.dateTime.toLocalDate)
          },
          user = auditEvent.user,
          changes = changes.map(_.map { case (student, s) => RecordAttendanceHistoryChange(student, s) })
        )
      }.reverse // Most recent first
    }
  }

  @RequestMapping
  def showForm(@ModelAttribute("command") command: RecordAttendanceCommand.Command): Mav = {
    command.populate()
    form(command)
  }

  def form(command: RecordAttendanceCommand.Command): Mav =
    Mav("groups/attendance/form",
      "command" -> command,
      "showAttendedRemotelyOption" -> (command.event.deliveryMethod != FaceToFaceOnly),
      "showAttendedInPersonOption" -> (command.event.deliveryMethod != OnlineOnly),
      "allCheckpointStates" -> SmallGroupAttendanceState.values,
      "eventInFuture" -> command.isFutureEvent,
      "returnTo" -> getReturnTo(Routes.tutor.mygroups))

  @PostMapping(params = Array("action=refresh"))
  def refresh(@ModelAttribute("command") command: RecordAttendanceCommand.Command): Mav = {
    command.addAdditionalStudent(command.members)
    command.doRemoveAdditionalStudent(command.members)

    form(command)
  }

  @PostMapping
  def submit(@Valid @ModelAttribute("command") command: RecordAttendanceCommand.Command, errors: Errors): Mav =
    if (errors.hasErrors) {
      form(command)
    } else {
      val result = command.apply()
      Redirect(Routes.tutor.mygroups, "updatedOccurrence" -> result.occurrence.id)
    }

}

@RequestMapping(Array("/groups/event/{event}/register/additional"))
@Controller
class RecordAttendanceAddAdditionalStudentController extends GroupsController with AutowiringSmallGroupServiceComponent {

  @RequestMapping
  def addAdditionalForm(@PathVariable event: SmallGroupEvent, @RequestParam week: Int, @RequestParam student: Member): Mav = {
    case class EventOccurrenceAndState(
      event: SmallGroupEvent,
      week: Int,
      occurrence: Option[SmallGroupEventOccurrence],
      attendance: Option[SmallGroupEventAttendance]
    )

    // Find any groups that the student SHOULD be attending in the same module and academic year
    val possibleReplacements: Seq[EventOccurrenceAndState] =
      smallGroupService.findSmallGroupsByStudent(student.asSsoUser)
        .filter { group => group.groupSet.module == event.group.groupSet.module && group.groupSet.academicYear == event.group.groupSet.academicYear }
        .filterNot(_ == event.group) // No self references!
        .flatMap { group =>
          val occurrences = smallGroupService.findAttendanceByGroup(group)

          group.events.filter(!_.isUnscheduled).flatMap { event =>
            val allWeeks = event.weekRanges.flatMap(_.toWeeks)
            allWeeks.map { week =>
              val occurrence = occurrences.find { o =>
                o.event == event && o.week == week
              }

              EventOccurrenceAndState(
                event,
                week,
                occurrence,
                occurrence.flatMap(_.attendance.asScala.find(_.universityId == student.universityId))
              )
            }
          }
        }
        .filterNot(_.attendance.exists(_.state == AttendanceState.Attended)) // Can't replace an attended instance

    Mav("groups/attendance/additional",
      "student" -> student,
      "possibleReplacements" -> possibleReplacements,
      "week" -> week
    ).noLayout()
  }

}
