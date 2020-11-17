package uk.ac.warwick.tabula.commands.reports.smallgroups

import org.joda.time.LocalDate
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.groups.SmallGroupAttendanceState
import uk.ac.warwick.tabula.commands.reports.smallgroups.AllSmallGroupEventAttendanceReportCommand.Result
import uk.ac.warwick.tabula.commands.reports.{ReportCommandRequest, ReportCommandRequestValidation, ReportCommandState, ReportPermissions}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}

import scala.jdk.CollectionConverters._

case class AllSmallGroupEventAttendanceReportResult(
  eventWeek: SmallGroupEventWeek,
  recorded: Int,
  unrecorded: Int,
  earliestRecordedAttendance: Option[LocalDate],
  latestRecordedAttendance: Option[LocalDate]
)

object AllSmallGroupEventAttendanceReportCommand {
  type Result = Seq[AllSmallGroupEventAttendanceReportResult]
  type Command = Appliable[Result] with ReportCommandRequest with ReportCommandRequestValidation

  def apply(department: Department, academicYear: AcademicYear): Command =
    new AllSmallGroupEventAttendanceReportCommand(department, academicYear)
      with ComposableCommand[Result]
      with ReportCommandRequest
      with ReportPermissions
      with ReportCommandRequestValidation
      with Unaudited with ReadOnly
      with AutowiringSmallGroupServiceComponent
}

abstract class AllSmallGroupEventAttendanceReportCommand(val department: Department, val academicYear: AcademicYear)
  extends CommandInternal[Result]
    with ReportCommandState {
  self: ReportCommandRequest
    with SmallGroupServiceComponent =>

  override def applyInternal(): Result = {
    def hasScheduledEventMatchingFilter(set: SmallGroupSet): Boolean =
      set.groups.asScala.exists { group =>
        group.events.filterNot(_.isUnscheduled).exists { event =>
          event.allWeeks.exists { week =>
            event.dateForWeek(week).exists { eventDate =>
              !eventDate.isBefore(startDate) && !eventDate.isAfter(endDate)
            }
          }
        }
      }

    val sets =
      smallGroupService.getAllSmallGroupSets(department)
        .filter(_.academicYear == academicYear)
        .filter(_.collectAttendance)
        .filter(hasScheduledEventMatchingFilter)

    // Can't guarantee that all the occurrences will exist for each event,
    // so generate case classes to represent each occurrence (a combination of event and week)
    val eventWeeks: Seq[SmallGroupEventWeek] =
      sets.flatMap(_.groups.asScala.flatMap(_.events).filter(!_.isUnscheduled).flatMap { sge =>
        sge.allWeeks.map { week =>
          SmallGroupEventWeek(
            id = s"${sge.id}-$week",
            event = sge,
            week = week,
            date = sge.dateForWeek(week).get,
            late = sge.dateForWeek(week).exists(_.isBefore(LocalDate.now()))
          )
        }
      }).filter { sgew =>
        !sgew.date.isBefore(startDate) && !sgew.date.isAfter(endDate)
      }.sortBy(sgew => (sgew.date, sgew.event.group.groupSet.module, sgew.event.group.groupSet, sgew.event.group, sgew.event))

    val sgewAttendanceMap =
      sets.flatMap(_.groups.asScala).flatMap(smallGroupService.findAttendanceByGroup).flatMap { occurrence =>
        // Ignore any occurrences that aren't in the eventWeeks
        eventWeeks.find(sgew => sgew.event == occurrence.event && sgew.week == occurrence.week)
          .map(sgew => sgew -> occurrence.attendance.asScala)
      }.toMap

    eventWeeks.map { sgew =>
      val attendance = sgewAttendanceMap.get(sgew)

      val attendedStates: Set[SmallGroupAttendanceState] = Set(
        SmallGroupAttendanceState.Attended,
        SmallGroupAttendanceState.AttendedRemotely,
        SmallGroupAttendanceState.MissedAuthorised,
        SmallGroupAttendanceState.MissedUnauthorised
      )

      AllSmallGroupEventAttendanceReportResult(
        eventWeek = sgew,
        recorded = attendance.map(_.count { sgea =>
          val state = SmallGroupAttendanceState.from(Some(sgea))
          attendedStates.contains(state)
        }).getOrElse(0),
        unrecorded = {
          val studentsCount = sgew.event.group.students.size
          attendance.map { a =>
            val explicitlyUnrecorded = a.count { sgea =>
              val state = SmallGroupAttendanceState.from(Some(sgea))
              state == SmallGroupAttendanceState.NotRecorded
            }

            val noRecord = sgew.event.group.students.users.count { student =>
              !a.exists(_.universityId == student.getWarwickId)
            }

            explicitlyUnrecorded + noRecord
          }.getOrElse(studentsCount)
        },
        earliestRecordedAttendance = attendance.flatMap(_.map(_.updatedDate.toLocalDate).minOption),
        latestRecordedAttendance = attendance.flatMap(_.map(_.updatedDate.toLocalDate).maxOption)
      )
    }
  }
}
