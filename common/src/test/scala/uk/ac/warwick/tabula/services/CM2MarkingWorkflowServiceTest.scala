package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.JavaImports.JArrayList

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.CM2MarkingWorkflowDao
import uk.ac.warwick.tabula.data.model.{AssignmentFeedback, Feedback, MarkerFeedback}
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.{DblBlndFinalMarker, DblBlndInitialMarkerA, SingleMarker, _}
import uk.ac.warwick.tabula.data.model.markingworkflow.{DoubleBlindWorkflow, SingleMarkerWorkflow}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

import scala.collection.Map

// scalastyle:off public.methods.have.type
// scalastyle:off public.property.type.annotation

class CM2MarkingWorkflowServiceTest extends TestBase with Mockito {

	val fs = smartMock[FeedbackService]
	val mwd = smartMock[CM2MarkingWorkflowDao]

	val dept = Fixtures.department("in")
	val assignment = Fixtures.assignment("test")

	val service = new CM2MarkingWorkflowServiceImpl {
		feedbackService = fs
		markingWorkflowDao = mwd
	}

	val marker1 = Fixtures.user("1170836", "cuslaj")
	val marker2 = Fixtures.user("1170837", "cuslak")
	val student1 = Fixtures.user("student1", "student1")
	val student2 = Fixtures.user("student2", "student2")
	val student3 = Fixtures.user("student3", "student3")

	val userLookup = Fixtures.userLookupService(marker1, marker2, student1, student2, student3)

	trait MarkerFeedbackFixture extends Mockito {
		val mf1 = Fixtures.markerFeedback(Fixtures.assignmentFeedback(userId = "student1"))
		mf1.userLookup = userLookup
		mf1.marker = marker1
		val mf2 = Fixtures.markerFeedback(Fixtures.assignmentFeedback(userId = "student2"))
		mf2.userLookup = userLookup
		mf2.marker = marker1
		val mf3 = Fixtures.markerFeedback(Fixtures.assignmentFeedback(userId = "student3"))
		mf3.userLookup = userLookup
		mf3.marker = marker2

		val markerFeedback = Seq(mf1, mf2, mf3)
		mwd.markerFeedbackForAssignmentAndStage(assignment, SingleMarker) returns markerFeedback
	}

	@Test
	def save() {
		val workflow = SingleMarkerWorkflow("testAssignment", dept, Seq(marker1))
		service.save(workflow)
		verify(mwd, times(1)).saveOrUpdate(workflow)
	}

	@Test
	def releaseFeedback(){
		val workflow = SingleMarkerWorkflow("testAssignment", dept, Seq(marker1))
		val assignment = Fixtures.assignment("test")
		assignment.cm2MarkingWorkflow = workflow

		val feedback = Seq(
			Fixtures.assignmentFeedback("1431777", "u1431777"),
			Fixtures.assignmentFeedback("1431778", "u1431778"),
			Fixtures.assignmentFeedback("1431779", "u1431779")
		)
		feedback.foreach(f => f.assignment = assignment)

		val releasedFeedback = service.releaseFeedback(feedback)

		feedback.foreach(f => verify(fs, times(1)).saveOrUpdate(f))

		releasedFeedback.foreach(rf => {
			rf.outstandingStages.asScala should be (Seq(SingleMarker))
		})
	}

