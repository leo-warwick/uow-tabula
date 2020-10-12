package uk.ac.warwick.tabula.commands.cm2.markingworkflows

import uk.ac.warwick.tabula.data.model.markingworkflow.{CM2MarkingWorkflow, ModeratedWorkflow, ModerationSampler, SelectedModeratedWorkflow}
import uk.ac.warwick.tabula.data.model.{Department, UserGroup}
import uk.ac.warwick.tabula.services.{CM2MarkingWorkflowService, CM2MarkingWorkflowServiceComponent}
import uk.ac.warwick.tabula._
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

class CopyMarkingWorkflowCommandTest extends TestBase with Mockito {

  private trait CommandTestSupport extends CM2MarkingWorkflowServiceComponent {
    override val cm2MarkingWorkflowService: CM2MarkingWorkflowService = smartMock[CM2MarkingWorkflowService]
  }

  private trait Fixture {
    val department: Department = Fixtures.department("ws", "Wizarding Studies")
  }

  private trait SelectedModeratedWorkflowFixture extends Fixture {
    val marker1: User = Fixtures.user("0000001", "marker1")
    val marker2: User = Fixtures.user("0000002", "marker2")
    val moderator: User = Fixtures.user("0000003", "moderator")

    val userLookup: MockUserLookup = new MockUserLookup
    userLookup.registerUserObjects(marker1, marker2, moderator)

    val markingWorkflow: ModeratedWorkflow = SelectedModeratedWorkflow("test", department, ModerationSampler.Admin, Seq(marker1, marker2), Seq(moderator))
    markingWorkflow.academicYear = AcademicYear.now() - 1
    markingWorkflow.isReusable = true
    markingWorkflow.stageMarkers.asScala.foreach(_.markers.asInstanceOf[UserGroup].userLookup = userLookup)
  }

  // TAB-8008
  @Test def copySelectedModerated(): Unit = new SelectedModeratedWorkflowFixture {
    val command = new CopyMarkingWorkflowCommandInternal(department, markingWorkflow) with CommandTestSupport

    val copiedWorkflow: CM2MarkingWorkflow = command.applyInternal()
    copiedWorkflow.stageMarkers.asScala.foreach(_.markers.asInstanceOf[UserGroup].userLookup = userLookup)

    copiedWorkflow should be (anInstanceOf[SelectedModeratedWorkflow])
    copiedWorkflow.name should be ("test")
    copiedWorkflow.academicYear should be (markingWorkflow.academicYear + 1)
    copiedWorkflow.isReusable.booleanValue() should be (true)
    copiedWorkflow.department should be (department)
    copiedWorkflow.asInstanceOf[SelectedModeratedWorkflow].moderationSampler should be (ModerationSampler.Admin)
    copiedWorkflow.markersByRole should be (Map(
      "Marker" -> Seq(marker1, marker2),
      "Moderator" -> Seq(moderator)
    ))
  }

}
