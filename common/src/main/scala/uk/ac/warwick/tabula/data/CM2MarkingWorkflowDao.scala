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

  def markerFeedbackForMarker(assignment: Assignment, marker: User): Seq[MarkerFeedback]

  def delete(workflow: CM2MarkingWorkflow): Unit

  /** All assignments using this marking workflow. */
  def getAssignmentsUsingMarkingWorkflow(workflow: CM2MarkingWorkflow): Seq[Assignment]

  def getAssignmentsUsingMarkingWorkflows(workflows: Seq[CM2MarkingWorkflow]): Seq[Assignment]
}

@Repository
class CM2MarkingWorkflowDaoImpl extends CM2MarkingWorkflowDao with Daoisms {
  override def get(id: String): Option[CM2MarkingWorkflow] = getById[CM2MarkingWorkflow](id)

  override def getReusableWorkflows(department: Department, academicYear: AcademicYear): Seq[CM2MarkingWorkflow] = {

    session.newQuery[CM2MarkingWorkflow](
      """select c from CM2MarkingWorkflow c
				where c.academicYear = :year
				and c.department = :department
				and c.isReusable = true
				order by c.name""")
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

  override def markerFeedbackForMarker(assignment: Assignment, marker: User): Seq[MarkerFeedback] = {
    session.newCriteria[MarkerFeedback]
      .createAlias("feedback", "f")
      .add(is("markerUsercode", marker.getUserId))
      .add(is("f.assignment", assignment))
      .seq
      .distinct
  }

  override def delete(workflow: CM2MarkingWorkflow): Unit = {
    session.delete(workflow)
  }

  def getAssignmentsUsingMarkingWorkflow(workflow: CM2MarkingWorkflow): Seq[Assignment] =
    session.newCriteria[Assignment]
      .add(is("cm2MarkingWorkflow", workflow))
      .add(is("deleted", false))
      .seq

  def getAssignmentsUsingMarkingWorkflows(workflows: Seq[CM2MarkingWorkflow]): Seq[Assignment] =
    if (workflows.isEmpty) Nil
    else
      session.newCriteria[Assignment]
        .add(safeIn("cm2MarkingWorkflow", workflows))
        .add(is("deleted", false))
        .seq
}