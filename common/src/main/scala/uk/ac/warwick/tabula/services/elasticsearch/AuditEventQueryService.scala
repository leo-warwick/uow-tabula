package uk.ac.warwick.tabula.services.elasticsearch

import java.util.regex.Pattern

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.json.SourceAsContentBuilder
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.searches.aggs.responses.bucket.Terms
import com.sksamuel.elastic4s.requests.searches.aggs.responses.metrics.{TopHit, TopHits}
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.requests.searches.sort.SortOrder
import com.sksamuel.elastic4s.{Hit, Index, Response}
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroupEventOccurrence}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.helpers.ExecutionContexts.global
import uk.ac.warwick.tabula.helpers.Futures
import uk.ac.warwick.tabula.services.UserLookupService.{UniversityId, Usercode}
import uk.ac.warwick.tabula.services.elasticsearch.AuditEventQueryService.RecordAttendanceEvent
import uk.ac.warwick.tabula.services.{AuditEventService, AuditEventServiceComponent, UserLookupComponent, UserLookupService}
import uk.ac.warwick.userlookup.{AnonymousUser, User}

import scala.concurrent.Future
import scala.jdk.CollectionConverters._

object AuditEventQueryService {
  case class RecordAttendanceEvent(
    dateTime: DateTime,
    user: User,
    attendance: Option[Map[UniversityId, AttendanceState]],
    onlyIncludesChanges: Boolean
  )
}

trait AuditEventQueryService
  extends AuditEventQueryMethods

case class PagedAuditEvents(items: Seq[AuditEvent], lastUpdatedDate: Option[DateTime], totalHits: Long)

trait AuditEventNoteworthySubmissionsService {
  final val DefaultMaxEvents = 50

  def submissionsForModules(modules: Seq[Module], lastUpdatedDate: Option[DateTime], max: Int = DefaultMaxEvents): Future[PagedAuditEvents]

  def noteworthySubmissionsForModules(modules: Seq[Module], lastUpdatedDate: Option[DateTime], max: Int = DefaultMaxEvents): Future[PagedAuditEvents]
}

trait AuditEventQueryMethods extends AuditEventNoteworthySubmissionsService {
  /**
    * Search based on a query string, sorted by event date descending - should only be used by admins
    * as may expose sensitive information.
    */
  def query(queryString: String, start: Int, count: Int): Future[Seq[AuditEvent]]

  /**
    * Shows recent events, sorted by event date descending - should only be used by admins
    * as may expose sensitive information.
    */
  def listRecent(start: Int, count: Int): Future[Seq[AuditEvent]]

  /**
    * Work out which submissions have been downloaded from the admin interface
    * based on the audit events.
    */
  def adminDownloadedSubmissions(assignment: Assignment, submissions: Seq[Submission]): Future[Seq[Submission]]

  /**
    * Get a sequence of User-datetime pairs for when feedback was downloaded for
    * this assignment.
    */
  def feedbackDownloads(assignment: Assignment, feedback: Seq[Feedback]): Future[Seq[(User, DateTime)]]

  /**
    * Get a map of usercode-datetime for when online feedback was last viewed on
    * a per-student basis for this assignment.
    */
  def latestOnlineFeedbackViews(assignment: Assignment, feedback: Seq[Feedback]): Future[Map[User, DateTime]]

  /**
    * Get a map of universityId-datetime for when online feedback was last
    * added on a per-student basis for this assignment.
    */
  def latestOnlineFeedbackAdded(assignment: Assignment): Future[Map[User, DateTime]]

  /**
    * Get the last datetime that generic feedback was added for this assignment, if
    * an event exists.
    */
  def latestGenericFeedbackAdded(assignment: Assignment): Future[Option[DateTime]]

  /**
    * A list of audit events for publishing feedback that includes a particular student
    */
  def publishFeedbackForStudent(assignment: Assignment, usercode: Usercode, uniId: Option[UniversityId]): Future[Seq[AuditEvent]]

  /**
   * A list of recorded attendance events for a small group event in a particular week.
   *
   * @return a tuple of
   * - The date-time that the register was updated
   * - The user who took the register
   * - (optionally) A Map of UniversityId to AttendanceState - may be None for audit events before TAB-7756 (Tabula 2019.10.5)
   */
  def smallGroupEventAttendanceRegisterEvents(smallGroupEvent: SmallGroupEvent, week: SmallGroupEventOccurrence.WeekNumber): Future[Seq[RecordAttendanceEvent]]
}

