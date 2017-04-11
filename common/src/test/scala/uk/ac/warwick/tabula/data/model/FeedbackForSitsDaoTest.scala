package uk.ac.warwick.tabula.data.model

import org.junit.Before
import uk.ac.warwick.tabula.data.FeedbackForSitsDaoImpl
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.{MockUserLookup, Fixtures, Mockito, PersistenceTestBase}

class FeedbackForSitsDaoTest extends PersistenceTestBase with Mockito {

	trait Environment {
		val feedbackForSitsDao = new FeedbackForSitsDaoImpl
		val mockUserLookup = new MockUserLookup

		feedbackForSitsDao.sessionFactory = sessionFactory

		SSOUserType.userLookup = smartMock[UserLookupService]
	}

	@Test
	def testIt = withUser("0070790", "cusdx") {
		new Environment {
			val assignment: Assignment = Fixtures.assignment("some assignment")

			val feedback: AssignmentFeedback = Fixtures.assignmentFeedback("1234567")
			feedback.assignment = assignment

			val feedbackForSits = new FeedbackForSits
			feedbackForSits.init(feedback, currentUser.apparentUser)

			transactional { tx =>
				session.saveOrUpdate(assignment)
				session.saveOrUpdate(feedback)

				feedbackForSitsDao.saveOrUpdate(feedbackForSits)
				session.flush()
				session.clear()

				feedbackForSitsDao.getByFeedback(feedback) should be(Some(feedbackForSits))

				feedbackForSitsDao.feedbackToLoad should be(Seq(feedbackForSits))
		}
		}
	}


}
