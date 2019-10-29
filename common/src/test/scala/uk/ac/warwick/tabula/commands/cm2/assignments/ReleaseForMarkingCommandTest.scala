package uk.ac.warwick.tabula.commands.cm2.assignments


import uk.ac.warwick.tabula.data.model.AssignmentFeedback
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.SingleMarker
import uk.ac.warwick.tabula.data.model.markingworkflow.SingleMarkerWorkflow
import uk.ac.warwick.tabula.services.{CM2MarkingWorkflowService, CM2MarkingWorkflowServiceComponent}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

import scala.collection.JavaConverters._


class ReleaseForMarkingCommandTest extends TestBase with Mockito {

  trait MockCM2MarkingWorkflowServiceComponent extends CM2MarkingWorkflowServiceComponent {
    val cm2MarkingWorkflowService: CM2MarkingWorkflowService = mock[CM2MarkingWorkflowService]
    cm2MarkingWorkflowService.releaseForMarking(any[Seq[AssignmentFeedback]]) answers { f: Any =>
      val released = f.asInstanceOf[Seq[AssignmentFeedback]]
      released.foreach(_.outstandingStages.add(SingleMarker))
      released
    }
  }

  @Test
  def cantReleaseIfNoMarkerAssigned() {
    withUser("test") {
      val marker = Fixtures.user("1170836", "cuslaj")
      val assignment = newDeepAssignment()
      assignment.cm2MarkingWorkflow = SingleMarkerWorkflow("test", assignment.module.adminDepartment, Seq(marker))
      val cmd = ReleaseForMarkingCommand(assignment, currentUser.apparentUser)
      cmd.students = Seq("1", "2", "3").asJava
      cmd.unreleasableSubmissions should be(Seq("1", "2", "3"))
    }
  }

  @Test
  def testStudentsAlreadyReleased() {
    withUser("test") {
      val marker = Fixtures.user("1170836", "cuslaj")
      val assignment = newDeepAssignment()
      assignment.cm2MarkingWorkflow = SingleMarkerWorkflow("test", assignment.module.adminDepartment, Seq(marker))
      val feedback = Fixtures.assignmentFeedback("1", "1")

      feedback.outstandingStages.add(SingleMarker)
      assignment.feedbacks.add(feedback)
      val cmd = ReleaseForMarkingCommand(assignment, currentUser.apparentUser)
      cmd.students = Seq("1", "2", "3").asJava
      // no known marker as firstmarker not assigned
      cmd.studentsWithoutKnownMarkers should be(Seq("1", "2", "3"))
      feedback.markerFeedback.add({
        val m = Fixtures.markerFeedback(feedback)
        val u = Fixtures.user("marker", "marker")
        m.marker = u
        m.userLookup = Fixtures.userLookupService(u)
        m.stage = SingleMarker
        m
      })
      // has known marker as first marker is assigned
      cmd.studentsWithoutKnownMarkers should be(Seq("2", "3"))
      cmd.studentsAlreadyReleased should be(Seq("1"))
      cmd.unreleasableSubmissions should be(Seq("2", "3", "1"))
    }
  }

  @Test
  def testCanReleaseIfMarkerIsAssigned() {
    withUser("test") {
      val marker = Fixtures.user("1170836", "cuslaj")
      val assignment = newDeepAssignment()
      assignment.cm2MarkingWorkflow = SingleMarkerWorkflow("test", assignment.module.adminDepartment, Seq(marker))
      val feedback = Fixtures.assignmentFeedback("1", "1")
      val mf = Fixtures.markerFeedback(feedback)
      mf.userLookup = Fixtures.userLookupService(marker)
      mf.marker = marker
      mf.stage = SingleMarker
      assignment.feedbacks.add(feedback)
      val cmd = new ReleaseForMarkingCommandInternal(assignment, currentUser.apparentUser) with MockCM2MarkingWorkflowServiceComponent
      cmd.students = Seq("1", "2", "3").asJava
      cmd.studentsWithoutKnownMarkers should be(Seq("2", "3"))
      cmd.studentsAlreadyReleased should be(Nil)
      cmd.unreleasableSubmissions should be(Seq("2", "3"))

      cmd.applyInternal() should be(Seq(feedback))
      cmd.newReleasedFeedback.asScala should be(Seq(mf))
    }
  }

}
