package uk.ac.warwick.tabula.commands.coursework.assignments

import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.coursework.assignments.MarkerAssignmentsSummaryCommand.Result
import uk.ac.warwick.tabula.data.model.MarkingState.MarkingCompleted
import uk.ac.warwick.tabula.data.model.{Assignment, MarkingWorkflow, Member}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.services.{AssessmentServiceComponent, AutowiringAssessmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

object MarkerAssignmentsSummaryCommand {

	case class AssignmentInfo(
		assignment: Assignment,
		feedbackDeadline: DateTime,
		status: String
	)

	case class Result(
		todo: Seq[AssignmentInfo],
		doing: Seq[AssignmentInfo],
		done: Seq[AssignmentInfo]
	)
	
	def apply(marker: Member) =
		new MarkerAssignmentsSummaryCommandInternal(marker)
			with AutowiringAssessmentServiceComponent
			with ComposableCommand[MarkerAssignmentsSummaryCommand.Result]
			with PubliclyVisiblePermissions
			with ReadOnly with Unaudited
}


class MarkerAssignmentsSummaryCommandInternal(marker: Member) extends CommandInternal[MarkerAssignmentsSummaryCommand.Result]
	with TaskBenchmarking {

	self: AssessmentServiceComponent =>

	override def applyInternal(): Result = {
		val markerUser = MemberOrUser(marker).asUser
		val allAssignments = benchmarkTask("allAssignments") { assessmentService.getAssignmentWhereMarker(markerUser) }

		val parsedAssignments: Seq[MarkerAssignmentsSummaryCommand.Result] = benchmarkTask("parsedAssignments") { allAssignments.map { assignment =>
			val markerStudents = assignment.markingWorkflow.getMarkersStudents(assignment, markerUser)
			val markerFeedbacks = markerStudents.flatMap(student => assignment.getAllMarkerFeedbacks(student.getUserId, markerUser))

			if (markerFeedbacks.map(_.feedback).forall(_.released)) {
				MarkerAssignmentsSummaryCommand.Result(
					Nil,
					Nil,
					Seq(MarkerAssignmentsSummaryCommand.AssignmentInfo(
						assignment,
						assignment.feedbackDeadline.getOrElse(new LocalDate().plusYears(500)).toDateTimeAtStartOfDay,
						""
					))
				)
			} else if (markerFeedbacks.exists(_.state != MarkingCompleted)) {
				MarkerAssignmentsSummaryCommand.Result(
					Seq(MarkerAssignmentsSummaryCommand.AssignmentInfo(
						assignment,
						assignment.feedbackDeadline.getOrElse(new LocalDate().plusYears(500)).toDateTimeAtStartOfDay,
						""
					)),
					Nil,
					Nil
				)
			} else {
				val position = markerFeedbacks.headOption.map(_.getFeedbackPosition)
				val status = position.map(p => assignment.markingWorkflow.getRoleNameForNextPosition(p).toLowerCase).getOrElse(MarkingWorkflow.adminRole)
				MarkerAssignmentsSummaryCommand.Result(
					Nil,
					Seq(MarkerAssignmentsSummaryCommand.AssignmentInfo(
						assignment,
						assignment.feedbackDeadline.getOrElse(new LocalDate().plusYears(500)).toDateTimeAtStartOfDay,
						s"Sent to $status"
					)),
					Nil
				)
			}
		}}

		MarkerAssignmentsSummaryCommand.Result(
			todo = parsedAssignments.flatMap(_.todo).sortBy(_.feedbackDeadline),
			doing = parsedAssignments.flatMap(_.doing).sortBy(_.feedbackDeadline),
			done = parsedAssignments.flatMap(_.done).sortBy(_.feedbackDeadline).reverse
		)
	}

}
