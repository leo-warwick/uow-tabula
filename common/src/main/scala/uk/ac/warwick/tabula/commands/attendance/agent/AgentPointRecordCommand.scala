package uk.ac.warwick.tabula.commands.attendance.agent

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.attendance.GroupsPoints
import uk.ac.warwick.tabula.commands.attendance.view.{GroupedPointRecordValidation, MissedAttendanceMonitoringCheckpointsNotifications}
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model.{Member, StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.jdk.CollectionConverters._

object AgentPointRecordCommand {
  def apply(
    relationshipType: StudentRelationshipType,
    academicYear: AcademicYear,
    templatePoint: AttendanceMonitoringPoint,
    user: CurrentUser,
    member: Member
  ) = new AgentPointRecordCommandInternal(relationshipType, academicYear, templatePoint, user, member)
    with AutowiringRelationshipServiceComponent
    with AutowiringAttendanceMonitoringServiceComponent
    with AutowiringSecurityServiceComponent
    with ComposableCommand[(Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])]
    with AgentPointRecordValidation
    with AgentPointRecordDescription
    with AgentPointRecordPermissions
    with AgentPointRecordCommandState
    with PopulateAgentPointRecordCommand
    with MissedAttendanceMonitoringCheckpointsNotifications
}


class AgentPointRecordCommandInternal(
  val relationshipType: StudentRelationshipType,
  val academicYear: AcademicYear,
  val templatePoint: AttendanceMonitoringPoint,
  val user: CurrentUser,
  val member: Member
) extends CommandInternal[(Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])] {

  self: AgentPointRecordCommandState with AttendanceMonitoringServiceComponent with SecurityServiceComponent =>

  override def applyInternal(): (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal]) = {
    checkpointMap.asScala
      .filter { case (student, _) => securityService.can(user, Permissions.MonitoringPoints.Record, student) }
      .map { case (student, pointMap) => attendanceMonitoringService.setAttendance(student, pointMap.asScala.toMap, user) }
      .foldLeft((Seq[AttendanceMonitoringCheckpoint](), Seq[AttendanceMonitoringCheckpointTotal]())) {
        case ((leftCheckpoints, leftTotals), (rightCheckpoints, rightTotals)) => (leftCheckpoints ++ rightCheckpoints, leftTotals ++ rightTotals)
      }
  }

}

trait PopulateAgentPointRecordCommand extends PopulateOnForm {

  self: AgentPointRecordCommandState with SecurityServiceComponent =>

  override def populate(): Unit = {
    checkpointMap = studentPointMap.map {
      case (student, _) if !securityService.can(user, Permissions.MonitoringPoints.Record, student) => (student, Map.empty[AttendanceMonitoringPoint, AttendanceState].asJava)
      case (student, points) =>
        student -> points.map { point =>
          point -> studentPointCheckpointMap.get(student).map { pointMap =>
            pointMap.get(point).map(_.state).orNull
          }.orNull
        }.toMap.asJava
    }.asJava
  }
}

trait AgentPointRecordValidation extends SelfValidating with GroupedPointRecordValidation {

  self: AgentPointRecordCommandState with AttendanceMonitoringServiceComponent with SecurityServiceComponent =>

  override def validate(errors: Errors): Unit = {
    validateGroupedPoint(
      errors,
      templatePoint,
      checkpointMap.asScala.view.mapValues(_.asScala.toMap).toMap,
      studentPointCheckpointMap.view.mapValues(_.view.mapValues(_.state).toMap).toMap,
      user
    )
  }

}

trait AgentPointRecordPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: AgentPointRecordCommandState with RelationshipServiceComponent =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Profiles.StudentRelationship.Read(mandatory(relationshipType)), member)
    p.PermissionCheckAny(
      relationshipService.listCurrentStudentRelationshipsWithMember(relationshipType, member)
        .flatMap(_.studentMember)
        .distinct
        .map(member => CheckablePermission(Permissions.MonitoringPoints.Record, member))
    )
  }

}

trait AgentPointRecordDescription extends Describable[(Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])] {

  self: AgentPointRecordCommandState =>

  override lazy val eventName = "AgentPointRecord"

  override def describe(d: Description): Unit =
    d.attendanceMonitoringCheckpoints(checkpointMap.asScala.toMap.view.mapValues(_.asScala.toMap).toMap, verbose = true)
}

trait AgentPointRecordCommandState extends GroupsPoints {

  self: AttendanceMonitoringServiceComponent with RelationshipServiceComponent =>

  def relationshipType: StudentRelationshipType

  def academicYear: AcademicYear

  def templatePoint: AttendanceMonitoringPoint

  def user: CurrentUser

  def member: Member

  lazy val students: Seq[StudentMember] = relationshipService.listCurrentStudentRelationshipsWithMember(relationshipType, member).flatMap(_.studentMember).distinct

  lazy val studentPointMap: Map[StudentMember, Seq[AttendanceMonitoringPoint]] = {
    students.map { student =>
      student -> attendanceMonitoringService.listStudentsPoints(student, None, academicYear)
    }.toMap.view.mapValues(points => points.filter(p => {
      p.name.toLowerCase == templatePoint.name.toLowerCase &&
        templatePoint.scheme.pointStyle == p.scheme.pointStyle && {
        templatePoint.scheme.pointStyle match {
          case AttendanceMonitoringPointStyle.Week =>
            p.startWeek == templatePoint.startWeek && p.endWeek == templatePoint.endWeek
          case AttendanceMonitoringPointStyle.Date =>
            p.startDate == templatePoint.startDate && p.endDate == templatePoint.endDate
        }
      }
    })).filter(_._2.nonEmpty).toMap
  }

  lazy val studentPointCheckpointMap: Map[StudentMember, Map[AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint]] =
    attendanceMonitoringService.getCheckpoints(studentPointMap.values.flatten.toSeq, students)

  lazy val attendanceNoteMap: Map[StudentMember, Map[AttendanceMonitoringPoint, AttendanceMonitoringNote]] =
    students.map(student => student -> attendanceMonitoringService.getAttendanceNoteMap(student)).toMap

  lazy val hasReportedMap: Map[StudentMember, Boolean] =
    students.map(student =>
      student -> {
        val nonReportedTerms = attendanceMonitoringService.findNonReportedTerms(Seq(student), academicYear)
        !nonReportedTerms.contains(AcademicYear.forDate(templatePoint.startDate).termOrVacationForDate(templatePoint.startDate).periodType.toString)
      }
    ).toMap

  // Bind variables
  var checkpointMap: JMap[StudentMember, JMap[AttendanceMonitoringPoint, AttendanceState]] =
    LazyMaps.create { _: StudentMember => JHashMap(): JMap[AttendanceMonitoringPoint, AttendanceState] }.asJava
}
