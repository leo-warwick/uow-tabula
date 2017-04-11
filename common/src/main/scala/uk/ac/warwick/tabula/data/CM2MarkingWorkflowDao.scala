package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.markingworkflow.{CM2MarkingWorkflow, MarkingWorkflowStage, StageMarkers}
import uk.ac.warwick.userlookup.User


trait CM2MarkingWorkflowDaoComponent {
	def markingWorkflowDao: CM2MarkingWorkflowDao
}

trait AutowiringCM2MarkingWorkflowDaoComponent extends CM2MarkingWorkflowDaoComponent {
	var markingWorkflowDao: CM2MarkingWorkflowDao = Wire[CM2MarkingWorkflowDao]
}

trait CM2MarkingWorkflowDao {
	def get(id: String): Option[CM2MarkingWorkflow]
	def getReusableWorkflows(department: Department, academicYear: AcademicYear): Seq[CM2MarkingWorkflow]
	def saveOrUpdate(workflow: CM2MarkingWorkflow): Unit
	def saveOrUpdate(markers: StageMarkers): Unit
	def markerFeedbackForAssignmentAndStage(assignment: Assignment, stage: MarkingWorkflowStage): Seq[MarkerFeedback]
	def markerFeedbackForFeedback(feedback: AssignmentFeedback): Seq[MarkerFeedback]
	def markerFeedbackForMarker(assignment: Assignment, marker: User): Seq[MarkerFeedback]
	def delete(workflow: CM2MarkingWorkflow): Unit
}

@Repository
class CM2MarkingWorkflowDaoImpl extends CM2MarkingWorkflowDao with Daoisms {
	override def get(id: String): Option[CM2MarkingWorkflow] = getById[CM2MarkingWorkflow](id)
	override def getReusableWorkflows(department: Department, academicYear: AcademicYear): Seq[CM2MarkingWorkflow] = {

		session.newQuery[CM2MarkingWorkflow]("""select c from CM2MarkingWorkflow c
				where c.academicYear = :year
				and c.department = :department
				and c.isReusable = true""")
			.setParameter("year", academicYear)
			.setEntity("department", department)
			.distinct
			.seq
	}
	override def saveOrUpdate(workflow: CM2MarkingWorkflow): Unit = session.saveOrUpdate(workflow)
	override def saveOrUpdate(markers: StageMarkers): Unit = session.saveOrUpdate(markers)
	override def markerFeedbackForAssignmentAndStage(assignment: Assignment, stage: MarkingWorkflowStage): Seq[MarkerFeedback] = {
		session.newCriteria[MarkerFeedback]
			.createAlias("feedback", "f")
			.add(is("stage", stage))
			.add(is("f.assignment", assignment))
			.seq
	}

	override def markerFeedbackForFeedback(feedback: AssignmentFeedback): Seq[MarkerFeedback] = {
		session.newCriteria[MarkerFeedback]
			.add(is("feedback", feedback))
			.seq
	}

	override def markerFeedbackForMarker(assignment: Assignment, marker: User): Seq[MarkerFeedback] = {
		session.newCriteria[MarkerFeedback]
			.createAlias("feedback", "f")
			.add(is("markerUsercode", marker.getUserId))
			.add(is("f.assignment", assignment))
			.seq
	}

	override def delete(workflow: CM2MarkingWorkflow): Unit = {
		session.delete(workflow)
	}
}