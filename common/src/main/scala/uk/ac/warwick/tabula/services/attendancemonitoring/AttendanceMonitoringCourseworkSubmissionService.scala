package uk.ac.warwick.tabula.services.attendancemonitoring

import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model.{Assignment, StudentMember, Submission}
import uk.ac.warwick.tabula.services.{AssessmentServiceComponent, AutowiringAssessmentServiceComponent, AutowiringProfileServiceComponent, ProfileServiceComponent}

trait AttendanceMonitoringCourseworkSubmissionServiceComponent {
	def attendanceMonitoringCourseworkSubmissionService: AttendanceMonitoringCourseworkSubmissionService
}

trait AutowiringAttendanceMonitoringCourseworkSubmissionServiceComponent extends AttendanceMonitoringCourseworkSubmissionServiceComponent {
	val attendanceMonitoringCourseworkSubmissionService: AttendanceMonitoringCourseworkSubmissionService = Wire[AttendanceMonitoringCourseworkSubmissionService]
}

trait AttendanceMonitoringCourseworkSubmissionService {
	def getCheckpoints(submission: Submission): Seq[AttendanceMonitoringCheckpoint]
	def updateCheckpoints(submission: Submission): (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])
}

abstract class AbstractAttendanceMonitoringCourseworkSubmissionService extends AttendanceMonitoringCourseworkSubmissionService {

	self: ProfileServiceComponent with AttendanceMonitoringServiceComponent with AssessmentServiceComponent =>

	def getCheckpoints(submission: Submission): Seq[AttendanceMonitoringCheckpoint] = {

		val member = submission.universityId.flatMap(uid => profileService.getMemberByUniversityId(uid))
		member.flatMap{
			case studentMember: StudentMember =>
					val relevantPoints = getRelevantPoints(
						attendanceMonitoringService.listStudentsPoints(studentMember, None, submission.assignment.academicYear),
						submission,
						studentMember
					)
					val checkpoints = relevantPoints.filter(point => checkQuantity(point, submission, studentMember)).map(point => {
						val checkpoint = new AttendanceMonitoringCheckpoint
						checkpoint.autoCreated = true
						checkpoint.point = point
						checkpoint.attendanceMonitoringService = attendanceMonitoringService
						checkpoint.student = studentMember
						checkpoint.updatedBy = submission.usercode
						checkpoint.updatedDate = DateTime.now
						checkpoint.state = AttendanceState.Attended
						checkpoint
					})
					Some(checkpoints)
			case _ => None
		}.getOrElse(Seq())
	}

	def updateCheckpoints(submission: Submission): (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal]) = {
		getCheckpoints(submission).map(checkpoint => {
			attendanceMonitoringService.setAttendance(checkpoint.student, Map(checkpoint.point -> checkpoint.state), checkpoint.updatedBy, autocreated = true)
		}).foldLeft(
			(Seq[AttendanceMonitoringCheckpoint](), Seq[AttendanceMonitoringCheckpointTotal]())
		){
			case ((leftCheckpoints, leftTotals), (rightCheckpoints, rightTotals)) => (leftCheckpoints ++ rightCheckpoints, leftTotals ++ rightTotals)
		}
	}

	private def getRelevantPoints(points: Seq[AttendanceMonitoringPoint], submission: Submission, studentMember: StudentMember): Seq[AttendanceMonitoringPoint] = {
		points.filter(point =>
			// Is it the correct type
			point.pointType == AttendanceMonitoringPointType.AssignmentSubmission
				// Is the assignment's due date inside the point's weeks
				&& point.isDateValidForPoint(submission.assignment.closeDate.toLocalDate)
				// Is the submission on time or the submission time inside the point's weeks
				&& (!submission.isLate || point.isDateValidForPoint(submission.submittedDate.toLocalDate))
				// Is the submission's assignment or module valid
				&& isAssignmentOrModuleValidForPoint(point, submission.assignment)
				// Is there no existing checkpoint
				&& attendanceMonitoringService.getCheckpoints(Seq(point), Seq(studentMember)).isEmpty
				// The student hasn't been sent to SITS for this point
				&& !attendanceMonitoringService.studentAlreadyReportedThisTerm(studentMember, point)
		)
	}

	private def isAssignmentOrModuleValidForPoint(point: AttendanceMonitoringPoint, assignment: Assignment) = {
		point.assignmentSubmissionType == AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Any ||
			point.assignmentSubmissionType == AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Assignments && point.assignmentSubmissionAssignments.contains(assignment) ||
			point.assignmentSubmissionType == AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Modules && point.assignmentSubmissionModules.contains(assignment.module)

	}

	private def checkQuantity(point: AttendanceMonitoringPoint, submission: Submission, studentMember: StudentMember): Boolean = {
		if (point.assignmentSubmissionType == AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Assignments) {
			if (point.assignmentSubmissionIsDisjunction) {
				true
			} else {
				val submissions = assessmentService.getSubmissionsForAssignmentsBetweenDates(
					studentMember.userId,
					point.startDate.toDateTimeAtStartOfDay,
					point.endDate.plusDays(1).toDateTimeAtStartOfDay
				).filterNot(_.isLate).filterNot(s => s.assignment == submission.assignment) ++ Seq(submission)

				point.assignmentSubmissionAssignments.forall(a => submissions.exists(s => s.assignment == a))
			}
		} else {
			def allSubmissions = {
				assessmentService.getSubmissionsForAssignmentsBetweenDates(
					studentMember.userId,
					point.startDate.toDateTimeAtStartOfDay,
					point.endDate.plusDays(1).toDateTimeAtStartOfDay
				).filterNot(_.isLate).filterNot(
					s => s.assignment == submission.assignment
				) ++ Seq(submission)
			}
			if (point.assignmentSubmissionType == AttendanceMonitoringPoint.Settings.AssignmentSubmissionTypes.Modules) {
				if (point.assignmentSubmissionTypeModulesQuantity == 1) {
					true
				} else {
					val submissions = allSubmissions.filter(s => point.assignmentSubmissionModules.contains(s.assignment.module))
					submissions.size >= point.assignmentSubmissionTypeModulesQuantity
				}
			} else {
				if (point.assignmentSubmissionTypeAnyQuantity == 1) {
					true
				} else {
					allSubmissions.size >= point.assignmentSubmissionTypeAnyQuantity
				}
			}
		}
	}
}

@Service("attendanceMonitoringCourseworkSubmissionService")
class AttendanceMonitoringCourseworkSubmissionServiceImpl
	extends AbstractAttendanceMonitoringCourseworkSubmissionService
		with AutowiringAttendanceMonitoringServiceComponent
		with AutowiringProfileServiceComponent
		with AutowiringAssessmentServiceComponent
