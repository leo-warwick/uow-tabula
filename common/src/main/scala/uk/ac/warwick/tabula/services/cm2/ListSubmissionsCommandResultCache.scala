package uk.ac.warwick.tabula.services.cm2

import java.time.{Duration, LocalDate, LocalDateTime}

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{Assignment, Submission}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.cm2.SubmissionListItem

import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

@Service("ListSubmissionsCommandResultCache")
class ListSubmissionsCommandResultCache extends Logging {

	private var cacheMap: mutable.HashMap[Assignment, Seq[SubmissionListItem]] = mutable.HashMap.empty

	private var lastUpdated: LocalDateTime = LocalDateTime.now().minusHours(2)

	def updateAndReturn(futureResult: Future[Seq[SubmissionListItem]], assignment: Assignment): Seq[SubmissionListItem] = {
		try {
			val result = Await.result(futureResult, 15.seconds)
			lastUpdated = LocalDateTime.now()
			cacheMap.put(assignment, result)
			result
		} catch {
			case _ => Seq.empty
		}
	}

	def getSubmissionListItemsOrUpdate(
		assignment: Assignment,
		update: Future[Seq[SubmissionListItem]]
	): Seq[SubmissionListItem] = {
		cacheMap
			.get(assignment)
			.map { submissionListItems =>
				if (LocalDateTime.now().minusHours(1).isBefore(lastUpdated)) {
					logger.info("ListSubmissionsCommandResultCache returning cached result")
					return submissionListItems
				} else {
					logger.info("ListSubmissionsCommandResultCache updating with fresh result")
					updateAndReturn(update, assignment)
				}
			}.getOrElse {
			updateAndReturn(update, assignment)
		}
	}
}

trait ListSubmissionsCommandResultCacheComponent {
	def listSubmissionsCommandResultCache: ListSubmissionsCommandResultCache
}

trait AutowiringListSubmissionsCommandResultCache extends ListSubmissionsCommandResultCacheComponent {
	var listSubmissionsCommandResultCache: ListSubmissionsCommandResultCache = Wire[ListSubmissionsCommandResultCache]
}