@Service
class AuditEventQueryServiceImpl extends AbstractQueryService
  with AuditEventQueryService
  with AuditEventQueryMethodsImpl
  with AuditEventServiceComponent
  with UserLookupComponent {

  /**
    * The name of the index alias that this service reads from
    */
  @Value("${elasticsearch.index.audit.alias}") var indexName: String = _
  lazy val index: Index = Index(indexName)

  @Autowired var auditEventService: AuditEventService = _
  @Autowired var userLookup: UserLookupService = _

}

trait AuditEventQueryMethodsImpl extends AuditEventQueryMethods {
  self: ElasticsearchClientComponent
    with ElasticsearchSearching
    with AuditEventServiceComponent
    with UserLookupComponent =>

  /**
    * Convert a list of ElasticSearch hits to a list of AuditEvents.
    * Any events not found in the database will be returned as placeholder
    * events with whatever data we kept in ElasticSearch, just in case
    * an event went missing and we'd like to see the data.
    */
  private def toAuditEvents(hits: Iterable[Hit]): Seq[AuditEvent] = {
    if (hits.isEmpty) Seq()
    else {
      val databaseResults: Map[Long, AuditEvent] =
        auditEventService.getByIds(hits.map(_.id.toLong).toSeq)
          .map { event => event.id -> event }
          .toMap

      /** A placeholder AuditEvent to display if a hit has no matching event in the DB. */
      def placeholderEvent(hit: Hit) = {
        val event = AuditEvent()
        event.eventStage = "before"
        event.data = "{}" // We can't restore this if it's not from the db
        event.id = hit.id.toLong

        hit.sourceAsMap.get("eventId").foreach { v => event.eventId = v.toString }
        hit.sourceAsMap.get("userId").foreach { v => event.userId = v.toString }
        hit.sourceAsMap.get("masqueradeUserId").foreach { v => event.masqueradeUserId = v.toString }
        hit.sourceAsMap.get("eventType").foreach { v => event.eventType = v.toString }
        hit.sourceAsMap.get("eventDate").foreach { v => event.eventDate = DateFormats.IsoDateTime.parseDateTime(v.toString) }

        event
      }

      hits.map { hit =>
        databaseResults.getOrElse(hit.id.toLong, placeholderEvent(hit))
      }.toSeq
    }
  }

  private def parse(event: AuditEvent): AuditEvent = {
    event.parsedData = auditEventService.parseData(event.data)
    event.related.foreach { e =>
      e.parsedData = auditEventService.parseData(e.data)
    }
    event
  }

  def query(queryString: String, start: Int, count: Int): Future[Seq[AuditEvent]] =
    client.execute {
      searchRequest
        .query(queryStringQuery(queryString).defaultOperator("AND"))
        .sortBy(fieldSort("eventDate").order(SortOrder.Desc))
        .start(start)
        .limit(count)
    }.map { response => toAuditEvents(response.result.hits.hits) }

  def listRecent(start: Int, count: Int): Future[Seq[AuditEvent]] =
    client.execute {
      searchRequest
        .sortBy(fieldSort("eventDate").order(SortOrder.Desc))
        .start(start)
        .limit(count)
    }.map { response => toAuditEvents(response.result.hits.hits) }

  private def eventsOfType(eventType: String, restrictions: Query*): Future[Seq[AuditEvent]] = {
    val eventTypeQuery = termQuery("eventType.keyword", eventType)

    val searchQuery =
      if (restrictions.nonEmpty) boolQuery().must(restrictions :+ eventTypeQuery)
      else eventTypeQuery

    def scrollNextResults(response: Response[SearchResponse]): Future[Seq[AuditEvent]] = {
      val events = toAuditEvents(response.result.hits.hits)

      if (events.isEmpty || response.result.scrollId.isEmpty) Future.successful(events)
      else forScroll(response.result.scrollId.get).map { nextEvents => events ++ nextEvents }
    }

    def forScroll(id: String): Future[Seq[AuditEvent]] =
      client.execute(searchScroll(id).keepAlive("15s")).flatMap(scrollNextResults)

    // Keep scroll context open for 15 seconds, fetch 100 at a time for performance
    client.execute {
      searchRequest.query(searchQuery).scroll("15s").limit(100)
    }.flatMap(scrollNextResults)
  }

