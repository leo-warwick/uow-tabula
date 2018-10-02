package uk.ac.warwick.tabula.data.model.notifications.cm2

import javax.persistence.{DiscriminatorValue, Entity, Transient}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.services.coursework.{AutowiringCourseworkWorkflowServiceComponent, CourseworkWorkflowService, CourseworkWorkflowStage}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

object ReleaseToMarkerNotification {
	val templateLocation = "/WEB-INF/freemarker/emails/released_to_marker_notification.ftl"
}

@Entity
@DiscriminatorValue("CM2ReleaseToMarker")
class ReleaseToMarkerNotification
	extends NotificationWithTarget[MarkerFeedback, Assignment]
	with SingleRecipientNotification
	with UserIdRecipientNotification
	with AutowiringUserLookupComponent
	with Logging
	with AutowiringCourseworkWorkflowServiceComponent
	with AllCompletedActionRequiredNotification {

	@Transient
	override var courseworkWorkflowService: CourseworkWorkflowService = _

	def workflowVerb: String = items.asScala.headOption.map(_.entity.stage.verb).getOrElse(MarkingWorkflowStage.DefaultVerb)

	def verb = "released"
	def assignment: Assignment = target.entity

	def title: String = s"${assignment.module.code.toUpperCase}: ${assignment.name} has been released for marking"

	def submissionsCnt = if (assignment.cm2Assignment) {
		val markerStudents = 	assignment.cm2MarkerAllocations.filter(_.marker == recipient).flatMap(_.students.map(_.getUserId)).distinct
		markerStudents.count{ stu => assignment.submissions.asScala.exists( s => s.usercode == stu && s.submitted == true) }
	} else {
		assignment.getMarkersSubmissions(recipient).distinct.size
	}

	def allocatedStudents = if (assignment.cm2Assignment) {
		assignment.cm2MarkerAllocations.filter(_.marker == recipient).flatMap(_.students).distinct.size
	} else {
		assignment.markingWorkflow.getMarkersStudents(assignment, recipient).distinct.size
	}

	def allocatedAsFirstMarker = {


//		val workflow: Seq[CourseworkWorkflowStage] = courseworkWorkflowService
//			.getStagesFor(assignment)
//  		.map{
//				cased
//			}

		val markers: Seq[User] = assignment.cm2MarkerAllocations.map{allocation =>
			allocation.marker
		}




		val s: Seq[Assignment.MarkerAllocation] = assignment.cm2MarkerAllocations
			.filter(_.marker == recipient)
			.distinct
  			.map{student =>

					student.coursework.enhancedSubmission
						.flatMap { s => Option(s.submission) }
						.flatMap { _.firstMarker }
						.map { _.getWarwickId }
						.getOrElse("first marker")
				}

		???
	}

	def content = FreemarkerModel(ReleaseToMarkerNotification.templateLocation,
		if(assignment.collectSubmissions) {
			Map(
				"assignment" -> assignment,
				"numAllocated" -> allocatedStudents,
				"numAllocatedAsFirstMarker" -> ???,
				"numAllocatedAsFinalMarker" -> ???,
				"numReleasedFeedbacks" -> items.size,
				"numReleasedSubmissionsFeedbacks" -> submissionsCnt,
				"numReleasedNoSubmissionsFeedbacks" -> (allocatedStudents - submissionsCnt),
				"workflowVerb" -> workflowVerb
			)
		}else{
			Map(
				"assignment" -> assignment,
				"numReleasedFeedbacks" -> items.size,
				"workflowVerb" -> workflowVerb
			)
		}
	)

	def url: String = Routes.admin.assignment.markerFeedback(assignment, recipient)
	def urlTitle = s"${workflowVerb} the assignment '${assignment.module.code.toUpperCase} - ${assignment.name}'"

	priority = Warning

}

