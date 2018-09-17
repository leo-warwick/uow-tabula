package uk.ac.warwick.tabula.services.cm2

import java.time.{Duration, LocalDate, LocalDateTime}

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.cm2.feedback.ListFeedbackCommand.ListFeedbackResult
import uk.ac.warwick.tabula.data.model.{Assignment, Submission}
import uk.ac.warwick.tabula.helpers.Logging
import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

@Service("ListFeedbackCommandResultCache")
class ListFeedbackCommandResultCache extends Logging {

	private var cacheMap: mutable.HashMap[Assignment, ListFeedbackResult] = mutable.HashMap.empty

	private var lastUpdated: LocalDateTime = LocalDateTime.now().minusHours(2)

	def updateAndReturn(futureResult: Future[ListFeedbackResult], assignment: Assignment): ListFeedbackResult = {
		try {
			val result = Await.result(futureResult, 30.seconds)
			lastUpdated = LocalDateTime.now()
			cacheMap.put(assignment, result)
			result
		} catch {
			case _ => ListFeedbackResult(
				Seq.empty,
				Map.empty,
				Map.empty,
				None
			)
		}
	}

	def getListFeedbackResultOrUpdate(
		assignment: Assignment,
		update: Future[ListFeedbackResult]
	): ListFeedbackResult = {
		cacheMap
			.get(assignment)
			.map { listFeedbackResult =>
				if (LocalDateTime.now().minusHours(1).isBefore(lastUpdated)) {
					logger.info("ListFeedbackCommandResultCache returning cached result")
					return listFeedbackResult
				} else {
					logger.info("ListFeedbackCommandResultCache updating with fresh result")
					updateAndReturn(update, assignment)
				}
			}.getOrElse {
			updateAndReturn(update, assignment)
		}
	}
}

trait ListFeedbackCommandResultCacheComponent {
	def listFeedbackCommandResultCache: ListFeedbackCommandResultCache
}

trait AutowiringListFeedbackCommandResultCache extends ListFeedbackCommandResultCacheComponent {
	var listFeedbackCommandResultCache: ListFeedbackCommandResultCache = Wire[ListFeedbackCommandResultCache]
}