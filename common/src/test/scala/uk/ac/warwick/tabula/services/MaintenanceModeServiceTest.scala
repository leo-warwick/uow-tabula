package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Reactor
import org.joda.time.DateTime

class MaintenanceModeServiceTest extends TestBase {

	val service = new MaintenanceModeServiceImpl

	@Test def enable {
		service._enabled = false

		var called = false
		service.changingState.observe { enabled =>
			called should be (false)
			enabled should be (true)
			called = true
		}

		val message = new MaintenanceModeMessage
		message.enabled = true
		message.until = DateTime.now.getMillis
		message.message = "Yo momma"

		service.update(message)

		// so good I called it twice
		service.update(message)

		called should be (true)
	}

	@Test def disable {
		service._enabled = true

		var called = false
		service.changingState.observe { enabled =>
			called should be (false)
			enabled should be (false)
			called = true
		}

		val message = new MaintenanceModeMessage
		message.enabled = false

		service.update(message)

		// so good I called it twice
		service.update(message)
	}

}