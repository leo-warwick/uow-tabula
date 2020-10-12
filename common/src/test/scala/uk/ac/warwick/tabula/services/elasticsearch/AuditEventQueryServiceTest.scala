package uk.ac.warwick.tabula.services.elasticsearch

import java.util.UUID

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.Index
import org.joda.time.DateTime
import org.junit.{After, Before}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Stopwatches._
import uk.ac.warwick.tabula.services.AuditEventService
import uk.ac.warwick.tabula.{ElasticsearchTestBase, Fixtures, MockUserLookup, Mockito}

import scala.collection.immutable.IndexedSeq
import scala.jdk.CollectionConverters._

class AuditEventQueryServiceTest extends ElasticsearchTestBase with Mockito {

  val index = Index("audit")

  private trait Fixture {
    val queryService = new AuditEventQueryServiceImpl
    queryService.userLookup = new MockUserLookup()
    queryService.auditEventService = smartMock[AuditEventService]
    queryService.client = AuditEventQueryServiceTest.this.client
    queryService.indexName = AuditEventQueryServiceTest.this.index.name

    queryService.auditEventService.parseData(any[String]) answers { data: Any =>
      try {
        Option(data.asInstanceOf[String]).map {
          json.readValue(_, classOf[Map[String, Any]])
        }
      } catch {
        case e@(_: JsonParseException | _: JsonMappingException) => None
      }
    }

    implicit val indexable: ElasticsearchIndexable[AuditEvent] = AuditEventIndexService.auditEventIndexable(queryService.auditEventService)
  }

  @Before def setUp(): Unit = {
    new AuditEventElasticsearchConfig {
      client.execute {
        createIndex(index.name).mapping(properties(fields)).analysis(analysis)
      }.await.result.acknowledged should be(true)
    }
    blockUntilIndexExists(index.name)
  }

  @After def tearDown(): Unit = {
    deleteIndex(index.name)
    blockUntilIndexNotExists(index.name)
  }

  /**
    * Check that when you download submissions, they are shown
    * by adminDownloadedSubmissions(Assignment).
    */
  @Test def downloadedSubmissions(): Unit = withFakeTime(dateTime(2001, 6)) {
    new Fixture {
      val assignment: Assignment = {
        val a = newDeepAssignment()
        a.id = "12345"
        val s1 = new Submission
        s1.id = "submissionId1"
        s1.submittedDate = new DateTime().minusHours(5)
        a.submissions add s1
        a
      }

      val data: Map[String, Any] = Map(
        "assignment" -> assignment.id,
        "submissions" -> Seq("submissionId1"),
        "students" -> Seq("1234567"),
        "submissionCount" -> 1
      )

      val auditEvent = AuditEvent(
        id = 1,
        eventId = UUID.randomUUID.toString,
        eventDate = DateTime.now(),
        eventType = "DownloadAllSubmissions",
        eventStage = "before",
        data = json.writeValueAsString(data),
        parsedData = Some(data)
      )
      auditEvent.related = Seq(auditEvent)

      queryService.auditEventService.getByIds(Seq(1)) returns Seq(auditEvent)

      queryService.adminDownloadedSubmissions(assignment, assignment.submissions.asScala.toSeq).futureValue should be(Symbol("empty"))

      // Index the audit event
      client.execute {
        indexInto(index).source(auditEvent).id(auditEvent.id.toString)
      }
      blockUntilCount(1, index.name)

      queryService.adminDownloadedSubmissions(assignment, assignment.submissions.asScala.toSeq).futureValue should be(assignment.submissions.asScala)
    }
  }

