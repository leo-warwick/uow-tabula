package uk.ac.warwick.tabula.services

import java.time.LocalDateTime
import java.util.concurrent.TimeoutException

import uk.ac.warwick.tabula.helpers.Logging

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait BaseCacheService[K, V] extends Logging {

	case class CacheResult(value: V, validUntil: LocalDateTime)

	private val cacheMap: mutable.HashMap[K, CacheResult] = mutable.HashMap.empty

	def futureTimeout: FiniteDuration

	def defaultValue: V

	def validUntil(key: K): Option[LocalDateTime] = cacheMap.get(key).map(_.validUntil)

	def validDuration: Int = 1

	private def updateAndReturn(futureResult: Future[V], key: K): V = {
		try {
			val result = Await.result(futureResult, futureTimeout)
			cacheMap.put(
				key,
				CacheResult(result, LocalDateTime.now().plusHours(validDuration))
			)
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
			.map { cacheResult =>
				if (LocalDateTime.now().isBefore(cacheResult.validUntil)) {
					logger.info("returning cached result")
					return cacheResult.value
				} else {
					logger.info("updating with fresh value")
					updateAndReturn(futureValue, key)
				}
			}.getOrElse {
			updateAndReturn(futureValue, key)
		}
	}

}