	@Test(expected = classOf[IllegalArgumentException])
	def progressAndReturnFeedback(){
		val markerA = Fixtures.user("1170836", "cuslaj")
		val markerB = Fixtures.user("1170837", "cuslak")
		val finalMarker = Fixtures.user("1170838", "cuslal")
		val workflow = DoubleBlindWorkflow("testAssignment", dept, Seq(markerA, markerB), Seq(finalMarker))
		val assignment = Fixtures.assignment("test")
		assignment.cm2MarkingWorkflow = workflow

		val feedback = Seq(
			Fixtures.assignmentFeedback("1431777", "u1431777"),
			Fixtures.assignmentFeedback("1431778", "u1431778"),
			Fixtures.assignmentFeedback("1431779", "u1431779")
		)
		feedback.foreach(f => {
			f.assignment = assignment
			f.outstandingStages = workflow.initialStages.asJava
		})

		val doneA = service.progressFeedback(DblBlndInitialMarkerA, Seq(feedback.head))
		val doneB = service.progressFeedback(DblBlndInitialMarkerB, feedback.tail)
		doneA.foreach(f => f.outstandingStages.asScala should be (Seq(DblBlndInitialMarkerB)))
		doneB.foreach(f => f.outstandingStages.asScala should be (Seq(DblBlndInitialMarkerA)))
		(doneA ++ doneB).foreach(f => verify(fs, times(1)).saveOrUpdate(f))

		val initalDone = service.progressFeedback(DblBlndInitialMarkerB, Seq(feedback.head)) ++
			service.progressFeedback(DblBlndInitialMarkerA, feedback.tail)

		initalDone.foreach(f => f.outstandingStages.asScala should be (Seq(DblBlndFinalMarker)))
		initalDone.foreach(f => verify(fs, times(2)).saveOrUpdate(f))

		val finalDone = service.progressFeedback(DblBlndFinalMarker, initalDone)
		finalDone.foreach(f => f.outstandingStages.asScala should be (Seq(DblBlndCompleted)))
		finalDone.foreach(f => verify(fs, times(3)).saveOrUpdate(f))

		val previous = service.returnFeedback(finalDone)
		previous.foreach(f => f.outstandingStages.asScala should be (Seq(DblBlndFinalMarker)))
		previous.foreach(f => verify(fs, times(4)).saveOrUpdate(f))

		// throws the expected IllegalArgumentException
		service.progressFeedback(DblBlndCompleted, finalDone)
	}

	@Test
	def markerAllocationsAndFeedbackByMarker() { new MarkerFeedbackFixture {
		service.getMarkerAllocations(assignment, SingleMarker) should be(Map(marker1 -> Set(student1, student2), marker2 -> Set(student3)))
		service.feedbackByMarker(assignment, SingleMarker) should be (Map(marker1 -> Seq(mf1,mf2), marker2 -> Seq(mf3)))
	}}

	@Test
	def addMarkersForStage(){
		val workflow = SingleMarkerWorkflow("testAssignment", dept, Nil)
		service.addMarkersForStage(workflow, SingleMarker, Seq(marker1))
		verify(mwd, times(1)).saveOrUpdate(workflow.stageMarkers.asScala.head)
		workflow.stageMarkers.asScala.head.markers.knownType.members should be (Seq("cuslaj"))
	}

	@Test
	def removeMarkersForStage(){
		val workflow = SingleMarkerWorkflow("testAssignment", dept, Seq(marker1, marker2))
		service.removeMarkersForStage(workflow, SingleMarker, Seq(marker1))
		verify(mwd, times(1)).saveOrUpdate(workflow.stageMarkers.asScala.head)
		workflow.stageMarkers.asScala.head.markers.knownType.members should be (Seq("cuslak"))
	}

	@Test(expected = classOf[IllegalArgumentException])
	def removeMarkersForStageErrorNoStageMarkers(){
		val workflow = SingleMarkerWorkflow("testAssignment", dept, Seq(marker1))
		workflow.stageMarkers = JArrayList()
		service.removeMarkersForStage(workflow, SingleMarker, Seq(marker1))
	}

	@Test(expected = classOf[IllegalArgumentException])
	def removeMarkersForStageErrorExistingFeedback() { new MarkerFeedbackFixture {
		val workflow = SingleMarkerWorkflow("testAssignment", dept, Seq(marker1))
		workflow.assignments.add(assignment)
		service.removeMarkersForStage(workflow, SingleMarker, Seq(marker1))
	}}

