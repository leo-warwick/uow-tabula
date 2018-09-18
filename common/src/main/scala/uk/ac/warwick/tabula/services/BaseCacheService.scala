package uk.ac.warwick.tabula.services

import java.time.LocalDateTime
import java.util.concurrent.TimeoutException

import uk.ac.warwick.tabula.helpers.{Futures, Logging}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

trait BaseCacheService[K, V] extends Logging with AutowiringTaskSchedulerServiceComponent {

	case class CacheResult(value: V, validUntil: LocalDateTime)

	private val cacheMap: mutable.HashMap[K, CacheResult] = mutable.HashMap.empty

	def futureTimeout: FiniteDuration

	def defaultValue: V

	def validUntil(key: K): Option[LocalDateTime] = cacheMap.get(key).map(_.validUntil)

	def validDuration: Int = 1

	private def updateAndReturnFresh(futureResult: Future[V], key: K): V = {
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

	private def update(futureResult: Future[V], key: K): Future[CacheResult] = {
		val futureResultWithTimeout = Futures.withTimeout(futureResult, futureTimeout)
		futureResultWithTimeout.map { result =>
			val value = CacheResult(result, LocalDateTime.now().plusHours(validDuration))
			cacheMap.put(key, value)
			value
		}
	}

	def getValueForKey(key: K, futureValue: Future[V]): V = {
		cacheMap.get(key)
			.map { cacheResult =>
				if (cacheResult.validUntil.isBefore(LocalDateTime.now())) {
					logger.info(s"updating cached value for $key")
					update(futureValue, key)
				}
				cacheResult.value // return previously cached result
			}.getOrElse {
			updateAndReturnFresh(futureValue, key)
		}
	}

}
