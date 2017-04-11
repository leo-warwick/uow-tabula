package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.helpers.StringUtils._

trait SmallGroupToJsonConverter {
	self: SmallGroupEventToJsonConverter =>

	def jsonSmallGroupObject(group: SmallGroup): Map[String, Any] = {
		val basicInfo = Map(
			"id" -> group.id,
			"name" -> group.name,
			"students" -> group.students.users.map { user =>
				Seq(
					user.getUserId.maybeText.map { "userId" -> _ },
					user.getWarwickId.maybeText.map { "universityId" -> _ }
				).flatten.toMap
			},
			"maxGroupSize" -> group.maxGroupSize,
			"events" -> group.events.map(jsonSmallGroupEventObject)
		)

		val linkedInfo = Option(group.linkedDepartmentSmallGroup).map { linkedGroup =>
			Map(
				"linkedDepartmentGroup" -> linkedGroup.id
			)
		}.getOrElse(Map())

		basicInfo ++ linkedInfo
	}
}
