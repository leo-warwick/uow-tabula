package uk.ac.warwick.tabula.services.cm2

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.helpers.cm2.SubmissionListItem
import uk.ac.warwick.tabula.services.BaseCacheService
import scala.concurrent.duration._

@Service("ListSubmissionsCommandResultCache")
class ListSubmissionsCommandResultCache extends BaseCacheService[Assignment, Seq[SubmissionListItem]] {
	override def futureTimeout: FiniteDuration = 15.seconds

	override def defaultValue: Seq[SubmissionListItem] = Seq.empty
}

trait ListSubmissionsCommandResultCacheComponent {
	def listSubmissionsCommandResultCache: ListSubmissionsCommandResultCache
}

trait AutowiringListSubmissionsCommandResultCache extends ListSubmissionsCommandResultCacheComponent {
	var listSubmissionsCommandResultCache: ListSubmissionsCommandResultCache = Wire[ListSubmissionsCommandResultCache]
}