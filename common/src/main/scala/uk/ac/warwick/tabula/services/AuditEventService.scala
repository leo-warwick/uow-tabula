package uk.ac.warwick.tabula.services

import java.io.StringWriter
import java.sql.Clob
import javax.annotation.Resource

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.{JsonMappingException, ObjectMapper}
import org.hibernate.dialect.Dialect
import org.jadira.usertype.dateandtime.joda.columnmapper.TimestampColumnDateTimeMapper
import org.joda.time.{DateTime, DateTimeZone}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation._
import org.springframework.util.FileCopyUtils
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.JsonObjectMapperFactory
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.AuditEvent
import uk.ac.warwick.tabula.data.{Daoisms, SessionComponent}
import uk.ac.warwick.tabula.events.Event
import uk.ac.warwick.tabula.services.elasticsearch.AuditEventIndexService

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConverters._

trait AuditEventService {
	def getById(id: Long): Option[AuditEvent]
	def getByIds(ids: Seq[Long]): Seq[AuditEvent]
	def mapListToObject(array: Array[Object]): AuditEvent
	def unclob(any: Object): String
	def save(event: Event, stage: String): Unit
	def save(auditevent: AuditEvent): Unit
	def listNewerThan(date: DateTime, max: Int): Seq[AuditEvent]
	def listRecent(start: Int, count: Int): Seq[AuditEvent]
	def parseData(data: String): Option[Map[String, Any]]
	def getByEventId(eventId: String): Seq[AuditEvent]
	def latest: DateTime

	def addRelated(event: AuditEvent): AuditEvent
}

@Service
class AutowiringEventServiceImpl extends AuditEventServiceImpl
	with Daoisms

class AuditEventServiceImpl extends AuditEventService {
	self: SessionComponent =>

	var json: ObjectMapper = JsonObjectMapperFactory.instance

	@Autowired var auditEventIndexService: AuditEventIndexService = _
	@Resource(name = "mainDatabaseDialect") var dialect: Dialect = _

	private val baseSelect = """select
		eventdate,eventstage,eventtype,masquerade_user_id,real_user_id,data,eventid,id
		from auditevent a"""

	private val DateIndex = 0
	private val StageIndex = 1
	private val TypeIndex = 2
	private val MasqueradeIdIndex = 3
	private val RealIdIndex = 4
	private val DataIndex = 5
	private val EventIdIndex = 6
	private val IdIndex = 7

	private val idSql = baseSelect + " where id = :id"
	private def idsSql = baseSelect + " where id in (:ids)"

	private val eventIdSql = baseSelect + " where eventid = :id"

	// for viewing paginated lists of events
	private val listSql = baseSelect + """ order by eventdate desc """

	private val latestDateSql = """select max(a.eventdate) from auditevent a"""

	// for getting events newer than a certain date, for indexing
	private val indexListSql = baseSelect + """
					where eventdate > :eventdate and eventstage = 'before'
					order by eventdate asc """

	private val timestampColumnMapper = {
		val mapper = new TimestampColumnDateTimeMapper
		mapper.setDatabaseZone(DateTimeZone.forID("Europe/London"))
		mapper.setJavaZone(DateTimeZone.forID("Europe/London"))
		mapper
	}

	/**
	 * Get all AuditEvents with this eventId, i.e. all before/after stages
	 * that were part of the same action.
	 */
	def getByEventId(eventId: String): Seq[AuditEvent] = {
		val query = session.createSQLQuery(eventIdSql)
		query.setString("id", eventId)
		query.list.asInstanceOf[JList[Array[Object]]] map mapListToObject
	}

	def mapListToObject(array: Array[Object]): AuditEvent = {
		if (array == null) {
			null
		} else {
			val a = new AuditEvent
			a.eventDate = timestampColumnMapper.fromNonNullValue(array(DateIndex).asInstanceOf[java.sql.Timestamp])
			a.eventStage = array(StageIndex).toString
			a.eventType = array(TypeIndex).toString
			a.masqueradeUserId = array(MasqueradeIdIndex).asInstanceOf[String]
			a.userId = array(RealIdIndex).asInstanceOf[String]
			a.data = unclob(array(DataIndex))
			a.eventId = array(EventIdIndex).asInstanceOf[String]
			a.id = toIdType(array(IdIndex))
			a
		}
	}