  @Test def individuallyDownloadedSubmissions(): Unit = withFakeTime(dateTime(2001, 6)) {
    new Fixture {
      val assignment: Assignment = {
        val a = newDeepAssignment()
        a.id = "54321"
        a.createdDate = new DateTime().minusHours(10)
        val s1 = new Submission
        s1.id = "321"
        s1.assignment = a
        s1.submittedDate = new DateTime().minusHours(5)
        a.submissions add s1
        a
      }

      val data: Map[String, Any] = Map(
        "assignment" -> assignment.id,
        "submission" -> "321",
        "student" -> "1234567",
        "attachmentCount" -> 0
      )

      val auditEvent = AuditEvent(
        id = 1,
        eventId = UUID.randomUUID.toString,
        eventDate = DateTime.now(),
        eventType = "AdminGetSingleSubmission",
        eventStage = "before",
        data = json.writeValueAsString(data),
        parsedData = Some(data)
      )
      auditEvent.related = Seq(auditEvent)

      queryService.auditEventService.getByIds(Seq(1)) returns Seq(auditEvent)

      queryService.adminDownloadedSubmissions(assignment, assignment.submissions.asScala.toSeq).futureValue should be(Symbol("empty"))

      // Index the audit event
      client.execute {
        indexInto(index).source(auditEvent).id(auditEvent.id.toString)
      }
      blockUntilCount(1, index.name)

      queryService.adminDownloadedSubmissions(assignment, assignment.submissions.asScala.toSeq).futureValue should be(assignment.submissions.asScala)
    }
  }

  @Test def noteworthySubmissions(): Unit = withFakeTime(dateTime(2001, 6)) {
    new Fixture {
      val stopwatch = StopWatch()

      val dept = Fixtures.department("zx")

      val assignment = new Assignment
      assignment.id = "4a0ce216-adda-b0b0-c0c0-000000000000"
      // Merry Christmas
      assignment.closeDate = new DateTime(2009, 12, 25, 0, 0, 0)

      val module = new Module
      module.id = "367a9abd-adda-c0c0-b0b0-000000000000"
      module.assignments = List(assignment).asJava
      module.adminDepartment = dept

      val beforeJsonData: String = json.writeValueAsString(Map(
        "assignment" -> assignment.id,
        "module" -> module.id,
        "department" -> dept.code
      ))

      val afterJsonData: String = json.writeValueAsString(Map(
        "assignment" -> assignment.id,
        "module" -> module.id,
        "department" -> dept.code,
        "submission" -> "94624c3b-adda-0dd0-b0b0-REPLACE-THIS"
      ))

      val afterLateJsonData: String = json.writeValueAsString(Map(
        "assignment" -> assignment.id,
        "module" -> module.id,
        "department" -> dept.code,
        "submission" -> "94624c3b-adda-0dd0-b0b0-REPLACE-THIS",
        "submissionIsNoteworthy" -> true
      ))

      stopwatch.start("creating items")

      val submitBefore: IndexedSeq[AuditEvent] = for (i <- 1 to 70)
        yield AuditEvent(
          eventId = "ontime" + i, eventType = "SubmitAssignment", eventStage = "before", userId = "bob",
          eventDate = new DateTime(2009, 12, 1, 0, 0, 0).plusSeconds(i),
          data = beforeJsonData
        )

      val submitAfter: IndexedSeq[AuditEvent] = for (i <- 1 to 70)
        yield AuditEvent(
          eventId = "ontime" + i, eventType = "SubmitAssignment", eventStage = "after", userId = "bob",
          eventDate = new DateTime(2009, 12, 1, 0, 0, 0).plusMinutes(5).plusSeconds(i),
          data = afterLateJsonData.replace("REPLACE-THIS", "%012d".format(i))
        )

      val submitBeforeLate: IndexedSeq[AuditEvent] = for (i <- 1 to 70)
        yield AuditEvent(
          eventId = "late" + i, eventType = "SubmitAssignment", eventStage = "before", userId = "bob",
          eventDate = new DateTime(2010, 1, 1, 0, 0, 0).plusSeconds(i),
          data = beforeJsonData
        )

      val submitAfterLate: IndexedSeq[AuditEvent] = for (i <- 1 to 70)
        yield AuditEvent(
          eventId = "late" + i, eventType = "SubmitAssignment", eventStage = "after", userId = "bob",
          eventDate = new DateTime(2010, 1, 1, 0, 0, 0).plusMinutes(5).plusSeconds(i),
          data = afterJsonData.replace("REPLACE-THIS", "%012d".format(i))
        )

      stopwatch.stop()

      val events: IndexedSeq[AuditEvent] = submitBefore ++ submitAfter ++ submitBeforeLate ++ submitAfterLate
      events.foreach { event =>
        event.parsedData = Option(event.data).map {
          json.readValue(_, classOf[Map[String, Any]])
        }
      }

      val beforeEvents: IndexedSeq[AuditEvent] = events.filter(_.eventStage == "before")

      beforeEvents.zipWithIndex.foreach { case (auditEvent, i) =>
        auditEvent.id = i
        auditEvent.related = events.filter(_.eventId == auditEvent.eventId)
      }

      queryService.auditEventService.getByIds(any[Seq[Long]]) answers { ids: Any =>
        ids.asInstanceOf[Seq[Long]].flatMap { id => beforeEvents.find {
          _.id == id
        }
        }
      }

      stopwatch.start("indexing")

      // Index the audit event
      beforeEvents.foreach { auditEvent =>
        client.execute {
          indexInto(index).source(auditEvent).id(auditEvent.id.toString)
        }
      }

      blockUntilExactCount(140, index.name)

      stopwatch.stop()

      val paged0: PagedAuditEvents = queryService.submissionsForModules(Seq(module), None, 100).futureValue
      paged0.items.length should be(100)
      paged0.totalHits should be(140)

      val paged1: PagedAuditEvents = queryService.submissionsForModules(Seq(module), paged0.lastUpdatedDate, 100).futureValue
      // asked to batch in 100s, but only 40 left
      paged1.items.length should be(40)
      paged1.totalHits should be(40) // This excludes anything before lastUpdatedDate, which may be confusing.

      // check pager for noteworthy submissions
      val paged2: PagedAuditEvents = queryService.noteworthySubmissionsForModules(Seq(module), None, 100).futureValue
      paged2.items.length should be(70)
      paged2.totalHits should be(70)
    }
  }

