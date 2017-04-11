package uk.ac.warwick.tabula.data.convert
import org.hibernate.SessionFactory
import org.hibernate.Session

import uk.ac.warwick.tabula.Mockito

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.{OldStudentsChooseMarkerWorkflow, SeenSecondMarkingLegacyWorkflow, MarkingWorkflow}

class MarkingWorkflowIdConverterTest extends TestBase with Mockito {

	val converter = new MarkingWorkflowIdConverter

	val sessionFactory: SessionFactory = mock[SessionFactory]
	val session: Session = mock[Session]

	sessionFactory.getCurrentSession returns session

	converter.sessionFactory = sessionFactory

	@Test def validInput() {
		val workflow = new SeenSecondMarkingLegacyWorkflow
		workflow.id = "steve"

		session.get(classOf[MarkingWorkflow].getName, "steve") returns workflow

		converter.convertRight("steve") should be (workflow)
	}

	@Test def invalidInput() {
		session.get(classOf[MarkingWorkflow].getName, "20X6") returns null

		converter.convertRight("20X6") should be (null)
		converter.convertRight(null) should be (null)
	}

	@Test def formatting() {
		val workflow = new OldStudentsChooseMarkerWorkflow
		workflow.id = "steve"

		converter.convertLeft(workflow) should be ("steve")
		converter.convertLeft(null) should be (null)
	}

}