package uk.ac.warwick.tabula.data.model

import org.joda.time.DateTime
import uk.ac.warwick.tabula.JsonObjectMapperFactory
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.services.SubmissionService
import uk.ac.warwick.tabula.MockUserLookup
import uk.ac.warwick.tabula.Fixtures

class ActivityTest extends TestBase with Mockito {

	val submissionService: SubmissionService = mock[SubmissionService]
	val userLookup = new MockUserLookup
	userLookup.registerUsers("cuscav")

	Activity.submissionService = submissionService
	Activity.userLookup = userLookup

	@Test def fromEventNotSubmission {
		val event = AuditEvent(
			eventId="event", eventType="AddAssignment", userId="cuscav", eventDate=DateTime.now,
			eventStage="after", data="""{}"""
		)
		event.related = Seq(event)
		event.parsedData = Some(json.readValue(event.data, classOf[Map[String, Any]]))

		Activity(event) should be ('empty)
	}

	@Test def fromSubmission {
		val submission = Fixtures.submission()
		submissionService.getSubmission("submissionId") returns Some(submission)

		val event = AuditEvent(
			eventId="event", eventType="SubmitAssignment", userId="cuscav", eventDate=DateTime.now,
			eventStage="after", data="""{"submission": "submissionId"}"""
		)
		event.related = Seq(event)
		event.parsedData = Some(json.readValue(event.data, classOf[Map[String, Any]]))

		Activity(event) should be ('defined)
		Activity(event) map { activity =>
			activity.title should be ("New submission")
			activity.message should be ("")
			activity.date should be (event.eventDate)
			activity.agent.getUserId() should be ("cuscav")
			activity.entity should be (submission)
		}
	}

	@Test def fromSubmissionNotFound {
		submissionService.getSubmission("submissionId") returns None

		val event = AuditEvent(
			eventId="event", eventType="SubmitAssignment", userId="cuscav", eventDate=DateTime.now,
			eventStage="after", data="""{"submission": "submissionId"}"""
		)
		event.related = Seq(event)
		event.parsedData = Some(json.readValue(event.data, classOf[Map[String, Any]]))

		Activity(event) should be ('defined)
		Activity(event) map { activity =>
			activity.title should be ("New submission (since deleted)")
			activity.message should be ("")
			activity.date should be (event.eventDate)
			activity.agent.getUserId() should be ("cuscav")
			activity.entity should be (Nil)
		}
	}

}