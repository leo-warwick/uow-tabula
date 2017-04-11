package uk.ac.warwick.tabula.commands.coursework

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.{Notifies, UserAware}
import uk.ac.warwick.tabula.data.model.notifications.coursework.ReleaseToMarkerNotification
import uk.ac.warwick.tabula.data.model.{Assignment, MarkerFeedback, Notification}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.UserLookupComponent

import scala.collection.JavaConverters._

trait FeedbackReleasedNotifier[A] extends Notifies[A, Seq[MarkerFeedback]] {

	this: ReleasedState with UserAware with UserLookupComponent with Logging =>

	def blankNotification: ReleaseToMarkerNotification

	def emit(commandResult: A): Seq[Notification[MarkerFeedback, Assignment]] = {
		// emit notifications to each second marker that has new feedback
		val markerMap : Map[String, Seq[MarkerFeedback]] = newReleasedFeedback.asScala.groupBy(mf => {
			val marker = mf.getMarkerUsercode
			marker.getOrElse("unassigned")
		})

		val unassignedFeedback = markerMap.get("unassigned")
		if(unassignedFeedback.isDefined){
			logger.warn(s"${unassignedFeedback.get.size} marker feedback were released without a second marker for ${assignment.name}")
		}

		markerMap.filterNot(_._1 == "unassigned").map{ case (usercode, markerFeedbacks) =>
			val notification = Notification.init(blankNotification, user, markerFeedbacks, assignment)
			notification.recipientUserId = usercode
			notification
		}.toSeq
	}
}

trait ReleasedState {
	val assignment: Assignment
	var newReleasedFeedback: JList[MarkerFeedback] = JArrayList()
}