  private def parsedEventsOfType(eventType: String, restrictions: Query*): Future[Seq[AuditEvent]] =
    eventsOfType(eventType, restrictions: _*).map(_.map(parse))

  case class TopHitHit(hit: TopHit) extends Hit {
    override def id: String = hit.id

    override def index: String = hit.index

    override def `type`: String = hit.`type`

    override def version: Long = 1

    override def seqNo: Long = 1

    override def primaryTerm: Long = 1

    override def sourceAsString: String = SourceAsContentBuilder(sourceAsMap).string()

    override def sourceAsMap: Map[String, AnyRef] = hit.source.asInstanceOf[Map[String, AnyRef]]

    override def exists: Boolean = true

    override def score: Float = hit.score.map(_.toFloat).getOrElse(0.0f)
  }

  private def latestEventsOfType(eventType: String, restrictions: Query*): Future[Seq[AuditEvent]] = {
    val eventTypeQuery = termQuery("eventType.keyword", eventType)

    val searchQuery =
      if (restrictions.nonEmpty) boolQuery().must(restrictions :+ eventTypeQuery)
      else eventTypeQuery

    client.execute {
      searchRequest
        .query(searchQuery).limit(0)
        .aggregations(
          termsAgg("userIds", "userId.keyword")
            .size(1000)
            .addSubAggregation(
              topHitsAgg("latestEvent")
                .size(1)
                .sortBy(fieldSort("eventDate").order(SortOrder.Desc))
            )
        )
    }.map { response =>
      val hits =
        response.result
          .aggregations
          .result[Terms]("userIds")
          .buckets.map { bucket =>
            TopHitHit(bucket.result[TopHits]("latestEvent").hits.head)
          }

      toAuditEvents(hits)
    }
  }

  private def parsedLatestEventsOfType(eventType: String, restrictions: Query*): Future[Seq[AuditEvent]] =
    latestEventsOfType(eventType, restrictions: _*).map(_.map(parse))

  private def assignmentRangeRestriction(assignment: Assignment, referenceDate: Option[DateTime]): Query = referenceDate match {
    case Some(createdDate) => must(
      termQuery("assignment.keyword", assignment.id),
      rangeQuery("eventDate") gte DateFormats.IsoDateTime.print(createdDate)
    )
    case _ => termQuery("assignment.keyword", assignment.id)
  }

  def adminDownloadedSubmissions(assignment: Assignment, submissions: Seq[Submission]): Future[Seq[Submission]] = {
    val assignmentTerm = assignmentRangeRestriction(assignment, submissions.map(_.submittedDate).sorted.headOption.orElse(Option(assignment.createdDate)))

    // find events where you downloaded all available submissions
    val latestDownloadEvent =
      client.execute {
        searchRequest
          .query(
            boolQuery()
              .must(
                termQuery("eventType.keyword", "DownloadAllSubmissions"),
                assignmentTerm
              )
          )
          .sortBy(fieldSort("eventDate").order(SortOrder.Desc))
          .start(0)
          .limit(1)
      }.map { response => toAuditEvents(response.result.hits.hits).headOption }

    // take most recent event and find submissions made before then.
    val submissions1: Future[Seq[Submission]] = latestDownloadEvent.map {
      case None => Nil
      case Some(event) =>
        val latestDate = event.eventDate
        submissions.filter(_.submittedDate.isBefore(latestDate))
    }

    // find events where selected submissions were downloaded
    val selectedSubmissionsUsers =
      client.execute {
        searchRequest
          .query(
            boolQuery()
              .must(
                termQuery("eventType.keyword", "DownloadSubmissions"),
                assignmentTerm
              )
          )
          .limit(0)
          .aggregations(
            termsAgg("studentUsercodes", "studentUsercodes.keyword")
              .size(1000),
            termsAgg("studentUniversityIds", "students.keyword")
              .size(1000)
          )
      }.map { response =>
        val studentUsercodes = response.result.aggregations.result[Terms]("studentUsercodes").buckets.map(_.key)
        val studentUniversityIds = response.result.aggregations.result[Terms]("studentUniversityIds").buckets.map(_.key)

        (studentUsercodes, studentUniversityIds)
      }

    val submissions2: Future[Seq[Submission]] = selectedSubmissionsUsers.map { case (usercodes, universityIds) =>
      submissions.filter { submission =>
        usercodes.contains(submission.usercode) || submission.universityId.exists(universityIds.contains)
      }
    }

    // find events where individual submissions were downloaded
    val individualDownloadSubmissionIds =
      client.execute {
        searchRequest
          .query(
            boolQuery()
              .must(
                termQuery("eventType.keyword", "AdminGetSingleSubmission"),
                assignmentTerm
              )
          )
          .limit(0)
          .aggregations(
            termsAgg("submissionId", "submission.keyword")
              .size(1000)
          )
      }.map { response =>
        response.result.aggregations.result[Terms]("submissionId").buckets.map(_.key)
      }

    val submissions3: Future[Seq[Submission]] = individualDownloadSubmissionIds.map { submissionIds =>
      submissionIds.flatMap { id => submissions.find(_.id == id) }
    }

    Futures.flatten(submissions1, submissions2, submissions3).map(_.distinct)
  }

