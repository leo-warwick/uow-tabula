package uk.ac.warwick.tabula.data.model.notifications.cm2

import org.hibernate.SessionFactory
import org.junit.Before
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.data.model.Assignment.MarkerAllocation
import uk.ac.warwick.tabula.data.model.{Department, FirstMarkersMap, Notification, UserGroup}
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.SingleMarker
import uk.ac.warwick.tabula.data.model.markingworkflow.SingleMarkerWorkflow
import uk.ac.warwick.tabula.web.views.UserLookupTag
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.CM2MarkingWorkflowDaoImpl
import uk.ac.warwick.userlookup.{AnonymousUser, User}
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.JavaImports.{JHashMap, JList}
import uk.ac.warwick.tabula.services._

import collection.JavaConverters._

class ReleaseToMarkerNotificationTest extends PersistenceTestBase with Mockito {

	var assignmentMembershipService: AssessmentMembershipService = _
	var userLookup: UserLookupService = _
	val cm2MarkingWorkflowService: CM2MarkingWorkflowService = mock[CM2MarkingWorkflowService]
	var group: UserGroup = UserGroup.ofUsercodes
	val mockSessionFactory: SessionFactory = smartMock[SessionFactory]

	group.addUserId("1170836")
	group.addUserId("1170837")


	//	userLookup.registerUsers("1170836", "1170837", "1000001")
	val stu1 = Fixtures.user(universityId = "1000001", userId = "1000001")

	val dao = new CM2MarkingWorkflowDaoImpl

	val dept: Department = Fixtures.department("in")
	val marker: User = Fixtures.user("1170836", "1170836")
	val marker2: User = Fixtures.user("1170837", "1170837")


	var userDatabase: Seq[User] = Seq(
		("1170836", "1170836"),
		("1170837", "1170837"),
		("1000001", "1000001")
	) map { case (id, code) =>
		val user = new User(code)
		user.setWarwickId(id)
		user.setFullName("Roger " + code.head.toUpper + code.tail)
		user
	}

	userLookup = smartMock[UserLookupService]
	userLookup.getUserByUserId(any[String]) answers { id =>
		userDatabase find {
			_.getUserId == id
		} getOrElse new AnonymousUser()
	}
	userLookup.getUserByWarwickUniId(any[String]) answers { id =>
		userDatabase find {
			_.getWarwickId == id
		} getOrElse new AnonymousUser()
	}
	userLookup.getUsersByUserIds(any[JList[String]]) answers { ids =>
		val users = ids.asInstanceOf[JList[String]].asScala.map(id => (id, userDatabase find {
			_.getUserId == id
		} getOrElse new AnonymousUser()))
		JHashMap(users: _*)
	}
	userLookup.getUsersByWarwickUniIds(any[Seq[String]]) answers { ids =>
		ids.asInstanceOf[Seq[String]].map(id => (id, userDatabase.find {
			_.getWarwickId == id
		}.getOrElse(new AnonymousUser()))).toMap
	}
	val profileService = smartMock[ProfileService]
	profileService.getAllMembersWithUniversityIds(any[Seq[String]]) returns Seq()
	assignmentMembershipService = {
		val s = new AssessmentMembershipServiceImpl
		s.userLookup = userLookup
		s.profileService = profileService
		s
	}

	@Before
	def setup(): Unit = transactional { tx =>
		dao.sessionFactory = sessionFactory
		group.sessionFactory = sessionFactory

		group.userLookup = userLookup
		session.save(dept)
		session.save(group)
		session.flush()
	}

	@Test
	def ss(): Unit = withUser("1170836") {
		transactional { t =>

			val singleMarkerWorkflow: SingleMarkerWorkflow = SingleMarkerWorkflow("testAssignment", dept, Seq(marker, marker2))
			singleMarkerWorkflow.academicYear = AcademicYear(2016)

			dao.saveOrUpdate(singleMarkerWorkflow)
			singleMarkerWorkflow.stageMarkers.forEach { markers =>
				markers.cm2MarkingWorkflowService = Some(cm2MarkingWorkflowService)
			}

			session.flush()

			val assignment = Fixtures.assignment("test")
			assignment.cm2Assignment = true
			assignment.userLookup = userLookup
			assignment.cm2MarkingWorkflow = singleMarkerWorkflow


			assignment.firstMarkers.addAll(Seq(
				FirstMarkersMap(assignment, "1170837", group),
				FirstMarkersMap(assignment, "1170836", group),
			).asJava)

			val feedback = Fixtures.assignmentFeedback("1000001", "1000001")
			feedback.assignment = assignment
			val markerFeedback = Fixtures.markerFeedback(feedback)
			markerFeedback.marker = marker
			markerFeedback.stage = SingleMarker

			val notification = Notification.init(new ReleaseToMarkerNotification, currentUser.apparentUser, markerFeedback, assignment)

			notification.allocatedFirstMarker.size should be(1)

		}
	}

}
