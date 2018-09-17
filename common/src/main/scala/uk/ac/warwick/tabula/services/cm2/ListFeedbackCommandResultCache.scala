package uk.ac.warwick.tabula.services.cm2

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.cm2.feedback.ListFeedbackCommand.ListFeedbackResult
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.BaseCacheService
import scala.concurrent.duration._

@Service("ListFeedbackCommandResultCache")
class ListFeedbackCommandResultCache extends BaseCacheService[Assignment, ListFeedbackResult] {
	override def futureTimeout: FiniteDuration = 30.seconds

	override def defaultValue: ListFeedbackResult = ListFeedbackResult(
		Seq.empty,
		Map.empty,
		Map.empty,
		None
	)
}

trait ListFeedbackCommandResultCacheComponent {
	def listFeedbackCommandResultCache: ListFeedbackCommandResultCache
}

trait AutowiringListFeedbackCommandResultCache extends ListFeedbackCommandResultCacheComponent {
	var listFeedbackCommandResultCache: ListFeedbackCommandResultCache = Wire[ListFeedbackCommandResultCache]
}