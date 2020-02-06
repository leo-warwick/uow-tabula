package uk.ac.warwick.tabula.commands.groups

import org.joda.time.{DateTime, LocalDateTime}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.groups.ListStudentGroupAttendanceCommand._
import uk.ac.warwick.tabula.commands.groups.ViewSmallGroupAttendanceCommand._
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.groups.SmallGroupFormat.Example
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicPeriod, AcademicYear}
import uk.ac.warwick.userlookup.User

import scala.collection.immutable.SortedMap

case class StudentGroupAttendance(
  termWeeks: SortedMap[AcademicPeriod, WeekRange],
  attendance: ListStudentGroupAttendanceCommand.PerTermAttendance,
  notes: Map[EventInstance, SmallGroupEventAttendanceNote],
  missedCount: Int,
  missedCountByTerm: Map[AcademicPeriod, Int],
  groupTitle: String,
  hasGroups: Boolean
)

object ListStudentGroupAttendanceCommand {
  type PerInstanceAttendance = SortedMap[EventInstance, (SmallGroupAttendanceState, Option[SmallGroupEventAttendance])]
  type PerWeekAttendance = SortedMap[SmallGroupEventOccurrence.WeekNumber, PerInstanceAttendance]
  type PerGroupAttendance = SortedMap[SmallGroup, PerWeekAttendance]
  type PerTermAttendance = SortedMap[AcademicPeriod, PerGroupAttendance]

  type Command = Appliable[StudentGroupAttendance] with PermissionsChecking

  def apply(member: Member, academicYear: AcademicYear): Command =
    new ListStudentGroupAttendanceCommandInternal(member, academicYear)
      with ComposableCommand[StudentGroupAttendance]
      with ListStudentGroupAttendanceCommandPermissions
      with AutowiringSmallGroupServiceComponent
      with ReadOnly with Unaudited
}

class ListStudentGroupAttendanceCommandInternal(val member: Member, val academicYear: AcademicYear)
  extends CommandInternal[StudentGroupAttendance]
    with ListStudentGroupAttendanceCommandState with TaskBenchmarking {
  self: SmallGroupServiceComponent =>

  implicit val defaultOrderingForGroup: Ordering[SmallGroup] = Ordering.by { group: SmallGroup => (group.groupSet.module.code, group.groupSet.name, group.name, group.id) }
  implicit val defaultOrderingForDateTime: Ordering[DateTime] = Ordering.by[DateTime, Long](_.getMillis)
  implicit val defaultOrderingForTerm: Ordering[AcademicPeriod] = Ordering.ordered[AcademicPeriod]

  def applyInternal(): StudentGroupAttendance = {
    val user = member.asSsoUser

    val memberGroups = smallGroupService.findSmallGroupsByStudent(user)

    val attendanceRecordedGroups = smallGroupService.findSmallGroupsWithAttendanceRecorded(user.getWarwickId)

    val groups = (memberGroups ++ attendanceRecordedGroups).distinct.filter { group =>
      !group.groupSet.deleted &&
        group.groupSet.showAttendanceReports &&
        group.groupSet.academicYear == academicYear &&
        group.events.nonEmpty
    }

    val allInstances = groups.flatMap { group => allEventInstances(group, smallGroupService.findAttendanceByGroup(group)) }

    def hasExpectedAttendanceForWeek(kv: (SmallGroupEventOccurrence.WeekNumber, PerInstanceAttendance)) = kv match {
      case (_, attendance) =>
        attendance.exists { case (_, (state, _)) => state != SmallGroupAttendanceState.NotExpected }
    }

    def hasExpectedAttendanceForGroup(kv: (SmallGroup, PerWeekAttendance)) = kv match {
      case (_, weekAttendance) =>
        weekAttendance.exists(hasExpectedAttendanceForWeek)
    }

    val attendance = groupByTerm(allInstances).view.mapValues { instances =>
      val groups = SortedMap(instances.groupBy { case ((event, _), _) => event.group }.toSeq: _*)
      groups.view.filterKeys { smallGroup => smallGroup.groupSet.visibleToStudents }.mapValues { instances =>
        SortedMap(instances.groupBy { case ((_, week), _) => week }.toSeq: _*).view.mapValues { instances =>
          attendanceForStudent(instances, isLate(user))(user)
        }.to(SortedMap)
      }.filter(hasExpectedAttendanceForGroup).to(SortedMap)
    }.filterNot { case (_, attendance) => attendance.isEmpty }.to(SortedMap)

    val missedCountByTerm = attendance.view.mapValues { groups =>
      val count = groups.map { case (_, attendanceByInstance) =>
        attendanceByInstance.values.flatMap(_.values.map(_._1)).count(_ == SmallGroupAttendanceState.MissedUnauthorised)
      }

      count.sum
    }.toMap

    val termWeeks = SortedMap(attendance.keySet.map { term =>
      term -> WeekRange(term.firstWeek.weekNumber, term.lastWeek.weekNumber)
    }.toSeq: _*)

    val attendanceNotes = benchmarkTask("Get attendance notes") {
      smallGroupService.findAttendanceNotes(
        Seq(user.getWarwickId),
        allInstances.flatMap { case (_, occurenceOption) => occurenceOption }
      ).groupBy(n => (n.occurrence.event, n.occurrence.week)).view.mapValues(_.head).toMap
    }

    StudentGroupAttendance(
      termWeeks,
      attendance,
      attendanceNotes,
      missedCountByTerm.foldLeft(0) { (acc, missedByTerm) => acc + missedByTerm._2 },
      missedCountByTerm,
      groupTitle(attendance),
      attendance.values.nonEmpty
    )
  }

  def groupByTerm(
    instances: Seq[(EventInstance, Option[SmallGroupEventOccurrence])]
  ): SortedMap[AcademicPeriod, Seq[(EventInstance, Option[SmallGroupEventOccurrence])]] = {
    lazy val weeksForYear = academicYear.weeks

    SortedMap(instances.groupBy { case ((_, week), _) => weeksForYear(week).period }.toSeq: _*)
  }

  private def isLate(user: User)(instance: EventInstance): Boolean = instance match {
    case (event, week: SmallGroupEventOccurrence.WeekNumber) =>
      // Can't be late if the student is no longer in that group
      event.group.students.includesUser(user) &&
        // Get the actual end date of the event in this week
        event.endDateTimeForWeek(week).exists(_.isBefore(LocalDateTime.now))
  }

  private def groupTitle(attendance: PerTermAttendance): String = {
    val title = {
      val smallGroupSets = attendance.values.toSeq.flatMap(_.keys.map(_.groupSet))

      val formats = smallGroupSets.map(_.format.description).distinct
      val pluralisedFormats = formats.map {
        case s: String if s == Example.description => s + "es"
        case s: String => s + "s"
        case _ =>
      }
      pluralisedFormats.mkString(", ")
    }
    title
  }

}

trait ListStudentGroupAttendanceCommandState {
  def member: Member

  def academicYear: AcademicYear
}

trait ListStudentGroupAttendanceCommandPermissions extends RequiresPermissionsChecking {
  self: ListStudentGroupAttendanceCommandState =>
  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Profiles.Read.SmallGroups, member)
    p.PermissionCheck(Permissions.SmallGroupEvents.ViewRegister, member)
  }
}
