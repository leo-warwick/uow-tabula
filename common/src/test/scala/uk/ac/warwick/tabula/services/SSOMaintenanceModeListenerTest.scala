package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.sso.client.cache.spring.DatabaseUserCache
import org.springframework.mock.web.MockServletContext
import uk.ac.warwick.sso.client.{SSOConfigLoader, SSOConfiguration}
import uk.ac.warwick.util.queue.Queue

class SSOMaintenanceModeListenerTest extends TestBase with Mockito {

	val config: SSOConfiguration = newSSOConfiguration
	val cache = new DatabaseUserCache(config)

	val context = new MockServletContext
	context.setAttribute(SSOConfigLoader.SSO_CACHE_KEY, cache)
	context.setAttribute(SSOConfigLoader.SSO_CONFIG_KEY, config)

	val queue: Queue = mock[Queue]

	val observer = new SSOMaintenanceModeListener
	observer.maintenanceModeService = new MaintenanceModeServiceImpl
	observer.setServletContext(context)
	observer.queue = queue

	observer.afterPropertiesSet()

	verify(queue, times(1)).addListener("MaintenanceMode", observer)

	@Test def defaults {
		cache.isDatabaseEnabled() should be (true)
		config.getString("mode") should be ("new")
		config.getString("origin.login.location") should be ("https://xebsignon.warwick.ac.uk/origin/hs")
	}

	@Test def enableQueue {
		val message = new MaintenanceModeMessage
		message.enabled = true
		observer.onReceive(message)

		cache.isDatabaseEnabled() should be (false)
		config.getString("mode") should be ("old")
		config.getString("origin.login.location") should be ("https://websignon.warwick.ac.uk/origin/slogin")
	}

	@Test def disableQueue {
		// Enable then disable to override the defaults
		val message = new MaintenanceModeMessage
		message.enabled = true
		observer.onReceive(message)
		message.enabled = false
		observer.onReceive(message)

		cache.isDatabaseEnabled() should be (true)
		config.getString("mode") should be ("new")
		config.getString("origin.login.location") should be ("https://websignon.warwick.ac.uk/origin/hs")
	}

	@Test def enableSameHost {
		observer.maintenanceModeService.enable

		cache.isDatabaseEnabled() should be (false)
		config.getString("mode") should be ("old")
		config.getString("origin.login.location") should be ("https://websignon.warwick.ac.uk/origin/slogin")
	}

	@Test def disableSameHost {
		// Enable then disable to override the defaults
		observer.maintenanceModeService.enable
		observer.maintenanceModeService.disable

		cache.isDatabaseEnabled() should be (true)
		config.getString("mode") should be ("new")
		config.getString("origin.login.location") should be ("https://websignon.warwick.ac.uk/origin/hs")
	}

}