package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.spring.Wire

trait MarkingWorkflowDaoComponent {
	def markingWorkflowDao: MarkingWorkflowDao
}

trait AutowiringMarkingWorkflowDaoComponent extends MarkingWorkflowDaoComponent {
	var markingWorkflowDao: MarkingWorkflowDao = Wire[MarkingWorkflowDao]
}

trait MarkingWorkflowDao {
	def save(markingWorkflow: MarkingWorkflow): Unit

	/** All assignments using this marking workflow. */
	def getAssignmentsUsingMarkingWorkflow(markingWorkflow: MarkingWorkflow): Seq[Assignment]

	/** All exams using this marking workflow. */
	def getExamsUsingMarkingWorkflow(markingWorkflow: MarkingWorkflow): Seq[Exam]

}

@Repository
class MarkingWorkflowDaoImpl extends MarkingWorkflowDao with Daoisms {

	def save(markingWorkflow: MarkingWorkflow): Unit = session.saveOrUpdate(markingWorkflow)

	def getAssignmentsUsingMarkingWorkflow(markingWorkflow: MarkingWorkflow): Seq[Assignment] =
		session.newCriteria[Assignment]
			.add(is("markingWorkflow", markingWorkflow))
			.add(is("deleted", false))
			.seq

	def getExamsUsingMarkingWorkflow(markingWorkflow: MarkingWorkflow): Seq[Exam] =
		session.newCriteria[Exam]
			.add(is("markingWorkflow", markingWorkflow))
			.add(is("deleted", false))
			.seq

}