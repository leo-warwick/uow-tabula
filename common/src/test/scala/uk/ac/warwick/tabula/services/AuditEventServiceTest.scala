package uk.ac.warwick.tabula.services

import org.hibernate.Session
import org.joda.time.DateTime
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.data.SessionComponent
import uk.ac.warwick.tabula.data.model.AuditEvent
import uk.ac.warwick.tabula.events.Event
import uk.ac.warwick.tabula.services.elasticsearch.{AuditEventIndexService, ElasticsearchIndexingResult}
import uk.ac.warwick.tabula.system.PostgreSQL10Dialect
import uk.ac.warwick.tabula.{Mockito, PersistenceTestBase}

import scala.concurrent.Future

// scalastyle:off magic.number
class AuditEventServiceTest extends PersistenceTestBase with Mockito {

  val service = new AuditEventServiceImpl with SessionComponent {
    def session: Session = sessionFactory.getCurrentSession
  }
  service.dialect = new PostgreSQL10Dialect
  service.auditEventIndexService = smartMock[AuditEventIndexService]
  service.auditEventIndexService.indexItems(any[Seq[AuditEvent]]) returns Future.successful(ElasticsearchIndexingResult.empty)

  val now = new DateTime()

  @Transactional
  @Test def getByIds() {
    for (i <- Range(0, 1020)) {
      val event = new Event(s"id$i", "DownloadFeedback", "cusebr", "cusebr", "137.205.194.140", "Chrome/58", readOnly = false, Map(), now.plusSeconds(i))
      service.save(event, "before")
      service.save(event, "after")
    }
    val recent = service.listRecent(0, 1020)
    recent.length should be(1020)

    val result = service.getByIds(recent.map(_.id))
    result.length should be(1020)
  }

  @Transactional
  @Test def listEvents() {
    for (i <- Range(1, 30)) {
      val event = new Event("1138-9962-1813-4938", "Bite" + i, "cusebr", "cusebr", "137.205.194.140", "Chrome/58", readOnly = false, Map(), now.plusSeconds(i))
      service.save(event, "pre")
    }

    val recent = service.listRecent(5, 10).toList
    recent.size should be(10)
    recent(0).eventType should be("Bite24")
    recent(2).eventType should be("Bite22")
  }
}