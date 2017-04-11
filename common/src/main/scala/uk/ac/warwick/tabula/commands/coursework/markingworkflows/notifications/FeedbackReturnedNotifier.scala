package uk.ac.warwick.tabula.commands.coursework.markingworkflows.notifications

import uk.ac.warwick.tabula.commands.coursework.ReleasedState
import uk.ac.warwick.tabula.commands.{UserAware, Notifies}
import uk.ac.warwick.tabula.data.model.{Assignment, Notification, MarkerFeedback}
import uk.ac.warwick.tabula.data.model.notifications.coursework.ReturnToMarkerNotification
import uk.ac.warwick.tabula.helpers.Logging
import collection.JavaConverters._

trait FeedbackReturnedNotifier[A] extends Notifies[A, Seq[MarkerFeedback]] {

	this: ReleasedState with UserAware with Logging =>

	def blankNotification: ReturnToMarkerNotification

	def emit(commandResult: A): Seq[Notification[MarkerFeedback, Assignment]] = {

		// emit notifications to each marker that has returned feedback
		val markerMap : Map[String, Seq[MarkerFeedback]] = newReleasedFeedback.asScala.groupBy(mf => {
			val marker = mf.getMarkerUsercode
			marker.getOrElse("unassigned")
		})

		val unassignedFeedback = markerMap.get("unassigned")
		if(unassignedFeedback.isDefined){
			logger.warn(s"${unassignedFeedback.get.size} marker feedback were returned without a marker for ${assignment.name}")
		}

		markerMap.filterNot(_._1 == "unassigned").map{ case (usercode, markerFeedbacks) =>
			val notification = Notification.init(blankNotification, user, markerFeedbacks, assignment)
			notification.recipientUserId = usercode
			notification
		}.toSeq
	}
}
