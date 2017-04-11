package uk.ac.warwick.tabula.data.model.notifications.groups

import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, ScalaFreemarkerConfiguration}
import uk.ac.warwick.tabula.{FreemarkerTestHelpers, SmallGroupFixture, TestBase}

class OpenGroupSetNotificationTemplateTest extends TestBase with FreemarkerTestHelpers with FreemarkerRendering {

	private trait NotificationFixture extends SmallGroupFixture {

		val urlModel = new StubFreemarkerDirectiveModel

		implicit val config: ScalaFreemarkerConfiguration = newFreemarkerConfiguration
	}

	@Test
	def includesTheNameOfEachGroupSet {
		new NotificationFixture {
			val output: String =
				renderToString(OpenSmallGroupSetsNotification.templateLocation,
					Map("formatsString" -> "great", "groupsets" -> List(groupSet1, groupSet2), "profileUrl" -> "profileUrl"))
			output should include(groupSet1.name)
			output should include(groupSet2.name)
		}}


}
