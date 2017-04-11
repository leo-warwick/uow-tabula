package uk.ac.warwick.tabula.web.views
import scala.collection.JavaConverters._


import org.springframework.beans.factory.annotation.Autowired

import freemarker.template.TemplateMethodModelEx
import freemarker.template.TemplateModel
import freemarker.template.utility.DeepUnwrap
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.services.UserSettingsService

/**
 * Freemarker directive to retrieve a user setting
 */
class UserSettingFunction extends TemplateMethodModelEx {

	@Autowired var userSettings: UserSettingsService = _

	override def exec(args: java.util.List[_]): Object = {
		val arguments = args.asScala.toSeq.map { model => DeepUnwrap.unwrap(model.asInstanceOf[TemplateModel]) }

		val setting = arguments.toList match {
			case (setting: String) :: Nil => setting
			case _ => throw new IllegalArgumentException("Bad args: " + arguments)
		}

		val currentUser = RequestInfo.fromThread.get.user

		userSettings.getByUserId(currentUser.apparentId) match {
			case Some(settings) => settings.string(setting)
			case _ => null
		}
	}
}