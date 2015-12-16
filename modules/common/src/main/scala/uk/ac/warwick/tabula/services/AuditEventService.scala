package uk.ac.warwick.tabula.services

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.JsonParseException
import org.joda.time.DateTime
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.data.model.AuditEvent
import uk.ac.warwick.tabula.data.{AutowiringAuditEventDaoComponent, AuditEventDaoComponent}
import uk.ac.warwick.tabula.events.Event
import uk.ac.warwick.tabula.JsonObjectMapperFactory
import uk.ac.warwick.spring.Wire

trait AuditEventService {
	def getById(id: Long): Option[AuditEvent]
	def getByIds(ids: Seq[Long]): Seq[AuditEvent]
	def save(event: Event, stage: String): Unit
	def save(auditevent: AuditEvent): Unit
	def listNewerThan(date: DateTime, max: Int): Seq[AuditEvent]
	def listRecent(start: Int, count: Int): Seq[AuditEvent]
	def parseData(data: String): Option[Map[String, Any]]
	def getByEventId(eventId: String): Seq[AuditEvent]
	def latest: Option[DateTime]

	def addRelated(event: AuditEvent): AuditEvent
}

@Component
class AutowiringEventServiceImpl extends AuditEventServiceImpl
	with AutowiringAuditEventDaoComponent

class AuditEventServiceImpl extends AuditEventService {
	self: AuditEventDaoComponent =>

	var json: ObjectMapper = JsonObjectMapperFactory.instance

	/**
	 * Get all AuditEvents with this eventId, i.e. all before/after stages
	 * that were part of the same action.
	 */
	def getByEventId(eventId: String): Seq[AuditEvent] = auditEventDao.getByEventId(eventId)

	def getById(id: Long): Option[AuditEvent] = auditEventDao.getById(id).map(addRelated)
	def getByIds(ids: Seq[Long]): Seq[AuditEvent] = auditEventDao.getByIds(ids).map(addRelated)

	def latest: Option[DateTime] = auditEventDao.latest

	def addParsedData(event: AuditEvent) = {
		event.parsedData = parseData(event.data)
	}

	def addRelated(event: AuditEvent) = {
		event.related = getByEventId(event.eventId)
		event
	}

	def save(event: Event, stage: String) = auditEventDao.save(event, stage)
	def save(auditEvent: AuditEvent) = auditEventDao.save(auditEvent.toEvent, auditEvent.eventStage)

	def listNewerThan(date: DateTime, max: Int): Seq[AuditEvent] = auditEventDao.listNewerThan(date, max)

	def listRecent(start: Int, count: Int): Seq[AuditEvent] = auditEventDao.listRecent(start, count)

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
	var auditEventService = Wire[AuditEventService]
}