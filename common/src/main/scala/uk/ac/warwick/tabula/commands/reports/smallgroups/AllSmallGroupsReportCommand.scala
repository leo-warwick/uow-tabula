package uk.ac.warwick.tabula.commands.reports.smallgroups

import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.reports.{ReportCommandRequest, ReportCommandRequestValidation, ReportCommandState, ReportPermissions}
import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentData
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, SmallGroup, SmallGroupEvent, SmallGroupSet}
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, AutowiringUserLookupComponent, SmallGroupServiceComponent, UserLookupComponent}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

object AllSmallGroupsReportCommand {
  type ResultType = AllSmallGroupsReportCommandResult
  type CommandType = Appliable[AllSmallGroupsReportCommandResult] with ReportCommandRequestValidation

  def apply(
    department: Department,
    academicYear: AcademicYear,
    filter: AllSmallGroupsReportCommandResult => AllSmallGroupsReportCommandResult
  ): CommandType =
    new AllSmallGroupsReportCommandInternal(department, academicYear, filter)
      with AutowiringSmallGroupServiceComponent
      with AutowiringAttendanceMonitoringServiceComponent
      with AutowiringUserLookupComponent
      with ComposableCommand[AllSmallGroupsReportCommandResult]
      with ReportPermissions
      with ReportCommandRequest
      with ReportCommandRequestValidation
      with AllSmallGroupsReportCommandState
      with ReadOnly with Unaudited
}

case class SmallGroupEventWeek(
  id: String, // These are needed so the object can be used as a key in the JSON
  event: SmallGroupEvent,
  week: Int,
  date: LocalDate,
  late: Boolean
)

case class AllSmallGroupsReportCommandResult(
  attendance: Map[User, Map[SmallGroupEventWeek, AttendanceState]],
  relevantEvents: Map[User, Set[SmallGroupEventWeek]],
  studentDatas: Seq[AttendanceMonitoringStudentData],
  eventWeeks: Seq[SmallGroupEventWeek],
  reportRangeStartDate: LocalDate,
  reportRangeEndDate: LocalDate
)