  @Test def latestOnlineViews(): Unit = withFakeTime(dateTime(2001, 6)) {
    new Fixture {
      // Specifically this verifies the fix to TAB-6888 and its predecessor TAB-4168 where the query doesn't fetch all terms

      val dept = Fixtures.department("zx")

      val assignment = new Assignment
      assignment.id = "4a0ce216-adda-b0b0-c0c0-000000000000"

      // Merry Christmas
      assignment.closeDate = new DateTime(2009, 12, 25, 0, 0, 0)

      val module = new Module
      module.id = "367a9abd-adda-c0c0-b0b0-000000000000"
      module.assignments = List(assignment).asJava
      module.adminDepartment = dept

      val eventData: String = json.writeValueAsString(Map(
        "assignment" -> assignment.id,
        "module" -> module.id,
        "department" -> dept.code
      ))

      val studentUsercodes = (1 to 20).map { i => f"u00000$i%02d" }

      val events: IndexedSeq[AuditEvent] = (1 to 70).map { i =>
        AuditEvent(
          eventId = "viewed" + i, eventType = "ViewOnlineFeedback", eventStage = "before", userId = studentUsercodes(i % 20), masqueradeUserId = studentUsercodes(i % 20),
          eventDate = new DateTime(2009, 12, 30, 0, 0, 0).plusSeconds(i),
          data = eventData
        )
      }

      events.zipWithIndex.foreach { case (auditEvent, i) =>
        auditEvent.id = i
        auditEvent.related = events.filter(_.eventId == auditEvent.eventId)
        auditEvent.parsedData = Option(auditEvent.data).map {
          json.readValue(_, classOf[Map[String, Any]])
        }
      }

      queryService.auditEventService.getByIds(any[Seq[Long]]) answers { ids: Any =>
        ids.asInstanceOf[Seq[Long]].flatMap { id => events.find {
          _.id == id
        }
        }
      }

      // Index the audit event
      events.foreach { auditEvent =>
        client.execute {
          indexInto(index).source(auditEvent).id(auditEvent.id.toString)
        }
      }

      blockUntilExactCount(70, index.name)

      val feedback: Seq[Feedback] = studentUsercodes.map { usercode =>
        val f = new Feedback
        f.usercode = usercode
        f.released = true
        f.releasedDate = assignment.closeDate
        f
      }

      queryService.userLookup.asInstanceOf[MockUserLookup].registerUsers(studentUsercodes: _*)

      val views = queryService.latestOnlineFeedbackViews(assignment, feedback).futureValue
      views.size should be(20) // If this is 10, need to increase the size of the aggregation!

      // Check that we're returning the latest date
      views(queryService.userLookup.getUserByUserId("u0000001")) should be(new DateTime(2009, 12, 30, 0, 0, 0).plusSeconds(60))
      views(queryService.userLookup.getUserByUserId("u0000015")) should be(new DateTime(2009, 12, 30, 0, 0, 0).plusSeconds(54))
    }
  }

}