	def toIdType(any: Object): Long = any match {
		case n: Number => n.longValue
	}

	def unclob(any: Object): String = any match {
		case clob: Clob => FileCopyUtils.copyToString(clob.getCharacterStream)
		case string: String => string
		case null => ""
	}

	def getById(id: Long): Option[AuditEvent] = {
		val query = session.createSQLQuery(idSql)
		query.setLong("id", id)
		//		Option(query.uniqueResult.asInstanceOf[Array[Object]]) map mapListToObject map addRelated
		Option(mapListToObject(query.uniqueResult.asInstanceOf[Array[Object]])).map { addRelated }
	}

	def latest: DateTime = {
		val query = session.createSQLQuery(latestDateSql)
		timestampColumnMapper.fromNonNullValue(query.uniqueResult.asInstanceOf[java.sql.Timestamp])
	}

	def getByIds(ids: Seq[Long]): Seq[AuditEvent] =
		ids.grouped(Daoisms.MaxInClauseCount).flatMap { group =>
			val query = session.createSQLQuery(idsSql)
			query.setParameterList("ids", group.asJava)
			val results = query.list.asScala.toSeq.asInstanceOf[Seq[Array[Object]]]
			results.map(mapListToObject).map(addRelated)
		}.toSeq


	def addParsedData(event: AuditEvent): Unit = {
		event.parsedData = parseData(event.data)
	}

	def addRelated(event: AuditEvent): AuditEvent = {
		event.related = getByEventId(event.eventId)
		event
	}

	def save(event: Event, stage: String) {
		doSave(event, stage)
	}

	def save(auditEvent: AuditEvent) {
		doSave(auditEvent.toEvent, auditEvent.eventStage)
	}

	/**
	 * Saves the event in a separate transaction to the main one,
	 * so that it can be committed even if the main operation is
	 * rolling back.
	 */
	def doSave(event: Event, stage: String) {
		transactional(propagation = REQUIRES_NEW) {
			// Both Oracle and HSQLDB support sequences, but with different select syntax
			// TODO evaluate this and the SQL once on init
			val nextSeq = dialect.getSelectSequenceNextValString("auditevent_seq")

			val query = session.createSQLQuery("insert into auditevent " +
				"(id,eventid,eventdate,eventtype,eventstage,real_user_id,masquerade_user_id,data) " +
				"values(" + nextSeq + ", :eventid, :date,:name,:stage,:user_id,:masquerade_user_id,:data)")
			query.setString("eventid", event.id)
			query.setTimestamp("date", timestampColumnMapper.toNonNullValue(event.date))
			query.setString("name", event.name)
			query.setString("stage", stage)
			query.setString("user_id", event.realUserId)
			query.setString("masquerade_user_id", event.userId)
			if (event.extra != null) {
				val data = new StringWriter()
				json.writeValue(data, event.extra)
				query.setString("data", data.toString)
			}
			query.executeUpdate()

			auditEventIndexService.indexItems(getByEventId(event.id).filter(_.eventStage == "before"))
		}
	}

	def listNewerThan(date: DateTime, max: Int): Seq[AuditEvent] = {
		val query = session.createSQLQuery(indexListSql)
		query.setTimestamp("eventdate", timestampColumnMapper.toNonNullValue(date))
		query.setMaxResults(max)
		query.list()
			.asInstanceOf[JList[Array[Object]]]
			.map(mapListToObject)
	}

	def listRecent(start: Int, count: Int): Seq[AuditEvent] = {
		val query = session.createSQLQuery(listSql)
		query.setFirstResult(start)
		query.setMaxResults(count)
		query.list()
			.asInstanceOf[JList[Array[Object]]]
			.map(mapListToObject)
	}

	// parse the data portion of the AuditEvent
	def parseData(data: String): Option[Map[String, Any]] = try {
		Option(data).map { json.readValue(_, classOf[Map[String, Any]]) }
	} catch {
		case e @ (_: JsonParseException | _: JsonMappingException) => None
	}

}

trait AuditEventServiceComponent {
	def auditEventService: AuditEventService
}

trait AutowiringAuditEventServiceComponent extends AuditEventServiceComponent {
	var auditEventService: AuditEventService = Wire[AuditEventService]
}