package uk.ac.warwick.tabula.services

import java.time.LocalDateTime
import java.util.concurrent.TimeoutException

import uk.ac.warwick.tabula.helpers.Logging

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait BaseCacheService[K, V] extends Logging {

	val cacheMap: mutable.HashMap[K, V] = mutable.HashMap.empty

	private var lastUpdated: LocalDateTime = LocalDateTime.now().minusHours(2)

	def futureTimeout: FiniteDuration

	def defaultValue: V

	def cacheDurationInHour: Int = 1

	def updateAndReturn(futureResult: Future[V], key: K): V = {
		try {
			val result = Await.result(futureResult, futureTimeout)
			lastUpdated = LocalDateTime.now()
			cacheMap.put(key, result)
			result
		} catch {
			case e: TimeoutException =>
				logger.error(s"future timed out.", e)
				defaultValue
			case _ =>
				logger.error(s"unknown exception")
				defaultValue
		}
	}

	def getValueForKey(key: K, futureValue: Future[V]): V = {
		cacheMap
			.get(key)
			.map { value =>
				if (LocalDateTime.now().minusHours(cacheDurationInHour).isBefore(lastUpdated)) {
					logger.info("returning cached result")
					return value
				} else {
					logger.info("updating with fresh value")
					updateAndReturn(futureValue, key)
				}
			}.getOrElse {
			updateAndReturn(futureValue, key)
		}

	}

}
