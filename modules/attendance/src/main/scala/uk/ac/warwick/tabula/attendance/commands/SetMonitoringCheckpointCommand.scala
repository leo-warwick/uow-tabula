package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceState, MonitoringPointSet, MonitoringCheckpoint, MonitoringPoint}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.CurrentUser
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.LazyMaps
import org.hibernate.criterion.Order._
import org.hibernate.criterion.Order
import uk.ac.warwick.tabula.system.BindListener
import org.joda.time.DateTime

object SetMonitoringCheckpointCommand {
	def apply(department: Department, templateMonitoringPoint: MonitoringPoint, user: CurrentUser, routes: JList[Route]) =
		new SetMonitoringCheckpointCommand(department, templateMonitoringPoint, user, routes)
			with AutowiringProfileServiceComponent
			with AutowiringSecurityServicePermissionsAwareRoutes
			with SetMonitoringCheckpointCommandPermissions
			with ComposableCommand[Seq[MonitoringCheckpoint]]
			with SetMonitoringCheckpointCommandValidation
			with SetMonitoringPointDescription
			with SetAttendanceState
			with AutowiringMonitoringPointServiceComponent
			with AutowiringSecurityServiceComponent
			with AutowiringTermServiceComponent
}

abstract class SetMonitoringCheckpointCommand(val department: Department, val templateMonitoringPoint: MonitoringPoint, val user: CurrentUser, val routes: JList[Route])
	extends CommandInternal[Seq[MonitoringCheckpoint]] with Appliable[Seq[MonitoringCheckpoint]] with BindListener with PopulateOnForm {

	self: SetAttendanceState with MonitoringPointServiceComponent with ProfileServiceComponent =>

	def populate() {
		// Get students matching the filter
		val students = profileService.findAllStudentsByRestrictions(
			department = department,
			restrictions = buildRestrictions(),
			orders = buildOrders()
		)
		// Get monitoring points by student for the list of students matching the template point
		val studentPointMap = monitoringPointService.findSimilarPointsForMembers(templateMonitoringPoint, students)
		val allPoints = studentPointMap.values.flatten.toSeq
		val checkpoints = monitoringPointService.getCheckpointsByStudent(allPoints)
		// Map the checkpoint state to each point for each student
		studentsState = studentPointMap.map{ case (student, points) =>
			student -> points.map{ point =>
				point -> {
					val checkpointOption = checkpoints.find{
						case (s, checkpoint) => s == student && checkpoint.point == point
					}
					checkpointOption.map{case (_, checkpoint) => checkpoint.state}.getOrElse(null)
				}
			}.toMap.asJava
		}.toMap.asJava
	}

	def applyInternal(): Seq[MonitoringCheckpoint] = {
		studentsStateAsScala.flatMap{ case (student, pointMap) =>
			pointMap.flatMap{ case (point, state) =>
				if (state == null) {
					monitoringPointService.deleteCheckpoint(student, point)
					None
				} else {
					Option(monitoringPointService.saveOrUpdateCheckpoint(student, point, state, user))
				}
			}
		}.toSeq
	}

	def onBind(result: BindingResult) = {
		studentsStateAsScala = studentsState.asScala.map{case(student, pointMap) => student -> pointMap.asScala.toMap}.toMap
	}
}

trait SetMonitoringCheckpointCommandValidation extends SelfValidating {
	self: SetAttendanceState with SecurityServiceComponent with TermServiceComponent with MonitoringPointServiceComponent =>

	def validate(errors: Errors) {
		val academicYear = templateMonitoringPoint.pointSet.asInstanceOf[MonitoringPointSet].academicYear
		val currentAcademicWeek = termService.getAcademicWeekForAcademicYear(DateTime.now(), academicYear)
		studentsStateAsScala.foreach{ case(student, pointMap) => {
			val studentPointSet = monitoringPointService.getPointSetForStudent(student, academicYear)
			pointMap.foreach{ case(point, state) => {
				errors.pushNestedPath(s"studentsState[${student.universityId}][${point.id}]")
				val pointRoute = point.pointSet.asInstanceOf[MonitoringPointSet].route
				// Check point is valid for student
				if (!studentPointSet.exists(s => s.points.asScala.contains(point))) {
					errors.rejectValue("", "monitoringPoint.invalidStudent")
				// Check has permission for each point
				}	else if (!securityService.can(user, Permissions.MonitoringPoints.Record, pointRoute)) {
					errors.rejectValue("", "monitoringPoint.noRecordPermission")
				} else {
					// Check state change valid
					if (point.sentToAcademicOffice) {
						errors.rejectValue("", "monitoringCheckpoint.sentToAcademicOffice")
					}
					if (currentAcademicWeek < point.validFromWeek && !(state == null || state == AttendanceState.MissedAuthorised)) {
						errors.rejectValue("", "monitoringCheckpoint.beforeValidFromWeek")
					}
				}
				errors.popNestedPath()
			}}
		}}
	}

}

trait SetMonitoringCheckpointCommandPermissions extends RequiresPermissionsChecking with PermissionsChecking {
	self: SetAttendanceState =>

	def permissionsCheck(p: PermissionsChecking) {
		if (routesForPermission(user, Permissions.MonitoringPoints.View, department).size == department.routes.asScala.size)
			p.PermissionCheck(Permissions.MonitoringPoints.Record, department)
		else
			p.PermissionCheckAll(Permissions.MonitoringPoints.Record, routes.asScala)
	}
}


trait SetMonitoringPointDescription extends Describable[Seq[MonitoringCheckpoint]] {
	self: SetAttendanceState =>

	override lazy val eventName = "SetMonitoringCheckpoint"

	def describe(d: Description) {
		d.property("checkpoints", studentsStateAsScala.map{ case (student, pointMap) =>
			student.universityId -> pointMap.map{ case(point, state) => point -> {
				if (state == null)
					"null"
				else
					state.dbValue
			}}
		})
	}
}


trait SetAttendanceState extends FiltersStudents with PermissionsAwareRoutes {
	def templateMonitoringPoint: MonitoringPoint
	def department: Department
	def user: CurrentUser
	def routes: JList[Route]

	var studentsState: JMap[StudentMember, JMap[MonitoringPoint, AttendanceState]] =
		LazyMaps.create{student: StudentMember => JHashMap(): JMap[MonitoringPoint, AttendanceState] }.asJava
	var studentsStateAsScala: Map[StudentMember, Map[MonitoringPoint, AttendanceState]] = _

	var courseTypes: JList[CourseType] = JArrayList()
	var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var sprStatuses: JList[SitsStatus] = JArrayList()
	var modules: JList[Module] = JArrayList()

	// We don't actually allow any sorting, but these need to be defined
	val defaultOrder = Seq(asc("lastName"), asc("firstName")) // Don't allow this to be changed atm
	var sortOrder: JList[Order] = JArrayList()
}