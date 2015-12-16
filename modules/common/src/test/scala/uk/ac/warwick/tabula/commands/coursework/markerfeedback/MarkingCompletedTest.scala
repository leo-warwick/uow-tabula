package uk.ac.warwick.tabula.commands.coursework.markerfeedback

import org.hibernate.Session
import org.junit.{After, Before}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.coursework.ReleasedState
import uk.ac.warwick.tabula.{Mockito, _}
import uk.ac.warwick.tabula.commands.UserAware
import uk.ac.warwick.tabula.commands.coursework.assignments._
import uk.ac.warwick.tabula.data.SessionComponent
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.coursework.ReleaseToMarkerNotification
import uk.ac.warwick.tabula.events.EventListener
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services._

import scala.collection.JavaConversions._

/*
 * Fixed this test by replacing the full appcontext with a minimal functional one as below.
 * Not completely convinced this is the best solution, but as the command creates subcommands
 * and calls their apply() it seems necessary to make the services it looks for available. So
 * this may be the best we can do without refactoring Command some more.
 *
 * MinimalCommandDependencies could be pulled out and reused in other tests, though as I say,
 *
 */

object MarkingCompletedTest {
	class Ctx extends FunctionalContext with Mockito {
		bean() { mock[MaintenanceModeService] }
		bean() { mock[EventListener] }
	}
}

class MarkingCompletedTest extends TestBase with MarkingWorkflowWorld with Mockito with FunctionalContextTesting with FeedbackServiceComponent {

	val feedbackService = mock[FeedbackService]

	@Before
	def before() {
		Wire.ignoreMissingBeans = true
	}

	@After
	def afterTheFeast() {
		Wire.ignoreMissingBeans = false
	}

	private trait CommandTestSupport extends StateServiceComponent with FeedbackServiceComponent with FinaliseFeedbackTestImpl {
		self: UserAware =>

		val feedbackService = MarkingCompletedTest.this.feedbackService

		val stateService: StateService = new ComposableStateServiceImpl with FeedbackServiceComponent {
			val feedbackService = CommandTestSupport.this.feedbackService
		}
	}

	trait FinaliseFeedbackTestImpl extends FinaliseFeedbackComponent {
		self: UserAware =>

		def finaliseFeedback(assignment: Assignment, markerFeedbacks: Seq[MarkerFeedback]) {
			val finaliseFeedbackCommand = FinaliseFeedbackCommand(assignment, markerFeedbacks, user)
			finaliseFeedbackCommand.applyInternal()
		}
	}

	private trait CommandFixture {
		val command =
			new MarkingCompletedCommand(assignment.module, assignment, currentUser.apparentUser, currentUser)
				with CommandTestSupport
	}

	@Test
	def firstMarkerFinished() {
		withUser("cuslaj") { new CommandFixture {
			val students = List("9876004", "0270954", "9170726")
			command.markerFeedback = assignment.feedbacks.filter(f => students.contains(f.universityId)).map(_.firstMarkerFeedback)

			command.onBind(null)

			command.noFeedback.size should be (3)
			command.noMarks.size should be (3)

			command.applyInternal()
			val releasedFeedback = assignment.feedbacks.map(_.firstMarkerFeedback).filter(_.state == MarkingState.MarkingCompleted)
			releasedFeedback.size should be (3)

			val secondMarkerFeedback = assignment.feedbacks.flatMap(f => Option(f.secondMarkerFeedback)).filter(_.state == MarkingState.ReleasedForMarking)
			secondMarkerFeedback.size should be (3)
		}}
	}



	@Test
	def secondMarkerFinished(){
		inContext[MarkingCompletedTest.Ctx] {
		withUser("cuday") { new CommandFixture {
			val students = List("0672088", "0672089")
			assignment.feedbacks.map(addMarkerFeedback(_, SecondFeedback))
			val feedbacks = assignment.feedbacks.filter(f => students.contains(f.universityId))
			command.markerFeedback = feedbacks.map(_.secondMarkerFeedback)
			setFirstMarkerFeedbackState(MarkingState.MarkingCompleted)
			command.onBind(null)

			command.noFeedback.size should be (2)
			command.noMarks.size should be (2)

			command.applyInternal()

			val firstFeedback = assignment.feedbacks.flatMap(f => Option(f.firstMarkerFeedback))
			val completedFirstMarking = firstFeedback.filter(_.state == MarkingState.MarkingCompleted)
			completedFirstMarking.size should be (5)

			val secondFeedback = assignment.feedbacks.flatMap(f => Option(f.secondMarkerFeedback))
			val completedSecondMarking = secondFeedback.filter(_.state == MarkingState.MarkingCompleted)
			completedSecondMarking.size should be (2)

			val finalFeedback = assignment.feedbacks.flatMap(f => Option(f.thirdMarkerFeedback)).filter(_.state == MarkingState.ReleasedForMarking)
			finalFeedback.size should be (2)
		}}}
	}


