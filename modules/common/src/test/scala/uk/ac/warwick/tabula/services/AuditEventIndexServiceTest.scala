package uk.ac.warwick.tabula.services

import org.scalatest.time.{Millis, Seconds, Span}
import uk.ac.warwick.tabula._
import org.junit.After
import org.junit.Before
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.{Assignment, Submission}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.userlookup.User
import collection.JavaConversions._
import uk.ac.warwick.util.core.StopWatch
import java.io.File
import uk.ac.warwick.tabula.events.Event
import uk.ac.warwick.tabula.commands.Command
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Department
import org.apache.commons.io.FileUtils
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.model.AuditEvent
import org.hibernate.dialect.HSQLDialect
import uk.ac.warwick.tabula.data.{AuditEventDaoImpl, AbstractAuditEventDao, AuditEventDaoComponent, SessionComponent}

// scalastyle:off magic.number
class AuditEventIndexServiceTest extends PersistenceTestBase with Mockito with Logging {

	override implicit val patienceConfig =
		PatienceConfig(timeout = Span(2, Seconds), interval = Span(50, Millis))

	var indexer: AuditEventIndexService = _

	val auditEventDao = new AbstractAuditEventDao with SessionComponent {
		override def session = AuditEventIndexServiceTest.this.session
	}

	val service: AuditEventServiceImpl = new AuditEventServiceImpl with AuditEventDaoComponent {
		val auditEventDao = AuditEventIndexServiceTest.this.auditEventDao
	}

	var TEMP_DIR:File = _

	@Before def setup {
		TEMP_DIR = createTemporaryDirectory
		indexer = new AuditEventIndexService
		indexer.service = service
		indexer.indexPath = TEMP_DIR
		indexer.features = new FeaturesImpl
		indexer.features.searchOnApiComponent = false
		indexer.afterPropertiesSet
		auditEventDao.dialect = new HSQLDialect()
	}

	@After def tearDown {
		indexer.destroy()
		session.newSQLQuery("delete from auditevent").executeUpdate()
		try {
			FileUtils.deleteDirectory(TEMP_DIR)
		} catch {
			// windows holds onto a lock on some index files longer than it should. Don't fail over this
			case e:java.io.IOException => logger.warn(e.getMessage)
		}
	}