class AllSmallGroupsReportCommandInternal(
  val department: Department,
  val academicYear: AcademicYear,
  val filter: AllSmallGroupsReportCommandResult => AllSmallGroupsReportCommandResult
) extends CommandInternal[AllSmallGroupsReportCommandResult] with TaskBenchmarking {

  self: SmallGroupServiceComponent
    with AttendanceMonitoringServiceComponent
    with UserLookupComponent
    with ReportCommandRequest =>

  override def applyInternal(): AllSmallGroupsReportCommandResult = {
    val thisWeek =
      if (!academicYear.firstDay.isAfter(LocalDate.now) && !academicYear.lastDay.isBefore(LocalDate.now))
        academicYear.weekForDate(LocalDate.now).weekNumber
      else
        academicYear.weeks.keys.max + 1

    val thisDay = DateTime.now.getDayOfWeek
    val weeksForYear = academicYear.weeks

    def weekNumberToDate(weekNumber: Int, dayOfWeek: DayOfWeek) =
      weeksForYear(weekNumber).firstDay.withDayOfWeek(dayOfWeek.jodaDayOfWeek)

    def hasScheduledEventMatchingFilter(set: SmallGroupSet): Boolean =
      set.groups.asScala.exists { group =>
        group.events.filterNot(_.isUnscheduled).exists { event =>
          event.allWeeks.exists { week =>
            val eventDate = weekNumberToDate(week, event.day)
            !eventDate.isBefore(startDate) && !eventDate.isAfter(endDate)
          }
        }
      }

    val sets = benchmarkTask("sets") {
      smallGroupService.getAllSmallGroupSets(department).filter(_.academicYear == academicYear).filter(_.collectAttendance)
        .filter(hasScheduledEventMatchingFilter)
    }

    // Can't guarantee that all the occurrences will exist for each event,
    // so generate case classes to represent each occurrence (a combination of event and week)
    val eventWeeks: Seq[SmallGroupEventWeek] = benchmarkTask("eventWeeks") {
      sets.flatMap(_.groups.asScala.flatMap(_.events).filter(!_.isUnscheduled).flatMap(sge => {
        sge.allWeeks.map(week => SmallGroupEventWeek(s"${sge.id}-$week", sge, week, sge.dateForWeek(week).get, {
          week < thisWeek || week == thisWeek && sge.day.getAsInt < thisDay
        }))
      })).filter { sgew =>
        val eventDate = weekNumberToDate(sgew.week, sgew.event.day)
        (eventDate.isEqual(startDate) || eventDate.isAfter(startDate)) && (eventDate.isEqual(endDate) || eventDate.isBefore(endDate))
      }.sortBy(sgew => (sgew.week, sgew.event.day.getAsInt))
    }

    val sgewAttendanceMap = benchmarkTask("attendance") {
      sets.flatMap(_.groups.asScala).flatMap(smallGroupService.findAttendanceByGroup).flatMap(occurrence =>
        // Ignore any occurrences that aren't in the eventWeeks
        eventWeeks.find(sgew => sgew.event == occurrence.event && sgew.week == occurrence.week).map(sgew => sgew -> occurrence.attendance.asScala)
      ).toMap
    }

    val students: Seq[User] = sets.flatMap { set =>
      val membersOfSet = set.allStudents
      val groupMembersNotInSet = set.studentsNotInMembership

      // Get users who have subsequently been removed from groups
      val otherAttendanceRecordedUniversityIds =
        sgewAttendanceMap.values.flatten.map(_.universityId)
          .filterNot { universityId => membersOfSet.exists(_.getWarwickId == universityId) || groupMembersNotInSet.exists(_.getWarwickId == universityId) }
      val otherAttendanceRecordedUsers = userLookup.usersByWarwickUniIds(otherAttendanceRecordedUniversityIds.toSeq.distinct).values

      membersOfSet ++ groupMembersNotInSet ++ otherAttendanceRecordedUsers
    }.distinct.sortBy(s => (s.getLastName, s.getFirstName))

    val foundStudentDatas: Seq[AttendanceMonitoringStudentData] = attendanceMonitoringService.getAttendanceMonitoringDataForStudents(students.map(_.getWarwickId), academicYear)
    val notFoundStudentDatas: Seq[AttendanceMonitoringStudentData] =
      students.filterNot(u => foundStudentDatas.exists(_.universityId == u.getWarwickId))
        .map { user =>
          AttendanceMonitoringStudentData(
            firstName = user.getFirstName,
            lastName = user.getLastName,
            universityId = user.getWarwickId,
            userId = user.getUserId,
            scdBeginDate = null,
            scdEndDate = None,
            routeCode = null,
            routeName = null,
            yearOfStudy = null,
            sprCode = null,
            tier4Requirements = false, // TODO this is "Unknown"
            email = user.getEmail,
            tutorEmail = None
          )
        }

    val studentDatas: Seq[AttendanceMonitoringStudentData] = foundStudentDatas ++ notFoundStudentDatas

    val studentInGroup: Map[SmallGroup, Map[User, Boolean]] = benchmarkTask("studentInGroup") {
      sets.flatMap(_.groups.asScala).map(group => group -> students.map(student => student -> group.students.includesUser(student)).toMap).toMap
    }

    val studentAttendanceMap: Map[User, Map[SmallGroupEventWeek, AttendanceState]] = benchmarkTask("studentAttendanceMap") {
      students.map(student => student -> eventWeeks.map(sgew => sgew -> {
        sgewAttendanceMap.get(sgew).flatMap(attendance => {
          // There is some attendance recorded for this SGEW, so see if there is any for this student
          attendance.find(_.universityId == student.getWarwickId).map(_.state)
        }).getOrElse({
          // There is NO attendance recorded for this SGEW
          // No attendance for this student; should there be?
          if (studentInGroup(sgew.event.group)(student)) {
            AttendanceState.NotRecorded
          } else {
            null
          }
        })
      }).filter { case (_, state) => state != null }.toMap).toMap
    }

    val relevantEvents: Map[User, Set[SmallGroupEventWeek]] =
      studentAttendanceMap
        .view
        .mapValues(_.keySet)
        .toMap

    filter(AllSmallGroupsReportCommandResult(studentAttendanceMap, relevantEvents, studentDatas, eventWeeks,
      startDate, endDate))
  }
}

trait AllSmallGroupsReportCommandState extends ReportCommandState with ReportCommandRequest {
}