	@Test
	def allocateMarkersForStage(){
		val workflow = SingleMarkerWorkflow("testAssignment", dept, Seq(marker1))
		val assignment = Fixtures.assignment("test")
		assignment.cm2MarkingWorkflow = workflow
		mwd.markerFeedbackForAssignmentAndStage(assignment, SingleMarker) returns Nil

		val allocations = Seq(
			marker1 -> Set(student1, student2),
			marker2 -> Set(student3)
		).toMap

		val result = service.allocateMarkersForStage(assignment, SingleMarker, allocations)
		result.foreach(_.userLookup = userLookup)
		result.find(_.feedback.usercode == student1.getUserId).get.marker should be (marker1)
		val feedback = result.find(_.feedback.usercode == student2.getUserId).get
		feedback.marker should be (marker1)
		feedback.mark = Some(41)
		result.find(_.feedback.usercode == student3.getUserId).get.marker should be (marker2)

		verify(fs, times(3)).saveOrUpdate(any[Feedback])
		verify(fs, times(3)).save(any[MarkerFeedback])

		// update the mock to return the new values
		mwd.markerFeedbackForAssignmentAndStage(assignment, SingleMarker) returns result
		//add the mock userlookup to all the generated mf so the next test will work - bleah :(
		result.foreach(_.userLookup = userLookup)
		result.map(_.feedback.asInstanceOf[AssignmentFeedback]).foreach(assignment.feedbacks.add)

		val allocations2 = Seq(
			marker1 -> Set(student1),
			marker2 -> Set(student2, student3)
		).toMap
		val result2 = service.allocateMarkersForStage(assignment, SingleMarker, allocations2)
		result2.find(_.feedback.usercode == student1.getUserId).get.marker should be (marker1)
		val feedback2 = result2.find(_.feedback.usercode == student2.getUserId).get
		feedback2.marker should be (marker2)
		feedback2.mark should be (Some(41)) // marker has changed but the feedback is still here
		result2.find(_.feedback.usercode == student3.getUserId).get.marker should be (marker2)

		verify(fs, times(3)).saveOrUpdate(any[Feedback])
		verify(fs, times(6)).save(any[MarkerFeedback])

		// update the mock to return the new values
		mwd.markerFeedbackForAssignmentAndStage(assignment, SingleMarker) returns result2

		val allocations3 = Seq(
			marker1 -> Set[User](),
			marker2 -> Set(student2, student3)
		).toMap
		val result3 = service.allocateMarkersForStage(assignment, SingleMarker, allocations3)
		// student ones marker feedback shouldn't have a marker now
		result2.find(_.feedback.usercode == student1.getUserId).get.marker.isFoundUser should be (false)
		result3.size should be(2)

		verify(fs, times(3)).saveOrUpdate(any[Feedback])
		verify(fs, times(9)).save(any[MarkerFeedback])

	}

	@Test
	def allFeedbackForMarker() {

		val assignment = Fixtures.assignment("test")

		val feedback = Fixtures.assignmentFeedback("1431777", "u1431777")
		val feedback2 = Fixtures.assignmentFeedback("1431778", "u1431778")

		val markerFeedback = Fixtures.markerFeedback(feedback)
		markerFeedback.marker = marker1
		markerFeedback.stage = DblBlndInitialMarkerA

		val markerFeedback2 = Fixtures.markerFeedback(feedback)
		markerFeedback2.marker = marker2
		markerFeedback2.stage = DblBlndInitialMarkerB

		val markerFeedback3 = Fixtures.markerFeedback(feedback)
		markerFeedback3.marker = marker1
		markerFeedback3.stage = DblBlndFinalMarker

		val markerFeedback4 = Fixtures.markerFeedback(feedback2)
		markerFeedback4.marker = marker1
		markerFeedback4.stage = DblBlndInitialMarkerB

		val markerFeedback5 = Fixtures.markerFeedback(feedback2)
		markerFeedback5.marker = marker2
		markerFeedback5.stage = DblBlndInitialMarkerA

		mwd.markerFeedbackForMarker(assignment, marker1) returns Seq(markerFeedback, markerFeedback3, markerFeedback4)
		mwd.markerFeedbackForMarker(assignment, marker2) returns Seq(markerFeedback2, markerFeedback5)

		service.getAllFeedbackForMarker(assignment, marker1) should be (Map(
			DblBlndInitialMarkerA -> Seq(markerFeedback),
			DblBlndInitialMarkerB -> Seq(markerFeedback4),
			DblBlndFinalMarker -> Seq(markerFeedback3)
		))

		service.getAllFeedbackForMarker(assignment, marker2) should be (Map(
			DblBlndInitialMarkerA -> Seq(markerFeedback5),
			DblBlndInitialMarkerB -> Seq(markerFeedback2)
		))

	}
}
