package uk.ac.warwick.tabula.data.convert
import org.hibernate.SessionFactory
import org.hibernate.Session

import uk.ac.warwick.tabula.Mockito

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.FeedbackTemplate

class FeedbackTemplateIdConverterTest extends TestBase with Mockito {

	val converter = new FeedbackTemplateIdConverter

	val sessionFactory: SessionFactory = mock[SessionFactory]
	val session: Session = mock[Session]

	sessionFactory.getCurrentSession() returns (session)

	converter.sessionFactory = sessionFactory

	@Test def validInput {
		val template = new FeedbackTemplate
		template.id = "steve"

		session.get(classOf[FeedbackTemplate].getName(), "steve") returns (template)

		converter.convertRight("steve") should be (template)
	}

	@Test def invalidInput {
		session.get(classOf[FeedbackTemplate].getName(), "20X6") returns (null)

		converter.convertRight("20X6") should be (null)
		converter.convertRight(null) should be (null)
	}

	@Test def formatting {
		val template = new FeedbackTemplate
		template.id = "steve"

		converter.convertLeft(template) should be ("steve")
		converter.convertLeft(null) should be (null)
	}

}