	/**
	 * Check that when you download submissions, they are shown
	 * by adminDownloadedSubmissions(Assignment).
	 */
	@Transactional
	@Test def downloadedSubmissions = withFakeTime(dateTime(2001, 6)) {
		val assignment = {
			val a = newDeepAssignment()
			a.id = "12345"
			val s1 = new Submission
			s1.submittedDate = new DateTime().minusHours(5)
			a.submissions add s1
			a
		}

		val command = new NullCommand {
			override lazy val eventName = "DownloadAllSubmissions"

			override def describe(d: Description) = d
				.assignment(assignment)
				.submissions(assignment.submissions)
				.studentIds(assignment.submissions.map(_.universityId))
				.properties(
						"submissionCount" -> Option(assignment.submissions).map(_.size).getOrElse(0))
		}

		val auditEvent = recordAudit(command)

		indexer.adminDownloadedSubmissions(assignment).futureValue should be ('empty)
		indexer.incrementalIndex
		indexer.adminDownloadedSubmissions(assignment).futureValue should be (assignment.submissions.toList)

	}

	@Transactional
	@Test def individuallyDownloadedSubmissions = withFakeTime(dateTime(1999, 6)) {
		val assignment = {
			val a = newDeepAssignment()
			a.id = "54321"
			val s1 = new Submission
			s1.id = "321"
			s1.assignment = a
			s1.submittedDate = new DateTime().minusHours(5)
			a.submissions add s1
			a
		}

		val command = new NullCommand {
			override lazy val eventName = "AdminGetSingleSubmission"

			override def describe(d: Description) = {
				def submission = assignment.submissions.head

				d.submission(submission).properties(
						"studentId" -> submission.universityId,
						"attachmentCount" -> submission.allAttachments.size)
			}

		}

		val auditEvent = recordAudit(command)

		indexer.adminDownloadedSubmissions(assignment).futureValue should be ('empty)
		indexer.incrementalIndex()
		indexer.adminDownloadedSubmissions(assignment).futureValue should be (assignment.submissions.toList)
	}

	def recordAudit(command:Command[_]) = {
		val event = Event.fromDescribable(command)
		service.save(event, "before")
		service.getByEventId(event.id)
	}

	def addParsedData(event:AuditEvent) = {
		event.parsedData = service.parseData(event.data)
		event
	}

	@Transactional
	@Test def createdDate = withFakeTime(dateTime(2000, 6)) {

		val eventId = "a"
		val eventType = "AddAssignment"
		val userId = "bob"
		val d = new DateTime(2000,1,1,0,0,0)

		val before = AuditEvent(
			eventId=eventId, eventType=eventType, userId=userId, eventDate=d,
			eventStage="before", data="""{}"""
		)
		val after = AuditEvent(
			eventId=eventId, eventType=eventType, userId=userId, eventDate=d,
			eventStage="after", data="""{"assignment":"12345"}"""
		)

		for (event <- Seq(before,after)) service.save(addParsedData(event))
		//indexer.indexEvents(Seq(before))
		indexer.incrementalIndex

		val assignment = new Assignment()
		assignment.createdDate = null // Set null date otherwise the search will get restricted
		assignment.id = "12345"

		val maybeDate = indexer.getAssignmentCreatedDate(assignment)
		maybeDate.futureValue should be (Some(d))

	}

	@Transactional
	@Test def index = withFakeTime(dateTime(2000, 6)) {
		val stopwatch = new StopWatch

		val jsonData = Map(
					"students" -> Array("0123456", "0199999")
				)
		val jsonDataString = json.writeValueAsString(jsonData)

		stopwatch.start("creating items")

		val defendEvents = for (i <- 1 to 1000)
			yield AuditEvent(
				eventId="d"+i, eventType="DefendBase", eventStage="before", userId="jim",
				eventDate=new DateTime(2000,1,2,0,0,0).plusSeconds(i),
				data="{}"
			)

		val publishEvents = for (i <- 1 to 20)
			yield AuditEvent(
				eventId="s"+i, eventType="PublishFeedback", eventStage="before", userId="bob",
				eventDate=new DateTime(2000,1,1,0,0,0).plusSeconds(i),
				data=jsonDataString
			)

		stopwatch.stop()

		val events = defendEvents ++ publishEvents
		events.foreach { event =>
			service.save(addParsedData(event))
		}

		service.listNewerThan(new DateTime(2000,1,1,0,0,0), 100).size should be (100)

		stopwatch.start("indexing")

		// we only index 1000 at a time, so index twice to get all the latest stuff.
		indexer.incrementalIndex
		indexer.incrementalIndex

		stopwatch.stop()

		val user = new User("jeb")
		user.setWarwickId("0123456")

		indexer.listRecent(0, 1000).futureValue.size should be (1000)

		indexer.student(user).futureValue.size should be (20)

		val moreEvents = {
			val events = Seq(addParsedData(AuditEvent(
				eventId="x9000", eventType="PublishFeedback", eventStage="before", userId="bob",
				eventDate=new DateTime(2000,1,1,0,0,0).plusSeconds(9000),
				data=jsonDataString
			)))
			events.foreach { service.save(_) }
			service.getByEventId("x9000")
		}
		indexer.indexItems(moreEvents)

		indexer.student(user).futureValue.size should be (21)

		indexer.listRecent(0, 13).futureValue.size should be (13)

		indexer.openQuery("eventType:PublishFeedback", 0, 100).futureValue.size should be (21)

		// First query is slowest, but subsequent queries quickly drop
		// to a much smaller time
		for (i <- 1 to 20) {
			stopwatch.start("searching for newest item forever attempt "+i)
			val newest = indexer.newest()
			newest.futureValue.head.getValues("eventId").toList.head should be ("d1000")
			stopwatch.stop()
		}

		// index again to check that it doesn't do any once-only stuff
		indexer.incrementalIndex

	}

	@Transactional
	@Test def pagingSearch = withFakeTime(dateTime(2010, 6)) {
		val stopwatch = new StopWatch

		val dept = new Department
		dept.code = "zx"

		val assignment = new Assignment
		assignment.id = "4a0ce216-adda-b0b0-c0c0-000000000000"
		// Merry Christmas
		assignment.closeDate = new DateTime(2009,12,25,0,0,0)

		val module = new Module
		module.id = "367a9abd-adda-c0c0-b0b0-000000000000"
		module.assignments = List(assignment)
		module.adminDepartment = dept

		val beforeJsonData = json.writeValueAsString(Map(
					"assignment" -> assignment.id,
					"module" -> module.id,
					"department" -> dept.code
				))

		val afterJsonData = json.writeValueAsString(Map(
					"assignment" -> assignment.id,
					"module" -> module.id,
					"department" -> dept.code,
					"submission" -> "94624c3b-adda-0dd0-b0b0-REPLACE-THIS"
				))

		val afterLateJsonData = json.writeValueAsString(Map(
					"assignment" -> assignment.id,
					"module" -> module.id,
					"department" -> dept.code,
					"submission" -> "94624c3b-adda-0dd0-b0b0-REPLACE-THIS",
					"submissionIsNoteworthy" -> true
				))

		stopwatch.start("creating items")

		val submitBefore = for (i <- 1 to 70)
			yield AuditEvent(
				eventId="ontime"+i, eventType="SubmitAssignment", eventStage="before", userId="bob",
				eventDate=new DateTime(2009,12,1,0,0,0).plusSeconds(i),
				data=beforeJsonData
			)

		val submitAfter = for (i <- 1 to 70)
			yield AuditEvent(
				eventId="ontime"+i, eventType="SubmitAssignment", eventStage="after", userId="bob",
				eventDate=new DateTime(2009,12,1,0,0,0).plusSeconds(i),
				data=afterLateJsonData.replace("REPLACE-THIS", "%012d".format(i))
			)

		val submitBeforeLate = for (i <- 1 to 70)
			yield AuditEvent(
				eventId="late"+i, eventType="SubmitAssignment", eventStage="before", userId="bob",
				eventDate=new DateTime(2010,1,1,0,0,0).plusSeconds(i),
				data=beforeJsonData
			)

		val submitAfterLate = for (i <- 1 to 70)
			yield AuditEvent(
				eventId="late"+i, eventType="SubmitAssignment", eventStage="after", userId="bob",
				eventDate=new DateTime(2010,1,1,0,0,0).plusSeconds(i),
				data=afterJsonData.replace("REPLACE-THIS", "%012d".format(i))
			)

		stopwatch.stop()

		val events = submitBefore ++ submitAfter ++ submitBeforeLate ++ submitAfterLate
		events.foreach { event =>
			service.save(addParsedData(event))
		}

		// 140 total distinct events
		service.listNewerThan(new DateTime(2009,12,1,0,0,0), 500).size should be (140)

		// 70 new ones, since Christmas
		service.listNewerThan(new DateTime(2009,12,25,0,0,0), 500).size should be (70)

		stopwatch.start("indexing")
		indexer.incrementalIndex
		stopwatch.stop()

		// check pager
		val paged0 = indexer.submissionsForModules(Seq(module), None, None, 100)
		paged0.items.length should be (100)

		val paged1 = indexer.submissionsForModules(Seq(module), paged0.last, Some(paged0.token), 100)
		// asked to batch in 100s, but only 40 left
		paged1.items.length should be (40)

		// check pager for noteworthy submissions
		val paged2 = indexer.noteworthySubmissionsForModules(Seq(module), None, None, 100)
		paged2.items.length should be (70)

		// Find by user ID
		indexer.findByUserId("bob").futureValue.size should be (140)
		indexer.findByUserId("fred").futureValue.size should be (0)

		val beforeFeedbackJsonData = json.writeValueAsString(Map(
					"assignment" -> assignment.id,
					"module" -> module.id,
					"department" -> dept.code
				))

		val afterFeedbackJsonData = json.writeValueAsString(Map(
					"assignment" -> assignment.id,
					"module" -> module.id,
					"department" -> dept.code,
					"feedback" -> "94624c3b-adda-0dd0-b0b0-REPLACE-THIS"
				))

		val feedbackBefore = for (i <- 1 to 70)
			yield AuditEvent(
				eventId="ontime"+i, eventType="PublishFeedback", eventStage="before", userId="bob",
				eventDate=new DateTime(2008,12,1,0,0,0).plusSeconds(i),
				data=beforeFeedbackJsonData
			)

		val feedbackAfter = for (i <- 1 to 70)
			yield AuditEvent(
				eventId="ontime"+i, eventType="PublishFeedback", eventStage="after", userId="bob",
				eventDate=new DateTime(2008,12,1,0,0,0).plusSeconds(i),
				data=afterFeedbackJsonData.replace("REPLACE-THIS", "%012d".format(i))
			)

		val fevents = feedbackBefore ++ feedbackAfter
		fevents.foreach { event =>
			service.save(addParsedData(event))
		}

		indexer.findPublishFeedbackEvents(dept).futureValue.length should be (0)
		indexer.findPublishFeedbackEvents(Fixtures.department("in")).futureValue.length should be (0)
	}

}
