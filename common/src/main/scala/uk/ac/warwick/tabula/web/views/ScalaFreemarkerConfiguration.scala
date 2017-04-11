package uk.ac.warwick.tabula.web.views

import uk.ac.warwick.spring.Wire

import scala.collection.JavaConversions._

import org.springframework.beans.factory.annotation.Required
import org.springframework.web.context.ServletContextAware
import org.springframework.web.servlet.tags.form.FormTag

import freemarker.ext.beans.BeansWrapper
import freemarker.template.{ObjectWrapper, Configuration, TemplateDateModel, TemplateExceptionHandler}
import uk.ac.warwick.tabula.JavaImports._

/**
 * Adapted from http://code.google.com/p/sweetscala
 *
 * Sets Freemarker to use ScalaBeansWrapper will tells it how to
 * deal with the various Scala collection classes, so we don't
 * have to convert them to Java collections manually every time.
 */
class ScalaFreemarkerConfiguration extends Configuration(Configuration.VERSION_2_3_0) with ServletContextAware {
	// Default constructor init
	this.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX)
	this.setAutoIncludes(List("/WEB-INF/freemarker/prelude.ftl"))

	// Note that this doesn't affect inside servlet; see CustomFreemarkerExceptionHandler
	this.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER)

	this.setObjectWrapper(createWrapper(true))

	private def createWrapper(useCache: Boolean) = {
		val wrapper = new ScalaBeansWrapper
		wrapper.setMethodsShadowItems(false) // do not lookup method first.
		wrapper.setDefaultDateType(TemplateDateModel.DATETIME) //this allow java.util.Date to work from model.

		// TAB-351 TAB-469 Don't enable caching
		wrapper.setUseCache(false)
		wrapper.useWrapperCache = false
		wrapper.setExposureLevel(BeansWrapper.EXPOSE_SAFE)

		wrapper
	}

	// Mainly for tests to run - if servlet context is never set, it will
	// use the classloader to find templates.
	setClassForTemplateLoading(getClass, "/")

	@Required
	def setSharedVariables(vars: JMap[String, Any]) {
		val nonCachingWrapper = createWrapper(false)
		this.setSharedVariable("commandVarName", FormTag.MODEL_ATTRIBUTE_VARIABLE_NAME)
		for ((key, value) <- vars) this.setSharedVariable(key, nonCachingWrapper.wrap(value.asInstanceOf[Object]))
	}

	override def setServletContext(ctx: javax.servlet.ServletContext): Unit = {
		setServletContextForTemplateLoading(ctx, "/")
	}
}

trait ScalaFreemarkerConfigurationComponent {
	def freemarkerConfiguration: ScalaFreemarkerConfiguration
}

trait AutowiringScalaFreemarkerConfigurationComponent extends ScalaFreemarkerConfigurationComponent {
	var freemarkerConfiguration: ScalaFreemarkerConfiguration = Wire[ScalaFreemarkerConfiguration]
}