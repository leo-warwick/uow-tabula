package uk.ac.warwick.tabula.web.controllers.home

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestParam}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.home.{ActivityStreamCommandState, ActivityStreamCommand}
import uk.ac.warwick.tabula.services.ActivityService._
import uk.ac.warwick.tabula.services.ActivityStreamRequest
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.{JSONView, MarkdownRendererImpl}

import scala.collection.JavaConverters._

@Controller
class ActivityStreamController extends BaseController with ActivityJsonMav with MarkdownRendererImpl {

	@ModelAttribute("command")
	def command(
			user: CurrentUser,
			@RequestParam(defaultValue="20") max: Int,
			@RequestParam(required=false) types: JList[String],
			@RequestParam(defaultValue="0") minPriority: Double, // minPriority of zero means we show all by default
			@RequestParam(defaultValue="false") includeDismissed: Boolean,
			@RequestParam(required=false) lastCreated: DateTime) = {
		val typeSet = if (types == null || types.isEmpty) None else Some(types.asScala.toSet)
		val request = ActivityStreamRequest(user.apparentUser, max, minPriority, includeDismissed, typeSet, Option(lastCreated))
		ActivityStreamCommand(request)
	}

	@RequestMapping(value=Array("/activity/@me"))
	def userStream(@ModelAttribute("command") command: Appliable[PagedActivities] with ActivityStreamCommandState) = {
		val activities = command.apply()
		val model = toModel(activities.items)

		val extraModel = Map(
			"max" -> command.request.max,
			"latest" -> command.request.lastUpdatedDate.isEmpty,
			"total" -> activities.totalHits,
			"lastCreated" -> activities.lastUpdatedDate
		)

		new JSONView(model ++ extraModel)
	}
}