	@Test
	def finalMarkingComplete() {
		inContext[MarkingCompletedTest.Ctx] {
			withUser("cuslaj") { new CommandFixture {
				assignment.feedbacks.map(addMarkerFeedback(_, ThirdFeedback))
				assignment.feedbacks.map(addMarkerFeedback(_, SecondFeedback))
				setFinalMarkerFeedbackState(MarkingState.InProgress)
				setFirstMarkerFeedbackState(MarkingState.MarkingCompleted)
				setSecondMarkerFeedbackState(MarkingState.MarkingCompleted)

				val students = List("9876004", "0270954", "9170726")
				command.markerFeedback = assignment.feedbacks.filter(f => students.contains(f.universityId)).map(_.thirdMarkerFeedback)

				command.onBind(null)


				command.noFeedback.size should be(3)
				command.noMarks.size should be(3)

				command.applyInternal()

				val firstFeedback = assignment.feedbacks.flatMap(f => Option(f.firstMarkerFeedback))
				val completedFirstMarking = firstFeedback.filter(_.state == MarkingState.MarkingCompleted)
				completedFirstMarking.size should be(5)

				val secondFeedback = assignment.feedbacks.flatMap(f => Option(f.secondMarkerFeedback))
				val completedSecondMarking = secondFeedback.filter(_.state == MarkingState.MarkingCompleted)
				completedSecondMarking.size should be(5)

				val releasedFeedback = assignment.feedbacks.map(_.thirdMarkerFeedback).filter(_.state == MarkingState.MarkingCompleted)
				releasedFeedback.size should be(3)
			}
		}}
	}


	@Test
	def notifiesEachAffectedUser() { new MarkingNotificationFixture {

		testAssignment.markingWorkflow = new ModeratedMarkingWorkflow {
			userLookup = mockUserLookup
			firstMarkers = userGroup("marker1")
			secondMarkers = userGroup("marker2", "marker3")
		}

		val m2UserGroup = userGroup("student1", "student2")
		val m3UserGroup = userGroup("student3", "student4")

		testAssignment.secondMarkers = Seq(
			SecondMarkersMap(testAssignment, "marker2", m2UserGroup),
			SecondMarkersMap(testAssignment, "marker3", m3UserGroup)
		)

		val (f1, mf1) = makeMarkerFeedback(student1)(MarkingNotificationFixture.SecondMarkerLink)
		val (f2, mf2) = makeMarkerFeedback(student2)(MarkingNotificationFixture.SecondMarkerLink)
		val (f3, mf3) = makeMarkerFeedback(student3)(MarkingNotificationFixture.SecondMarkerLink)
		val (f4, mf4) = makeMarkerFeedback(student4)(MarkingNotificationFixture.SecondMarkerLink)

		val notifier = new SecondMarkerReleaseNotifier with MarkingCompletedState with ReleasedState with UserAware with UserLookupComponent with Logging {
			val user = marker1
			val submitter = new CurrentUser(marker1, marker1)
			val assignment = testAssignment
			val module = new Module
			newReleasedFeedback = List(mf1, mf2, mf3, mf4)
			var userLookup = mockUserLookup
		}

		val notifications = notifier.emit(())
		notifications.foreach {
			case n:ReleaseToMarkerNotification => n.userLookup = mockUserLookup
		}

		notifications.size should be(2)
		notifications(0).recipients should contain(marker3)
		notifications(0).entities should contain(mf3)
		notifications(0).entities should contain(mf4)
		notifications(1).recipients should contain(marker2)
		notifications(1).entities should contain(mf1)
		notifications(1).entities should contain(mf2)

	}}

}
