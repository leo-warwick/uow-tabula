package uk.ac.warwick.tabula.web.controllers.home

import org.joda.time.format.{DateTimeFormatter, ISODateTimeFormat}
import uk.ac.warwick.tabula.data.model.Activity
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{JSONView, MarkdownRenderer}

trait ActivityJsonMav {
	self: MarkdownRenderer =>

	val DateFormat: DateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis()

	def toModel(activities: Seq[Activity[_]]) = Map("items" -> activities.map { item =>
		val source = item.message
		val html = renderMarkdown(source)

		Map(
			"_id" -> item.id,
			"published" -> DateFormat.print(item.date),
			"priority" -> item.priority,
			"title" -> item.title,
			"url" -> item.url,
			"urlTitle" -> item.urlTitle,
			"content" -> html,
			"verb" -> item.verb
		)
	})

	def toMav(activities: Seq[Activity[_]]) = Mav(
		new JSONView(toModel(activities))
	)


}
