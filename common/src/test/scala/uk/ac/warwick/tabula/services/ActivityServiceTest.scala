package uk.ac.warwick.tabula.services

import org.joda.time.DateTime
import uk.ac.warwick.tabula.{Fixtures, MockUserLookup, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.{Activity, AuditEvent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.elasticsearch.{AuditEventNoteworthySubmissionsService, PagedAuditEvents}

import scala.concurrent.Future

//scalastyle:off magic.number
class ActivityServiceTest extends TestBase with Mockito {

	val service = new ActivityService

	val moduleService: ModuleAndDepartmentService = mock[ModuleAndDepartmentService]
	val submissionService: SubmissionService = mock[SubmissionService]
	val assignmentService: AssessmentService = mock[AssessmentService]
	val auditQueryService: AuditEventNoteworthySubmissionsService = mock[AuditEventNoteworthySubmissionsService]
	val userLookup = new MockUserLookup
	userLookup.registerUsers("cuscav")

	Activity.submissionService = submissionService
	Activity.userLookup = userLookup

	service.moduleService = moduleService
	service.assignmentService = assignmentService
	service.auditQueryService = auditQueryService

	@Test def noteworthySubmissionsFirstPage(): Unit = withUser("cuscav") {
		val om1 = Fixtures.module("own1")
		val om2 = Fixtures.module("own2")

		moduleService.modulesWithPermission(currentUser, Permissions.Module.ManageAssignments) returns Set(om1, om2)

		val am1 = Fixtures.module("admin1")
		val am2 = Fixtures.module("admin2")

		moduleService.modulesInDepartmentsWithPermission(currentUser, Permissions.Module.ManageAssignments) returns Set(am1, am2, om1)

		val ae1 = AuditEvent(
			eventId="event", eventType="SubmitAssignment", userId="cuscav", eventDate=DateTime.now,
			eventStage="after", data="""{"submission":"submissionId1"}""", id=100L
		)
		ae1.related = Seq(ae1)
		ae1.parsedData = Some(json.readValue(ae1.data, classOf[Map[String, Any]]))

		val ae2 = AuditEvent(
			eventId="event", eventType="SubmitAssignment", userId="cuscav", eventDate=DateTime.now,
			eventStage="after", data="""{"submission":"submissionId2"}""", id=80L
		)
		ae2.related = Seq(ae2)
		ae2.parsedData = Some(json.readValue(ae2.data, classOf[Map[String, Any]]))

		val submission1 = Fixtures.submission()
		val submission2 = Fixtures.submission()

		submissionService.getSubmission("submissionId1") returns Some(submission1)
		submissionService.getSubmission("submissionId2") returns Some(submission2)

		val pae = PagedAuditEvents(Seq(ae1, ae2), Some(ae2.eventDate), 30)

		auditQueryService.noteworthySubmissionsForModules(Seq(om1, om2, am1, am2), None, 8) returns Future.successful(pae)

		val activities = service.getNoteworthySubmissions(currentUser).futureValue
		activities.items.size should be (2)
		activities.items(0) should not be (null)
		activities.items(0).title should be ("New submission")
		activities.items(0).message should be ("")
		activities.items(0).date should be (ae1.eventDate)
		activities.items(0).agent.getUserId() should be ("cuscav")
		activities.items(0).entity should be (submission1)

		activities.items(1) should not be (null)
		activities.items(1).title should be ("New submission")
		activities.items(1).message should be ("")
		activities.items(1).date should be (ae2.eventDate)
		activities.items(1).agent.getUserId() should be ("cuscav")
		activities.items(1).entity should be (submission2)

		activities.lastUpdatedDate should be (Some(ae2.eventDate))
		activities.totalHits should be (30)
	}

	@Test def noteworthySubmissionsFollowingPages(): Unit = withUser("cuscav") {
		val om1 = Fixtures.module("own1")
		val om2 = Fixtures.module("own2")

		moduleService.modulesWithPermission(currentUser, Permissions.Module.ManageAssignments) returns Set(om1, om2)

		val am1 = Fixtures.module("admin1")
		val am2 = Fixtures.module("admin2")

		moduleService.modulesInDepartmentsWithPermission(currentUser, Permissions.Module.ManageAssignments) returns Set(am1, am2, om1)

		val ae1 = AuditEvent(
			eventId="event", eventType="SubmitAssignment", userId="cuscav", eventDate=DateTime.now,
			eventStage="after", data="""{"submission":"submissionId1"}""", id=100L
		)
		ae1.related = Seq(ae1)
		ae1.parsedData = Some(json.readValue(ae1.data, classOf[Map[String, Any]]))

		val ae2 = AuditEvent(
			eventId="event", eventType="SubmitAssignment", userId="cuscav", eventDate=DateTime.now,
			eventStage="after", data="""{"submission":"submissionId2"}""", id=80L
		)
		ae2.related = Seq(ae2)
		ae2.parsedData = Some(json.readValue(ae2.data, classOf[Map[String, Any]]))

		val submission1 = Fixtures.submission()
		val submission2 = Fixtures.submission()

		submissionService.getSubmission("submissionId1") returns Some(submission1)
		submissionService.getSubmission("submissionId2") returns Some(submission2)

		val pae = PagedAuditEvents(Seq(ae1, ae2), Some(ae2.eventDate), 30)

		auditQueryService.noteworthySubmissionsForModules(Seq(om1, om2, am1, am2), Some(ae2.eventDate), 8) returns Future.successful(pae)

		val activities = service.getNoteworthySubmissions(currentUser, ae2.eventDate).futureValue
		activities.items.size should be (2)
		activities.items(0) should not be (null)
		activities.items(0).title should be ("New submission")
		activities.items(0).message should be ("")
		activities.items(0).date should be (ae1.eventDate)
		activities.items(0).agent.getUserId() should be ("cuscav")
		activities.items(0).entity should be (submission1)

		activities.items(1) should not be (null)
		activities.items(1).title should be ("New submission")
		activities.items(1).message should be ("")
		activities.items(1).date should be (ae2.eventDate)
		activities.items(1).agent.getUserId() should be ("cuscav")
		activities.items(1).entity should be (submission2)

		activities.lastUpdatedDate should be (Some(ae2.eventDate))
		activities.totalHits should be (30)
	}

}