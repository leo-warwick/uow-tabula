package uk.ac.warwick.tabula.commands.coursework.markingworkflows

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{Assignment, Department, SeenSecondMarkingWorkflow, UserGroup}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{Fixtures, MockUserLookup, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

import collection.JavaConverters._

class OldReplaceMarkerInMarkingWorkflowCommandTest extends TestBase with Mockito {

	val mockUserLookup = new MockUserLookup
	val mockAssessmentService: AssessmentService = smartMock[AssessmentService]
	val mockMarkingWorkflowService: MarkingWorkflowService = smartMock[MarkingWorkflowService]

	trait Fixture {
		val marker1 = new User("marker1")
		val marker2 = new User("marker2")
		val marker3 = new User("marker3")
		val marker4 = new User("marker4")
		mockUserLookup.registerUserObjects(marker1, marker2, marker3, marker4)
		mockUserLookup.users.values.foreach(_.setFoundUser(true))

		val thisMarkingWorkflow = new SeenSecondMarkingWorkflow
		val firstMarkersGroup: UserGroup = UserGroup.ofUsercodes
		firstMarkersGroup.add(marker1)
		firstMarkersGroup.add(marker2)
		firstMarkersGroup.asInstanceOf[UserGroup].userLookup = mockUserLookup
		thisMarkingWorkflow.firstMarkers = firstMarkersGroup
		val secondMarkersGroup: UserGroup = UserGroup.ofUsercodes
		secondMarkersGroup.add(marker2)
		secondMarkersGroup.add(marker3)
		secondMarkersGroup.asInstanceOf[UserGroup].userLookup = mockUserLookup
		thisMarkingWorkflow.secondMarkers = secondMarkersGroup

		val validator = new ReplaceMarkerInMarkingWorkflowValidation with ReplaceMarkerInMarkingWorkflowCommandState
			with ReplaceMarkerInMarkingWorkflowCommandRequest with UserLookupComponent with MarkingWorkflowServiceComponent {

			val userLookup: MockUserLookup = mockUserLookup
			val markingWorkflowService = null
			val department = null
			val markingWorkflow: SeenSecondMarkingWorkflow = thisMarkingWorkflow
		}

		val department: Department = Fixtures.department("its")
		val command = new ReplaceMarkerInMarkingWorkflowCommandInternal(department, thisMarkingWorkflow) with UserLookupComponent with MarkingWorkflowServiceComponent
			with AssessmentServiceComponent with ReplaceMarkerInMarkingWorkflowCommandRequest with ReplaceMarkerInMarkingWorkflowCommandState{

			val userLookup: MockUserLookup = mockUserLookup
			val assessmentService: AssessmentService = mockAssessmentService
			val markingWorkflowService: MarkingWorkflowService = mockMarkingWorkflowService
		}

		val assignment1: Assignment = Fixtures.assignment("assignment1")
		assignment1.markingWorkflow = thisMarkingWorkflow
		Seq(
			Fixtures.firstMarkerMap(assignment1, marker1.getUserId, Seq("student1", "student2")),
			Fixtures.firstMarkerMap(assignment1, marker2.getUserId, Seq("student3", "student4"))
		).foreach(assignment1.firstMarkers.add)
		Seq(
			Fixtures.secondMarkerMap(assignment1, marker2.getUserId, Seq("student1", "student2")),
			Fixtures.secondMarkerMap(assignment1, marker3.getUserId, Seq("student3", "student4"))
		).foreach(assignment1.secondMarkers.add)

		mockMarkingWorkflowService.getAssignmentsUsingMarkingWorkflow(thisMarkingWorkflow) returns Seq(assignment1)
	}

	@Test
	def validateMissingOldMarker(): Unit = {
		new Fixture {
			var errors = new BindException(validator, "command")
			validator.validate(errors)
			errors.hasFieldErrors("oldMarker") should be {true}
		}
	}

	@Test
	def validateMissingNewMarker(): Unit = {
		new Fixture {
			var errors = new BindException(validator, "command")
			validator.validate(errors)
			errors.hasFieldErrors("newMarker") should be {true}
		}
	}

	@Test
	def validateNoSuchOldMarker(): Unit = {
		new Fixture {
			validator.oldMarker = marker4.getUserId
			var errors = new BindException(validator, "command")
			validator.validate(errors)
			errors.hasFieldErrors("oldMarker") should be {true}
		}
	}

	@Test
	def validateNoSuchNewMarker(): Unit = {
		new Fixture {
			validator.newMarker = "someGuy"
			var errors = new BindException(validator, "command")
			validator.validate(errors)
			errors.hasFieldErrors("newMarker") should be {true}
		}
	}

	@Test
	def validateNotConfirmed(): Unit = {
		new Fixture {
			var errors = new BindException(validator, "command")
			validator.validate(errors)
			errors.hasFieldErrors("confirm") should be {true}
		}
	}

	@Test
	def validateOK(): Unit = {
		new Fixture {
			validator.oldMarker = marker1.getUserId
			validator.newMarker = marker4.getUserId
			validator.confirm = true

			var errors = new BindException(validator, "command")
			validator.validate(errors)
			errors.hasFieldErrors("oldMarker") should be {false}
			errors.hasFieldErrors("newMarker") should be {false}
			errors.hasFieldErrors("confirm") should be {false}
		}
	}

	@Test
	def replaceWithExisting(): Unit = {
		new Fixture {
			command.oldMarker = marker1.getUserId
			command.newMarker = marker3.getUserId
			command.applyInternal()
			thisMarkingWorkflow.firstMarkers.includesUser(marker1) should be {false}
			thisMarkingWorkflow.secondMarkers.includesUser(marker1) should be {false}
			thisMarkingWorkflow.firstMarkers.includesUser(marker3) should be {true}
			thisMarkingWorkflow.secondMarkers.includesUser(marker3) should be {true}
			assignment1.firstMarkers.asScala.exists(_.marker_id == marker1.getUserId) should be {false}
			assignment1.secondMarkers.asScala.exists(_.marker_id == marker1.getUserId) should be {false}
			assignment1.firstMarkers.asScala.exists(_.marker_id == marker3.getUserId) should be {true}
			assignment1.secondMarkers.asScala.exists(_.marker_id == marker3.getUserId) should be {true}
			assignment1.firstMarkerMap(marker3.getUserId).knownType.members.size should be (2)
			assignment1.secondMarkerMap(marker3.getUserId).knownType.members.size should be (2)
			verify(mockAssessmentService, times(1)).save(assignment1)
			verify(mockMarkingWorkflowService, times(1)).save(thisMarkingWorkflow)
		}
		new Fixture {
			command.oldMarker = marker1.getUserId
			command.newMarker = marker2.getUserId
			command.applyInternal()
			thisMarkingWorkflow.firstMarkers.includesUser(marker1) should be {false}
			thisMarkingWorkflow.secondMarkers.includesUser(marker1) should be {false}
			thisMarkingWorkflow.firstMarkers.includesUser(marker2) should be {true}
			thisMarkingWorkflow.secondMarkers.includesUser(marker2) should be {true}
			assignment1.firstMarkers.asScala.exists(_.marker_id == marker1.getUserId) should be {false}
			assignment1.secondMarkers.asScala.exists(_.marker_id == marker1.getUserId) should be {false}
			assignment1.firstMarkers.asScala.exists(_.marker_id == marker2.getUserId) should be {true}
			assignment1.secondMarkers.asScala.exists(_.marker_id == marker2.getUserId) should be {true}
			assignment1.firstMarkerMap(marker2.getUserId).knownType.members.size should be (4)
			assignment1.secondMarkerMap(marker2.getUserId).knownType.members.size should be (2)
			verify(mockAssessmentService, times(1)).save(assignment1)
			verify(mockMarkingWorkflowService, times(1)).save(thisMarkingWorkflow)
		}
	}

	@Test
	def replaceWithNew(): Unit = {
		new Fixture {
			command.oldMarker = marker2.getUserId
			command.newMarker = marker4.getUserId
			command.applyInternal()
			thisMarkingWorkflow.firstMarkers.includesUser(marker2) should be {false}
			thisMarkingWorkflow.secondMarkers.includesUser(marker2) should be {false}
			thisMarkingWorkflow.firstMarkers.includesUser(marker4) should be {true}
			thisMarkingWorkflow.secondMarkers.includesUser(marker4) should be {true}
			assignment1.firstMarkers.asScala.exists(_.marker_id == marker2.getUserId) should be {false}
			assignment1.secondMarkers.asScala.exists(_.marker_id == marker2.getUserId) should be {false}
			assignment1.firstMarkers.asScala.exists(_.marker_id == marker4.getUserId) should be {true}
			assignment1.secondMarkers.asScala.exists(_.marker_id == marker4.getUserId) should be {true}
			assignment1.firstMarkerMap(marker4.getUserId).knownType.members.size should be (2)
			assignment1.secondMarkerMap(marker4.getUserId).knownType.members.size should be (2)
			verify(mockAssessmentService, times(1)).save(assignment1)
			verify(mockMarkingWorkflowService, times(1)).save(thisMarkingWorkflow)
		}
	}

}
