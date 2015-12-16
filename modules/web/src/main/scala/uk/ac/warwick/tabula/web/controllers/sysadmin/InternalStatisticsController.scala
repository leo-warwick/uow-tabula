package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.hibernate.SessionFactory
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestParam}
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.util.queue.Queue
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.util.cache.memcached.MemcachedCacheStore
import scala.collection.JavaConverters._

/**
 * Control the Hibernate SessionFactory's statistics object to log out various
 * statistics on demand, for analysing performance issues.
 */
@Controller
class InternalStatisticsController extends BaseSysadminController {

	// Make all changes through the queue, because each WAR has its own sessionfactory
	var queue = Wire.named[Queue]("settingsSyncTopic")
	val sessionFactory = Wire[SessionFactory]

	@RequestMapping(value=Array("/sysadmin/statistics"))
	def form() = Mav("sysadmin/statistics/form")

	@RequestMapping(value=Array("/sysadmin/statistics/hibernate/toggleAsync.json"), method=Array(POST))
	def toggleHibernateStatistics(@RequestParam enabled: Boolean) = {
		val oldValue = sessionFactory.getStatistics.isStatisticsEnabled
		queue.send(HibernateStatisticsMessage(if (enabled) "enable" else "disable"))
		new JSONView(Map("enabled" -> enabled, "wasEnabled" -> oldValue))
	}

	@RequestMapping(value=Array("/sysadmin/statistics/hibernate/log"))
	def hibernateStatisticsJson() = {
		queue.send(HibernateStatisticsMessage("log"))
		Mav("sysadmin/statistics/hibernatelogged").noLayout()
	}

	@RequestMapping(value=Array("/sysadmin/statistics/hibernate/clearAsync.json"), method=Array(POST))
	def clearHibernateStatistics() = {
		queue.send(HibernateStatisticsMessage("clear"))
		new JSONView(Map("cleared" -> true))
	}

	@ModelAttribute("memcachedStatistics")
	def memcachedStats = {
		MemcachedCacheStore.getDefaultMemcachedClient
			.getStats
			.asScala
			.map { case (_, stats) => stats }
			.head
			.asScala
			.toSeq
			.sorted
	}

	@RequestMapping(value=Array("/sysadmin/statistics/memcached/clearAsync.json"), method=Array(POST))
	def clearMemcached() = {
		MemcachedCacheStore.getDefaultMemcachedClient.flush().get()
		new JSONView(Map("cleared" -> true))
	}

}
