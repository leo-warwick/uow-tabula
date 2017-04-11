package uk.ac.warwick.tabula.commands.coursework.assignments

import uk.ac.warwick.tabula.data.model.{Assignment, Feedback, FirstMarkersMap, Module, UserGroup}
import uk.ac.warwick.tabula.helpers.Tap.tap
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{MockUserLookup, Mockito, TestBase}

import scala.collection.JavaConverters._

class ReleaseForMarkingCommandTest extends TestBase  with Mockito {

	trait MockUserLookupComponent extends UserLookupComponent {
		override def userLookup = new MockUserLookup
	}

	val ug1: UserGroup = UserGroup.ofUniversityIds.tap(g=>{
		g.includedUserIds = Seq("1", "2","4")
		g.userLookup = new MockUserLookup(true)
	})
	val ug2: UserGroup = UserGroup.ofUniversityIds.tap(g=>{
		g.includedUserIds = Seq("1", "2","3")
		g.userLookup = new MockUserLookup(true)
	})

	@Test
	def cantReleaseIfNoMarkerAssigned() {
		withUser("test") {

			val assignment = new Assignment().tap {
				a =>
					a.module = new Module().tap(_.id = "module_id")
			}

			val cmd = new ReleaseForMarkingCommand(assignment.module, assignment, currentUser.apparentUser)
			with MockUserLookupComponent with ReleaseForMarkingCommandTestSupport {
				override def studentsWithKnownMarkers = Seq()
			}

			cmd.students = Seq("1", "2", "3").asJava
			cmd.unreleasableSubmissions should be(Seq("1","2","3"))
			cmd.studentsWithKnownMarkers should be(Seq())
		}
	}

	@Test
	def testCanReleaseIfMarkerIsAssigned() {
		withUser("test") {

			val assignment = new Assignment().tap {
				a =>
					a.firstMarkers = Seq(FirstMarkersMap(a, "marker1", ug1)).asJava
					a.module = new Module().tap(_.id = "module_id")
			}

			val cmd = new ReleaseForMarkingCommand(assignment.module, assignment, currentUser.apparentUser)
			with MockUserLookupComponent with ReleaseForMarkingCommandTestSupport {
				override def studentsWithKnownMarkers = Seq("1","2")
			}

			cmd.students = Seq("1", "2", "3").asJava
			cmd.unreleasableSubmissions should be(Seq("3"))
			cmd.studentsWithKnownMarkers should be(Seq("1","2"))
		}
	}
	@Test
	def releasesAllSubmissionsIfMarkersAllocated() {
		withUser("test") {

			val assignment = new Assignment().tap {
				a =>
					a.firstMarkers = Seq(FirstMarkersMap(a, "marker1", ug2)).asJava
					a.module = new Module().tap(_.id = "module_id")
			}

			val cmd = new ReleaseForMarkingCommand(assignment.module, assignment, currentUser.apparentUser)
			with MockUserLookupComponent with ReleaseForMarkingCommandTestSupport {
				override def studentsWithKnownMarkers = Seq("1","2", "3")
			}
			cmd.students = Seq("1", "2", "3").asJava

			val feedbacks = cmd.applyInternal()
			feedbacks.length should be (3)
		}
	}

	@Test
	def releasesOnlyReleaseableSubmissions() {
		withUser("test") {

			val assignment = new Assignment().tap {
				a =>
					a.firstMarkers = Seq(FirstMarkersMap(a, "marker1", ug1)).asJava
					a.module = new Module().tap(_.id = "module_id")
			}

			val cmd = new ReleaseForMarkingCommand(assignment.module, assignment, currentUser.apparentUser)
			with MockUserLookupComponent with ReleaseForMarkingCommandTestSupport {
				override def studentsWithKnownMarkers = Seq("1","2")
			}
			cmd.students = Seq("1", "2", "3").asJava

			val feedbacks = cmd.applyInternal()
			feedbacks.length should be (2)
		}
	}
}

trait ReleaseForMarkingCommandTestSupport extends AssessmentServiceComponent with StateServiceComponent
with FeedbackServiceComponent with Mockito {

	val assessmentService: AssessmentService = mock[AssessmentService]
	val stateService: StateService = mock[StateService]
	val feedbackService: FeedbackService = mock[FeedbackService]
	def apply(): List[Feedback] = List()
}
