package uk.ac.warwick.tabula.commands.attendance.check

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.groups.SmallGroupAttendanceState
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEventAttendance, SmallGroupEventOccurrence}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringEventAttendanceServiceComponent, AutowiringAttendanceMonitoringEventAttendanceServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._

case class CheckpointResult(
  attendedMonitoringPointStudentList: Seq[StudentMember],
  missedUnauthorisedMonitoringPointStudentList: Seq[StudentMember],
  missedAuthorisedMonitoringPointStudentList: Seq[StudentMember]
)

object CheckEventAttendanceCheckpointsCommand {
  def apply(occurrence: SmallGroupEventOccurrence) =
    new CheckEventAttendanceCheckpointsCommandInternal(occurrence)
      with ComposableCommand[CheckpointResult]
      with AutowiringAttendanceMonitoringEventAttendanceServiceComponent
      with CheckEventAttendanceCheckpointsPermissions
      with CheckEventAttendanceCheckpointsCommandState
      with ReadOnly with Unaudited
}


class CheckEventAttendanceCheckpointsCommandInternal(val occurrence: SmallGroupEventOccurrence)
  extends CommandInternal[CheckpointResult] {

  self: CheckEventAttendanceCheckpointsCommandState with AttendanceMonitoringEventAttendanceServiceComponent =>

  override def applyInternal(): CheckpointResult = {
    val attendanceList = attendances.asScala.map { case (universityId, state) =>
      val attendance = new SmallGroupEventAttendance
      attendance.occurrence = occurrence
      attendance.state = SmallGroupAttendanceState.to(state)
      attendance.universityId = universityId
      attendance
    }.toSeq
    val studentListWithCheckpoints = attendanceMonitoringEventAttendanceService.getCheckpoints(attendanceList).map(a => a.student).distinct
    if (occurrence.event.group.groupSet.module.adminDepartment.autoMarkMissedMonitoringPoints) {
      val (unauthorisedCheckpoints, authorisedCheckpoints) =
        attendanceMonitoringEventAttendanceService.getMissedCheckpoints(attendanceList)
          .partition { case (checkpoint, _) => checkpoint.state == AttendanceState.MissedUnauthorised }
      CheckpointResult(
        studentListWithCheckpoints,
        unauthorisedCheckpoints.map { case (a, _) => a.student }.distinct,
        authorisedCheckpoints.map { case (a, _) => a.student }.distinct
      )
    } else {
      CheckpointResult(studentListWithCheckpoints, Seq(), Seq())
    }
  }

}

trait CheckEventAttendanceCheckpointsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: CheckEventAttendanceCheckpointsCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.SmallGroupEvents.Register, occurrence)
  }

}

trait CheckEventAttendanceCheckpointsCommandState {
  def occurrence: SmallGroupEventOccurrence

  var attendances: JMap[String, SmallGroupAttendanceState] = JHashMap()
}
