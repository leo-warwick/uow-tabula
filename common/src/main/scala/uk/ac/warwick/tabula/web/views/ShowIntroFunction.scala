package uk.ac.warwick.tabula.web.views
import org.springframework.beans.factory.annotation.Autowired

import freemarker.template.TemplateMethodModelEx
import freemarker.template.TemplateModel
import freemarker.template.TemplateModelException
import freemarker.template.utility.DeepUnwrap
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.tabula.services.UserSettingsService

/**
 * Freemarker directive to confirm whether an introductory popover should show automatically
 */
class ShowIntroFunction extends TemplateMethodModelEx {

	@Autowired var userSettings: UserSettingsService = _

	/**
	 * first argument is a string keying the feature to be hashed.
	 * second argument (optional) is string referencing the page where it's shown.
	 *
	 * Specifying a second common argument across several pages, or in a macro,
	 * permits a singleton introductory popover across the locations.
	 *
	 */
	override def exec(args: java.util.List[_]): Object = {
		val arguments = args.asInstanceOf[java.util.List[TemplateModel]]

		val location = Option(arguments) match {
			case Some(a) if a.size == 1 => RequestInfo.mappedPage
			case Some(a) if a.size == 2 => DeepUnwrap.unwrap(a.get(1)).asInstanceOf[String]
			case _ => throw new TemplateModelException("Invalid number of arguments")
		}

		val currentUser = RequestInfo.fromThread.get.user
		val featureToIntroduce = DeepUnwrap.unwrap(arguments.get(0)).asInstanceOf[String]

		val shaHash = UserSettings.Settings.hiddenIntroHash(location, featureToIntroduce)

		(userSettings.getByUserId(currentUser.apparentId) match {
			case Some(settings) if settings.hiddenIntros != null => !(settings.hiddenIntros contains(shaHash))
			case _ => true
		}): java.lang.Boolean
	}
}