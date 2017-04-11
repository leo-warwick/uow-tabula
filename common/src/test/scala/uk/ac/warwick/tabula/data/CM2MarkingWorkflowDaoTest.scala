package uk.ac.warwick.tabula.data

import scala.collection.JavaConverters._
import org.junit.Before
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage._
import uk.ac.warwick.tabula.data.model.markingworkflow.{DoubleBlindWorkflow, SingleMarkerWorkflow}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, PersistenceTestBase}
import uk.ac.warwick.userlookup.User

class CM2MarkingWorkflowDaoTest extends PersistenceTestBase {

	val dao = new CM2MarkingWorkflowDaoImpl

	val dept: Department = Fixtures.department("in")
	val marker: User = Fixtures.user("1170836", "cuslaj")
	val marker2: User = Fixtures.user("1170837", "cuslak")

	@Before
	def setup(): Unit = transactional { tx =>
		dao.sessionFactory = sessionFactory
		session.save(dept)
		session.flush()
	}

	@Test def saveThenFetch() = transactional { tx =>
		val singleMarkerWorkflow = SingleMarkerWorkflow("testAssignment", dept, Seq(marker))
		singleMarkerWorkflow.academicYear = AcademicYear(2016)

		dao.saveOrUpdate(singleMarkerWorkflow)
		session.flush()

		val workflow = dao.get(singleMarkerWorkflow.id).get
		workflow should be (singleMarkerWorkflow)

		workflow.name should be ("testAssignment")
		workflow.department should be (dept)
		workflow.academicYear should be (AcademicYear(2016))
		workflow.initialStages.size should be (1)

		val stageMarker = workflow.stageMarkers.asScala.head
		stageMarker.stage should be (SingleMarker)
		stageMarker.workflow should be (workflow)
		stageMarker.markers.knownType.members should be (Seq("cuslaj"))

		val stage = workflow.initialStages.head
		stage.nextStages should be {Seq(SingleMarkingCompleted)}
	}

	@Test def testGetReusableWorkflows() = transactional { tx =>
		val singleMarkerWorkflow = SingleMarkerWorkflow("testAssignment", dept, Seq(marker))
		singleMarkerWorkflow.academicYear = AcademicYear(2016)
		singleMarkerWorkflow.isReusable = true
		dao.saveOrUpdate(singleMarkerWorkflow)

		val doubleBlindWorkflow = DoubleBlindWorkflow("testAssignment", dept, Seq(marker), Seq(marker))
		doubleBlindWorkflow.academicYear = AcademicYear(2015)
		doubleBlindWorkflow.isReusable = true
		dao.saveOrUpdate(doubleBlindWorkflow)
		session.flush()

		dao.getReusableWorkflows(dept, AcademicYear(2016)).length should be (1)
		dao.getReusableWorkflows(dept, AcademicYear(2015)).length should be (1)
		dao.getReusableWorkflows(dept, AcademicYear(2014)).length should be (0)
	}

	@Test def markerFeedbackForAssignmentAndStage() = transactional { tx =>
		val w = SingleMarkerWorkflow("test", dept, Seq(marker))
		val assignment = Fixtures.assignment("test")
		assignment.cm2MarkingWorkflow = w
		val feedback = Fixtures.assignmentFeedback("1431777", "u1431777")
		feedback.assignment = assignment
		val markerFeedback = Fixtures.markerFeedback(feedback)
		markerFeedback.marker = marker
		markerFeedback.stage = SingleMarker

		val assignment2 = Fixtures.assignment("test2")
		assignment2.cm2MarkingWorkflow = w
		val feedback2 = Fixtures.assignmentFeedback("1431777", "u1431777")
		feedback2.assignment = assignment2
		val markerFeedback2 = Fixtures.markerFeedback(feedback2)
		markerFeedback2.marker = marker
		markerFeedback2.stage = SingleMarker

		session.save(assignment)
		session.save(assignment2)
		session.save(feedback)
		session.save(feedback2)
		session.save(markerFeedback)
		session.save(markerFeedback2)
		session.save(w)
		session.flush()

		dao.markerFeedbackForAssignmentAndStage(assignment, SingleMarker) should be (Seq(markerFeedback))
		dao.markerFeedbackForAssignmentAndStage(assignment2, SingleMarker) should be (Seq(markerFeedback2))
	}

	@Test def markerFeedbackForMarker() = transactional { tx =>
		val w = DoubleBlindWorkflow("test", dept, Seq(marker, marker2), Seq(marker))
		val assignment = Fixtures.assignment("test")
		assignment.cm2MarkingWorkflow = w

		val feedback = Fixtures.assignmentFeedback("1431777", "u1431777")
		feedback.assignment = assignment

		val markerFeedback = Fixtures.markerFeedback(feedback)
		markerFeedback.marker = marker
		markerFeedback.stage = DblBlndInitialMarkerA

		val markerFeedback2 = Fixtures.markerFeedback(feedback)
		markerFeedback2.marker = marker2
		markerFeedback2.stage = DblBlndInitialMarkerB

		val markerFeedback3 = Fixtures.markerFeedback(feedback)
		markerFeedback3.marker = marker
		markerFeedback3.stage = DblBlndFinalMarker

		val feedback2 = Fixtures.assignmentFeedback("1431778", "u1431778")
		feedback2.assignment = assignment

		val markerFeedback4 = Fixtures.markerFeedback(feedback2)
		markerFeedback4.marker = marker
		markerFeedback4.stage = DblBlndInitialMarkerB

		val markerFeedback5 = Fixtures.markerFeedback(feedback2)
		markerFeedback5.marker = marker2
		markerFeedback5.stage = DblBlndInitialMarkerA

		session.save(assignment)
		Seq(feedback, feedback2).foreach(session.save)
		Seq(markerFeedback, markerFeedback2, markerFeedback3, markerFeedback4, markerFeedback5).foreach(session.save)
		session.save(w)
		session.flush()

		val marker1MarkerFeedback = dao.markerFeedbackForMarker(assignment, marker)
		marker1MarkerFeedback.size should be (3)
		marker1MarkerFeedback.contains(markerFeedback) should be {true}
		marker1MarkerFeedback.contains(markerFeedback3) should be {true}
		marker1MarkerFeedback.contains(markerFeedback4) should be {true}

		val marker2MarkerFeedback = dao.markerFeedbackForMarker(assignment, marker2)
		marker2MarkerFeedback.size should be (2)
		marker2MarkerFeedback.contains(markerFeedback2) should be {true}
		marker2MarkerFeedback.contains(markerFeedback5) should be {true}
	}

}
