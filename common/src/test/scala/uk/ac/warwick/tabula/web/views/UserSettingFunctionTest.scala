package uk.ac.warwick.tabula.web.views

import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.JavaImports._
import freemarker.template.{ObjectWrapper, TemplateModel, SimpleHash}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.services.UserSettingsService
import uk.ac.warwick.tabula.data.model.UserSettings

class UserSettingFunctionTest extends TestBase with Mockito {

	val fn = new UserSettingFunction

	val settingsService: UserSettingsService = mock[UserSettingsService]
	fn.userSettings = settingsService

	@Test def noSettings = withUser("cuscav") {
		val args: JList[TemplateModel] = JArrayList()

		// Use a SimpleHash as a workaround to wrapping things manually
		val model = new SimpleHash(null.asInstanceOf[ObjectWrapper])
		model.put("settingName", UserSettings.Settings.BulkEmailSeparator)

		args.add(model.get("settingName"))

		settingsService.getByUserId("cuscav") returns (None)

		fn.exec(args) should be (null)
	}

	@Test def hasSetting = withUser("cuscav") {
		val args: JList[TemplateModel] = JArrayList()

		// Use a SimpleHash as a workaround to wrapping things manually
		val model = new SimpleHash(null.asInstanceOf[ObjectWrapper])
		model.put("settingName", UserSettings.Settings.BulkEmailSeparator)

		args.add(model.get("settingName"))

		val settings = new UserSettings
		settings.bulkEmailSeparator = ","

		settingsService.getByUserId("cuscav") returns (Some(settings))

		fn.exec(args) should be (",")
	}

	@Test def doesntHaveSetting = withUser("cuscav") {
		val args: JList[TemplateModel] = JArrayList()

		// Use a SimpleHash as a workaround to wrapping things manually
		val model = new SimpleHash(null.asInstanceOf[ObjectWrapper])
		model.put("settingName", UserSettings.Settings.BulkEmailSeparator)

		args.add(model.get("settingName"))

		val settings = new UserSettings

		settingsService.getByUserId("cuscav") returns (Some(settings))

		fn.exec(args) should be (null)
	}

}