package uk.ac.warwick.tabula.commands.cm2.markingworkflows

import uk.ac.warwick.tabula.data.model.{Assignment, Department}
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType.{DoubleBlindMarking, DoubleMarking, ModeratedMarking, SingleMarking}
import uk.ac.warwick.tabula.data.model.markingworkflow._
import uk.ac.warwick.tabula.services.CM2MarkingWorkflowServiceComponent
import uk.ac.warwick.userlookup.User


trait CreatesMarkingWorkflow {

	self: CM2MarkingWorkflowServiceComponent with ModifyMarkingWorkflowState =>

	def createAndSaveSingleUseWorkflow(assignment: Assignment): Unit = {
		val data = MarkingWorkflowData(
			department,
			s"${assignment.module.code.toUpperCase} ${assignment.name}",
			markersAUsers,
			markersBUsers,
			workflowType,
			Option(sampler)
		)
		val workflow = createWorkflow(data)
		workflow.isReusable = false
		workflow.academicYear = assignment.academicYear
		cm2MarkingWorkflowService.save(workflow)
		assignment.cm2MarkingWorkflow = workflow
	}

	case class  MarkingWorkflowData (
		department: Department,
		workflowName: String,
		markersAUsers: Seq[User],
		markersBUsers: Seq[User],
		workflowType: MarkingWorkflowType,
		moderationSampler: Option[ModerationSampler]
	)

	def createWorkflow(data: MarkingWorkflowData) : CM2MarkingWorkflow = {
		data.workflowType match {
			case DoubleMarking => DoubleWorkflow(data.workflowName, data.department, data.markersAUsers, data.markersBUsers)
			case SingleMarking => SingleMarkerWorkflow(data.workflowName, data.department, data.markersAUsers)
			case DoubleBlindMarking => DoubleBlindWorkflow(data.workflowName, data.department, data.markersAUsers, data.markersBUsers)
			case ModeratedMarking =>
				val sampler = data.moderationSampler.getOrElse(ModerationSampler.Moderator)
				if (sampler == ModerationSampler.Admin) SelectedModeratedWorkflow(data.workflowName, data.department, sampler, data.markersAUsers, data.markersBUsers)
				else ModeratedWorkflow(data.workflowName, data.department, sampler, data.markersAUsers, data.markersBUsers)
			case _ => throw new UnsupportedOperationException(data.workflowType + " not specified")
		}
	}

}