  // Only look at events since the first time feedback was released
  private def afterFeedbackPublishedRestriction(assignment: Assignment, feedback: Seq[Feedback]): Query =
    assignmentRangeRestriction(assignment, feedback.flatMap { f => Option(f.releasedDate) }.sorted.headOption.orElse {
      Option(assignment.createdDate)
    })

  def feedbackDownloads(assignment: Assignment, feedback: Seq[Feedback]): Future[Seq[(User, DateTime)]] =
    latestEventsOfType("DownloadFeedback", afterFeedbackPublishedRestriction(assignment, feedback))
      .map(_.filterNot(_.hadError))
      .map { events =>
        val userIds = events.map(_.masqueradeUserId).distinct
        val userIdToUser = batchedUsersByUserId(userIds)

        events.map { event =>
          userIdToUser(event.masqueradeUserId) -> event.eventDate
        }
      }

  private def batchedUsersByUserId(userIds: Seq[String]): Map[String, User] = {
    if (userIds.isEmpty) Map.empty[String, User]
    else userIds.grouped(100).map(userLookup.usersByUserIds).reduce(_ ++ _)
  }.withDefault { userId => new AnonymousUser(userId) }

  private def batchedUsersByWarwickId(warwickIds: Seq[String]): Map[String, User] = {
    if (warwickIds.isEmpty) Map.empty[String, User]
    else warwickIds.grouped(100).map(userLookup.usersByWarwickUniIds).reduce(_ ++ _)
  }.withDefault { warwickId =>
    val u = new AnonymousUser(s"u$warwickId")
    u.setWarwickId(warwickId)
    u
  }

  def latestOnlineFeedbackViews(assignment: Assignment, feedback: Seq[Feedback]): Future[Map[User, DateTime]] = // User ID to DateTime
    latestEventsOfType("ViewOnlineFeedback", afterFeedbackPublishedRestriction(assignment, feedback))
      .map(_.filterNot(_.hadError))
      .map { events =>
        val userIds = events.map(_.masqueradeUserId).distinct
        val userIdToUser = batchedUsersByUserId(userIds)

        events.map { event =>
          userIdToUser(event.masqueradeUserId) -> event.eventDate
        }.toMap
      }

  def latestOnlineFeedbackAdded(assignment: Assignment): Future[Map[User, DateTime]] =
    parsedLatestEventsOfType("OnlineFeedback", assignmentRangeRestriction(assignment, Option(assignment.createdDate)))
      .map(_.filterNot(_.hadError))
      .map { events =>
        val studentUniversityIds = events.flatMap(_.students).distinct
        val studentUsercodes = events.flatMap(_.studentUsercodes).distinct

        val allUsersByUniversityId = batchedUsersByWarwickId(studentUniversityIds)
        val allUsersByUsercode = batchedUsersByUserId(studentUsercodes)

        events.flatMap { event =>
          val usersById = event.students.map(allUsersByUniversityId)
          val usersByUsercode = event.studentUsercodes.map(allUsersByUsercode)
          val allUsers = (usersById ++ usersByUsercode).toSet
          allUsers.map { u => u -> event.eventDate }.toSeq
        }.toMap
      }

