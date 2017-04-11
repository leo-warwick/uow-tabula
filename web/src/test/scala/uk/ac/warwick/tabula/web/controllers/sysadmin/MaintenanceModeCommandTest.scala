package uk.ac.warwick.tabula.web.controllers.sysadmin

import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.util.queue.Queue
import uk.ac.warwick.tabula.services.{SettingsSyncQueueComponent, MaintenanceModeServiceComponent, MaintenanceModeService, MaintenanceModeMessage}
import org.joda.time.DateTime

class MaintenanceModeCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends MaintenanceModeServiceComponent with SettingsSyncQueueComponent {
		val maintenanceModeService: MaintenanceModeService = mock[MaintenanceModeService]
		val settingsSyncQueue: Queue = mock[Queue]
	}

	private trait CommandFixture {
		val command = new MaintenanceModeCommandInternal with CommandTestSupport with MaintenanceModeCommandPopulation
	}

	@Test def populateEmpty(): Unit = new CommandFixture {
		command.maintenanceModeService.until returns None
		command.maintenanceModeService.message returns None

		command.populate()

		command.until should not be null
		command.message should be (null)
	}

	@Test def populateNotEmpty(): Unit = new CommandFixture {
		val dt: DateTime = DateTime.now

		command.maintenanceModeService.until returns Some(dt)
		command.maintenanceModeService.message returns Some("yes")

		command.populate()

		command.until should be (dt)
		command.message should be ("yes")
	}

	@Test def enable(): Unit = new CommandFixture {
		command.maintenanceModeService.until returns None
		command.maintenanceModeService.message  returns None

		command.populate()

		val dt: DateTime = DateTime.now

		command.enable = true
		command.message = "Sound the alarm"
		command.until = dt

		command.applyInternal()

		verify(command.maintenanceModeService, times(1)).message_=(Some("Sound the alarm"))
		verify(command.maintenanceModeService, times(1)).until_=(Some(dt))
		verify(command.maintenanceModeService, times(1)).enable
		verify(command.settingsSyncQueue, times(1)).send(isA[MaintenanceModeMessage])
	}

	@Test def disable(): Unit = new CommandFixture {
		command.maintenanceModeService.until returns None
		command.maintenanceModeService.message returns None

		command.populate()

		val dt: DateTime = DateTime.now

		command.enable = false
		command.message = "Sound the alarm"
		command.until = dt

		command.applyInternal()

		// even though it was set in the command, we re-set to None
		verify(command.maintenanceModeService, times(1)).message_=(None)
		verify(command.maintenanceModeService, times(1)).until_=(None)
		verify(command.maintenanceModeService, times(1)).disable
		verify(command.settingsSyncQueue, times(1)).send(isA[MaintenanceModeMessage])
	}

}