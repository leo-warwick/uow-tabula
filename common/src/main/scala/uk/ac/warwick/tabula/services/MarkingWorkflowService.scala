package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{Exam, MarkingWorkflow, Assignment}
import uk.ac.warwick.tabula.data.AutowiringMarkingWorkflowDaoComponent
import uk.ac.warwick.tabula.data.MarkingWorkflowDaoComponent

trait MarkingWorkflowServiceComponent {
	def markingWorkflowService: MarkingWorkflowService
}

trait AutowiringMarkingWorkflowServiceComponent extends MarkingWorkflowServiceComponent {
	var markingWorkflowService: MarkingWorkflowService = Wire[MarkingWorkflowService]
}

trait MarkingWorkflowService {
	def save(markingWorkflow: MarkingWorkflow): Unit

	/** All assignments using this marking workflow. */
	def getAssignmentsUsingMarkingWorkflow(markingWorkflow: MarkingWorkflow): Seq[Assignment]

	/** All exams using this marking workflow. */
	def getExamsUsingMarkingWorkflow(markingWorkflow: MarkingWorkflow): Seq[Exam]
}

abstract class AbstractMarkingWorkflowService extends MarkingWorkflowService {
	self: MarkingWorkflowDaoComponent
		with Logging =>

	def save(markingWorkflow: MarkingWorkflow): Unit = markingWorkflowDao.save(markingWorkflow)

	def getAssignmentsUsingMarkingWorkflow(markingWorkflow: MarkingWorkflow): Seq[Assignment] =
		markingWorkflowDao.getAssignmentsUsingMarkingWorkflow(markingWorkflow)

	def getExamsUsingMarkingWorkflow(markingWorkflow: MarkingWorkflow): Seq[Exam] =
		markingWorkflowDao.getExamsUsingMarkingWorkflow(markingWorkflow)
}

@Service("markingWorkflowService")
class MarkingWorkflowServiceImpl
	extends AbstractMarkingWorkflowService
		with AutowiringMarkingWorkflowDaoComponent
		with Logging