  def latestGenericFeedbackAdded(assignment: Assignment): Future[Option[DateTime]] =
    eventsOfType("GenericFeedback", assignmentRangeRestriction(assignment, Option(assignment.createdDate))).map {
      case Nil => None
      case allEvents => Some(allEvents.maxBy {
        _.eventDate
      }.eventDate)
    }

  def publishFeedbackForStudent(assignment: Assignment, usercode: Usercode, uniId: Option[UniversityId]): Future[Seq[AuditEvent]] = {

    val optionalUniIdQuery = uniId.map(uid => termQuery("students", uid))
    val usercodeQuery = termQuery("studentUsercodes", usercode)
    val queries = optionalUniIdQuery.toSeq :+ usercodeQuery

    parsedEventsOfType(
      "PublishFeedback",
      boolQuery().should(queries),
      afterFeedbackPublishedRestriction(assignment, assignment.feedbacks.asScala.toSeq)
    ).map { events =>
      events.sortBy(_.eventDate).reverse
    }
  }

  private def submissionEventsForModules(modules: Seq[Module], lastUpdatedDate: Option[DateTime], max: Int, restrictions: Query*): Future[PagedAuditEvents] = {
    val eventTypeQuery = termQuery("eventType.keyword", "SubmitAssignment")

    val queries: Seq[Query] = lastUpdatedDate match {
      case None => restrictions :+ eventTypeQuery
      case Some(date) => Seq(eventTypeQuery, rangeQuery("eventDate").lt(DateFormats.IsoDateTime.print(date))) ++ restrictions
    }

    client.execute {
      searchRequest
        .query(
          boolQuery()
            .must(queries)
            .should(modules.map { module => termQuery("module.keyword", module.id) })
        )
        .limit(max)
        .sortBy(fieldSort("eventDate").order(SortOrder.Desc))
    }.map { response =>
      val items = toAuditEvents(response.result.hits.hits)

      PagedAuditEvents(
        items = items,
        lastUpdatedDate = items.lastOption.map(_.eventDate),
        totalHits = response.result.totalHits
      )
    }
  }


  def submissionsForModules(modules: Seq[Module], lastUpdatedDate: Option[DateTime], max: Int = DefaultMaxEvents): Future[PagedAuditEvents] =
    submissionEventsForModules(modules, lastUpdatedDate, max)

  def noteworthySubmissionsForModules(modules: Seq[Module], lastUpdatedDate: Option[DateTime], max: Int = DefaultMaxEvents): Future[PagedAuditEvents] =
    submissionEventsForModules(modules, lastUpdatedDate, max, termQuery("submissionIsNoteworthy", true))

  def smallGroupEventAttendanceRegisterEvents(smallGroupEvent: SmallGroupEvent, week: SmallGroupEventOccurrence.WeekNumber): Future[Seq[RecordAttendanceEvent]] = {
    parsedEventsOfType(
      "RecordAttendance",
      termQuery("smallGroupEvent.keyword", smallGroupEvent.id),
      termQuery("week", week)
    ).map { events =>
      val userIds = events.map(_.masqueradeUserId).distinct
      val userIdToUser = batchedUsersByUserId(userIds)

      events.sortBy(_.eventDate).map { auditEvent =>
        val attendance: Option[Map[UniversityId, AttendanceState]] = auditEvent.combinedParsedData.get("smallGroupAttendanceState").collect {
          case s: Seq[String @unchecked] => s.filter(_.contains(" - ")).flatMap { str =>
            str.split(Pattern.quote(" - "), 2) match {
              case Array(universityId, description) =>
                AttendanceState.values.find(_.description == description).map { state =>
                  universityId -> state
                }
              case _ => None
            }
          }.toMap
        }

        val onlyIncludesChanges = auditEvent.combinedParsedData.get("onlyIncludesChanges").collect { case b: Boolean => b }.getOrElse(false)
        RecordAttendanceEvent(auditEvent.eventDate, userIdToUser(auditEvent.masqueradeUserId), attendance, onlyIncludesChanges)
      }
    }
  }

}

trait AuditEventQueryServiceComponent {
  def auditEventQueryService: AuditEventQueryService
}

trait AutowiringAuditEventQueryServiceComponent extends AuditEventQueryServiceComponent {
  var auditEventQueryService: AuditEventQueryService = Wire[AuditEventQueryService]
}
