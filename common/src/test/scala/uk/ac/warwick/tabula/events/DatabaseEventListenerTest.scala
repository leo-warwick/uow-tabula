package uk.ac.warwick.tabula.events

import uk.ac.warwick.tabula.{TestBase, Mockito, Fixtures, ItemNotFoundException}
import uk.ac.warwick.tabula.services.AuditEventService
import uk.ac.warwick.tabula.services.MaintenanceModeEnabledException
import uk.ac.warwick.tabula.services.MaintenanceModeServiceImpl
import org.joda.time.DateTime
import java.io.File
import java.util.UUID

class DatabaseEventListenerTest extends TestBase with Mockito {

	val listener = new DatabaseEventListener

	val auditEventService: AuditEventService = mock[AuditEventService]
	val maintenanceModeService = new MaintenanceModeServiceImpl

	listener.auditEventService = auditEventService
	listener.maintenanceModeService = maintenanceModeService
	listener.auditDirectory = createTemporaryDirectory
	listener.auditDirectory.deleteOnExit()
	listener.createMissingDirs = true

	listener.afterPropertiesSet

	@Test def maintenanceModeDisabled {
		maintenanceModeService.disable

		val event = Event(
			id="event", name="AddAssignment", userId="cuscav", realUserId="cuscav",
			date=DateTime.now, extra=Map()
		)

		listener.beforeCommand(event)
		verify(auditEventService, times(1)).save(event, "before")

		listener.afterCommand(event, Fixtures.assignment("my assignment"))
		verify(auditEventService, times(1)).save(event, "after")

		listener.onException(event, new ItemNotFoundException)
		verify(auditEventService, times(1)).save(event, "error")
	}

	@Test def maintenanceMode {
		maintenanceModeService.enable

		val event = Event(
			id="event", name="AddAssignment", userId="cuscav", realUserId="cuscav",
			date=DateTime.now, extra=Map()
		)

		listener.beforeCommand(event)
		verify(auditEventService, times(0)).save(event, "before")

		listener.afterCommand(event, Fixtures.assignment("my assignment"))
		verify(auditEventService, times(0)).save(event, "after")

		listener.onException(event, new ItemNotFoundException)
		verify(auditEventService, times(0)).save(event, "error")

		// Ho ho ho, magic time - observation deck!
		maintenanceModeService.disable

		// That should flush the 3 events to the db
		verify(auditEventService, times(1)).save(event, "before")
		verify(auditEventService, times(1)).save(event, "after")
		verify(auditEventService, times(1)).save(event, "error")
	}

}
