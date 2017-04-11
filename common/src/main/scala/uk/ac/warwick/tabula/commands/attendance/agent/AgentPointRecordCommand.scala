package uk.ac.warwick.tabula.commands.attendance.agent

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.attendance.GroupsPoints
import uk.ac.warwick.tabula.commands.attendance.view.{GroupedPointRecordValidation, MissedAttendanceMonitoringCheckpointsNotifications}
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model.{Member, StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.collection.JavaConverters._

object AgentPointRecordCommand {
	def apply(
		relationshipType: StudentRelationshipType,
		academicYear: AcademicYear,
		templatePoint: AttendanceMonitoringPoint,
		user: CurrentUser,
		member: Member
	) =	new AgentPointRecordCommandInternal(relationshipType, academicYear, templatePoint, user, member)
			with AutowiringRelationshipServiceComponent
			with AutowiringTermServiceComponent
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

	self: AgentPointRecordCommandState with AttendanceMonitoringServiceComponent =>

	override def applyInternal(): (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal]) = {
		checkpointMap.asScala.map { case(student, pointMap) =>
			attendanceMonitoringService.setAttendance(student, pointMap.asScala.toMap, user)
		}.toSeq.foldLeft(
			(Seq[AttendanceMonitoringCheckpoint](), Seq[AttendanceMonitoringCheckpointTotal]())
		){
			case ((leftCheckpoints, leftTotals), (rightCheckpoints, rightTotals)) => (leftCheckpoints ++ rightCheckpoints, leftTotals ++ rightTotals)
		}
	}

}

trait PopulateAgentPointRecordCommand extends PopulateOnForm {

	self: AgentPointRecordCommandState =>

	override def populate(): Unit = {
		checkpointMap = studentPointMap.map{case(student, points) =>
			student -> points.map{ point =>
				point -> studentPointCheckpointMap.get(student).map { pointMap =>
					pointMap.get(point).map(_.state).orNull
				}.orNull
			}.toMap.asJava
		}.asJava
	}
}

trait AgentPointRecordValidation extends SelfValidating with GroupedPointRecordValidation {

	self: AgentPointRecordCommandState with AttendanceMonitoringServiceComponent with TermServiceComponent with SecurityServiceComponent =>

	override def validate(errors: Errors) {
		validateGroupedPoint(
			errors,
			templatePoint,
			checkpointMap.asScala.mapValues(_.asScala.toMap).toMap,
			studentPointCheckpointMap.mapValues(_.mapValues(_.state)),
			user
		)
	}

}

trait AgentPointRecordPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: AgentPointRecordCommandState with RelationshipServiceComponent =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Read(mandatory(relationshipType)), member)
		p.PermissionCheckAll(
			Permissions.MonitoringPoints.Record,
			relationshipService.listCurrentStudentRelationshipsWithMember(relationshipType, member).flatMap(_.studentMember).distinct
		)
	}

}

trait AgentPointRecordDescription extends Describable[(Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])] {

	self: AgentPointRecordCommandState =>

	override lazy val eventName = "AgentPointRecord"

	override def describe(d: Description) {
		d.property("checkpoints", checkpointMap.asScala.map{ case (student, pointMap) =>
			student.universityId -> pointMap.asScala.map{ case(point, state) => point -> {
				if (state == null)
					"null"
				else
					state.dbValue
			}}
		})
	}
}

trait AgentPointRecordCommandState extends GroupsPoints {

	self: AttendanceMonitoringServiceComponent with RelationshipServiceComponent with TermServiceComponent =>

	def relationshipType: StudentRelationshipType
	def academicYear: AcademicYear
	def templatePoint: AttendanceMonitoringPoint
	def user: CurrentUser
	def member: Member

	lazy val students: Seq[StudentMember] = relationshipService.listCurrentStudentRelationshipsWithMember(relationshipType, member).flatMap(_.studentMember).distinct

	lazy val studentPointMap: Map[StudentMember, Seq[AttendanceMonitoringPoint]] = {
		students.map { student =>
			student -> attendanceMonitoringService.listStudentsPoints(student, None, academicYear)
		}.toMap.mapValues(points => points.filter(p => {
			p.name.toLowerCase == templatePoint.name.toLowerCase &&
				templatePoint.scheme.pointStyle == p.scheme.pointStyle && {
				templatePoint.scheme.pointStyle match {
					case AttendanceMonitoringPointStyle.Week =>
						p.startWeek == templatePoint.startWeek && p.endWeek == templatePoint.endWeek
					case AttendanceMonitoringPointStyle.Date =>
						p.startDate == templatePoint.startDate && p.endDate == templatePoint.endDate
				}
			}
		})).filter(_._2.nonEmpty)
	}

	lazy val studentPointCheckpointMap: Map[StudentMember, Map[AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint]] =
		attendanceMonitoringService.getCheckpoints(studentPointMap.values.flatten.toSeq, students)

	lazy val attendanceNoteMap: Map[StudentMember, Map[AttendanceMonitoringPoint, AttendanceMonitoringNote]] =
		students.map(student => student -> attendanceMonitoringService.getAttendanceNoteMap(student)).toMap

	lazy val hasReportedMap: Map[StudentMember, Boolean] =
		students.map(student =>
			student -> {
				val nonReportedTerms = attendanceMonitoringService.findNonReportedTerms(Seq(student), academicYear)
				!nonReportedTerms.contains(termService.getTermFromDateIncludingVacations(templatePoint.startDate.toDateTimeAtStartOfDay).getTermTypeAsString)
			}
		).toMap

	// Bind variables
	var checkpointMap: JMap[StudentMember, JMap[AttendanceMonitoringPoint, AttendanceState]] =
		LazyMaps.create{student: StudentMember => JHashMap(): JMap[AttendanceMonitoringPoint, AttendanceState] }.asJava
}
