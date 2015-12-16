package uk.ac.warwick.tabula.data

import java.io.StringWriter
import java.sql.Clob
import javax.annotation.Resource

import com.fasterxml.jackson.databind.ObjectMapper
import org.hibernate.dialect.Dialect
import org.jadira.usertype.dateandtime.joda.columnmapper.TimestampColumnDateTimeMapper
import org.joda.time.{DateTime, DateTimeZone}
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation._
import org.springframework.util.FileCopyUtils
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JsonObjectMapperFactory
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.AuditEvent
import uk.ac.warwick.tabula.events.Event
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, MaintenanceModeServiceComponent}

trait AuditEventDao {
	def getById(id: Long): Option[AuditEvent]
	def getByIds(ids: Seq[Long]): Seq[AuditEvent]

	def getByEventId(eventId: String): Seq[AuditEvent]

	def save(event: Event, stage: String): Unit

	def listNewerThan(date: DateTime, max: Int): Seq[AuditEvent]
	def listRecent(start: Int, count: Int): Seq[AuditEvent]

	def latest: Option[DateTime]
}

abstract class AbstractAuditEventDao extends AuditEventDao {
	self: SessionComponent =>

	@Resource(name = "mainDatabaseDialect") var dialect: Dialect = _

	var json: ObjectMapper = JsonObjectMapperFactory.instance

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
	def getByEventId(eventId: String): Seq[AuditEvent] =
		session.newSQLQuery[Array[Object]](eventIdSql)
			.setString("id", eventId)
			.seq.map(mapListToObject)

	def getById(id: Long): Option[AuditEvent] =
		session.newSQLQuery[Array[Object]](idSql)
			.setLong("id", id)
			.uniqueResult
			.map(mapListToObject)

	def getByIds(ids: Seq[Long]): Seq[AuditEvent] =
		ids.grouped(Daoisms.MaxInClauseCount).toSeq.flatMap { group =>
			session.newSQLQuery[Array[Object]](idSql)
				.setParameterList("ids", group)
				.seq.map(mapListToObject)
		}

	private def mapListToObject(array: Array[Object]): AuditEvent = {
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

	private def toIdType(any: Object): Long = any match {
		case n: Number => n.longValue
	}

	private def unclob(any: Object): String = any match {
		case clob: Clob => FileCopyUtils.copyToString(clob.getCharacterStream)
		case string: String => string
		case null => ""
	}

	def latest: Option[DateTime] =
		session.newSQLQuery[java.sql.Timestamp](latestDateSql)
			.uniqueResult
			.map(timestampColumnMapper.fromNonNullValue)

	/**
		* Saves the event in a separate transaction to the main one,
		* so that it can be committed even if the main operation is
		* rolling back.
		*/
	def save(event: Event, stage: String) {
		transactional(propagation = REQUIRES_NEW) {
			// Both Oracle and HSQLDB support sequences, but with different select syntax
			// TODO evaluate this and the SQL once on init
			val nextSeq = dialect.getSelectSequenceNextValString("auditevent_seq")

			val query = session.newSQLQuery("insert into auditevent " +
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
		}
	}

	def listNewerThan(date: DateTime, max: Int): Seq[AuditEvent] =
		session.newSQLQuery[Array[Object]](indexListSql)
			.setTimestamp("eventdate", timestampColumnMapper.toNonNullValue(date))
			.setMaxResults(max)
			.seq.map(mapListToObject)

	def listRecent(start: Int, count: Int): Seq[AuditEvent] =
		session.newSQLQuery[Array[Object]](listSql)
			.setFirstResult(start)
			.setMaxResults(count)
			.seq.map(mapListToObject)

}

trait AuditEventDaoComponent {
	def auditEventDao: AuditEventDao
}

trait AutowiringAuditEventDaoComponent extends AuditEventDaoComponent {
	override val auditEventDao = Wire[AuditEventDao]
}

@Repository("auditEventDao")
class AuditEventDaoImpl extends AbstractAuditEventDao with Daoisms