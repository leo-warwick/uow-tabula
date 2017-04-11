package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{BoolQueryDefinition, QueryDefinition, RichSearchHit, RichSearchResponse}
import org.elasticsearch.index.query.QueryStringQueryBuilder.Operator
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.helpers.Futures
import uk.ac.warwick.tabula.helpers.Futures._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.UserLookupService.{UniversityId, Usercode}
import uk.ac.warwick.tabula.services.{AuditEventService, AuditEventServiceComponent, UserLookupComponent, UserLookupService}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import scala.concurrent.Future

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
	def adminDownloadedSubmissions(assignment: Assignment): Future[Seq[Submission]]

	/**
		* Get a sequence of User-datetime pairs for when feedback was downloaded for
		* this assignment.
		*/
	def feedbackDownloads(assignment: Assignment): Future[Seq[(User, DateTime)]]

	/**
		* Get a map of usercode-datetime for when online feedback was last viewed on
		* a per-student basis for this assignment.
		*/
	def latestOnlineFeedbackViews(assignment: Assignment): Future[Map[User, DateTime]]

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
}

@Service
class AuditEventQueryServiceImpl extends AbstractQueryService
	with AuditEventIndexType
	with AuditEventQueryService
	with AuditEventQueryMethodsImpl
	with AuditEventServiceComponent
	with UserLookupComponent {

	/**
		* The name of the index alias that this service reads from
		*/
	@Value("${elasticsearch.index.audit.alias}") var indexName: String = _

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
	private def toAuditEvents(hits: Iterable[RichSearchHit]): Seq[AuditEvent] = {
		if (hits.isEmpty) Seq()
		else {
			val databaseResults: Map[Long, AuditEvent] =
				auditEventService.getByIds(hits.map { _.id.toLong }.toSeq)
					.map { event => event.id -> event }
					.toMap

			/** A placeholder AuditEvent to display if a hit has no matching event in the DB. */
			def placeholderEvent(hit: RichSearchHit) = {
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

	def query(queryString: String, start: Int, count: Int): Future[Seq[AuditEvent]] = client.execute {
		searchFor query queryStringQuery(queryString).defaultOperator(Operator.AND) sort ( field sort "eventDate" order SortOrder.DESC ) start start limit count
	}.map { results =>
		toAuditEvents(results.hits)
	}

	def listRecent(start: Int, count: Int): Future[Seq[AuditEvent]] = client.execute {
		searchFor sort ( field sort "eventDate" order SortOrder.DESC ) start start limit count
	}.map { results =>
		toAuditEvents(results.hits)
	}

	private def eventsOfType(eventType: String, restrictions: QueryDefinition*): Future[Seq[AuditEvent]] = {
		val eventTypeQuery = termQuery("eventType", eventType)

		val searchQuery =
			if (restrictions.nonEmpty) bool { must(restrictions :+ eventTypeQuery) }
			else eventTypeQuery

		def scrollNextResults(results: RichSearchResponse): Future[Seq[AuditEvent]] = {
			val events = toAuditEvents(results.hits)

			if (events.isEmpty || results.scrollIdOpt.isEmpty) Future.successful(events)
			else forScroll(results.scrollId).map { nextEvents => events ++ nextEvents }
		}

		def forScroll(id: String): Future[Seq[AuditEvent]] =
			client.execute(searchScroll(id).keepAlive("15s")).flatMap(scrollNextResults)

		// Keep scroll context open for 15 seconds, fetch 100 at a time for performance
		client.execute {
			searchFor.query(searchQuery).scroll("15s").limit(100)
		}.flatMap(scrollNextResults)
	}

	private def parsedEventsOfType(eventType: String, restrictions: QueryDefinition*): Future[Seq[AuditEvent]] =
		eventsOfType(eventType, restrictions: _*).map(_.map(parse))

	private def assignmentRangeRestriction(assignment: Assignment, referenceDate: Option[DateTime]): QueryDefinition = referenceDate match {
		case Some(createdDate) => must(
			termQuery("assignment", assignment.id),
			rangeQuery("eventDate") gte DateFormats.IsoDateTime.print(createdDate)
		)
		case _ => termQuery("assignment", assignment.id)
	}

	def adminDownloadedSubmissions(assignment: Assignment): Future[Seq[Submission]] = {
		val assignmentTerm = assignmentRangeRestriction(assignment, assignment.submissions.asScala.map { _.submittedDate }.sorted.headOption.orElse { Option(assignment.createdDate )})

		// find events where you downloaded all available submissions
		val allDownloaded =
			parsedEventsOfType("DownloadAllSubmissions", assignmentTerm)
				.map { _.sortBy(_.eventDate).reverse }

		// take most recent event and find submissions made before then.
		val submissions1: Future[Seq[Submission]] = allDownloaded.map { _.headOption match {
			case None => Nil
			case Some(event) =>
				val latestDate = event.eventDate
				assignment.submissions.asScala.filter { _.submittedDate.isBefore(latestDate) }
		}}

		// find events where selected submissions were downloaded
		val someDownloaded = parsedEventsOfType("DownloadSubmissions", assignmentTerm)

		val submissions2: Future[Seq[Submission]] = someDownloaded.map { events =>
			events.flatMap { _.submissionIds }
				.flatMap { id =>
					assignment.submissions.asScala.find(_.id == id)
				}
		}

		// find events where individual submissions were downloaded
		val individualDownloads = parsedEventsOfType("AdminGetSingleSubmission", assignmentTerm)

		val submissions3: Future[Seq[Submission]] = individualDownloads.map { events =>
			events.flatMap { _.submissionId }
				.flatMap { id =>
					assignment.submissions.asScala.find(_.id == id)
				}
		}

		Futures.flatten(submissions1, submissions2, submissions3).map { _.distinct }
	}

	// Only look at events since the first time feedback was released
	private def afterFeedbackPublishedRestriction(assignment: Assignment): QueryDefinition =
		assignmentRangeRestriction(assignment, assignment.feedbacks.asScala.flatMap { f => Option(f.releasedDate) }.sorted.headOption.orElse { Option(assignment.createdDate )})

	def feedbackDownloads(assignment: Assignment): Future[Seq[(User, DateTime)]] =
		eventsOfType("DownloadFeedback", afterFeedbackPublishedRestriction(assignment)).map {
			_.filterNot { _.hadError }
			.map { event => userLookup.getUserByUserId(event.masqueradeUserId) -> event.eventDate }
		}

	private def mapToLatest(in: Seq[(User, DateTime)]): Map[User, DateTime] =
		// We take warwick ID as the key to match first, to try and cope with users using multiple IDs for the same uni ID
		in.groupBy { case (user, _) => user.getWarwickId.maybeText.getOrElse(user.getUserId) }
			.map { case (_, eventDates) => eventDates.maxBy { case (_, eventDate) => eventDate } }

	def latestOnlineFeedbackViews(assignment: Assignment): Future[Map[User, DateTime]] = // User ID to DateTime
		eventsOfType("ViewOnlineFeedback", afterFeedbackPublishedRestriction(assignment)).map {
			_.filterNot { _.hadError }
			.map { event => userLookup.getUserByUserId(event.masqueradeUserId) -> event.eventDate }
		}.map(mapToLatest)

	def latestOnlineFeedbackAdded(assignment: Assignment): Future[Map[User, DateTime]] =
		parsedEventsOfType("OnlineFeedback", assignmentRangeRestriction(assignment, Option(assignment.createdDate))).map {
			_.filterNot(_.hadError)
			.flatMap { event =>
				val usersById = event.students.map(userLookup.getUserByUserId)
				val usersByUsercode = event.studentUsercodes.map(userLookup.getUserByWarwickUniId)
				val allUsers = (usersById ++ usersByUsercode).toSet
				allUsers.map { u => u -> event.eventDate }.toSeq
			}
		}.map(mapToLatest)

	def latestGenericFeedbackAdded(assignment: Assignment): Future[Option[DateTime]] =
		eventsOfType("GenericFeedback", assignmentRangeRestriction(assignment, Option(assignment.createdDate))).map {
			case Nil => None
			case allEvents => Some(allEvents.maxBy { _.eventDate }.eventDate)
		}

	def publishFeedbackForStudent(assignment: Assignment, usercode: Usercode, uniId: Option[UniversityId]): Future[Seq[AuditEvent]] = {

		val optionalUniIdQuery = uniId.map(uid => termQuery("students",uid))
		val usercodeQuery = termQuery("studentUsercodes", usercode)
		val queries =  optionalUniIdQuery.toSeq :+ usercodeQuery

		parsedEventsOfType(
			"PublishFeedback",
			bool {
				should ( queries )
			},
			afterFeedbackPublishedRestriction(assignment)
		).map { events =>
			events.sortBy(_.eventDate).reverse
		}
	}

	private def submissionEventsForModules(modules: Seq[Module], lastUpdatedDate: Option[DateTime], max: Int, restrictions: QueryDefinition*): Future[PagedAuditEvents] = {
		val eventTypeQuery = termQuery("eventType", "SubmitAssignment")

		val queries: Seq[QueryDefinition] = lastUpdatedDate match {
			case None => restrictions :+ eventTypeQuery
			case Some(date) => Seq(eventTypeQuery, rangeQuery("eventDate") lte DateFormats.IsoDateTime.print(date) includeUpper false) ++ restrictions
		}

		client.execute {
			searchFor query bool {
				must(queries: _*).should(modules.map { module => termQuery("module", module.id) })
			} limit max sort ( field sort "eventDate" order SortOrder.DESC )
		}.map { results =>
			val items = toAuditEvents(results.hits)

			PagedAuditEvents(
				items = items,
				lastUpdatedDate = items.lastOption.map { _.eventDate },
				totalHits = results.totalHits
			)
		}
	}


	def submissionsForModules(modules: Seq[Module], lastUpdatedDate: Option[DateTime], max: Int = DefaultMaxEvents): Future[PagedAuditEvents] =
		submissionEventsForModules(modules, lastUpdatedDate, max)

	def noteworthySubmissionsForModules(modules: Seq[Module], lastUpdatedDate: Option[DateTime], max: Int = DefaultMaxEvents): Future[PagedAuditEvents] =
		submissionEventsForModules(modules, lastUpdatedDate, max, termQuery("submissionIsNoteworthy", true))

}

trait AuditEventQueryServiceComponent {
	def auditEventQueryService: AuditEventQueryService
}

trait AutowiringAuditEventQueryServiceComponent extends AuditEventQueryServiceComponent {
	var auditEventQueryService: AuditEventQueryService = Wire[AuditEventQueryService]
}