package uk.ac.warwick.tabula.system

import java.util.Properties
import scala.collection.JavaConversions._
import org.springframework.context.ApplicationContextInitializer
import org.springframework.core.env.MutablePropertySources
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.env.PropertySource
import org.springframework.core.io.ClassPathResource
import org.springframework.web.context.ConfigurableWebApplicationContext
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.spring.Wire

/**
 * We load config.properties in our Spring config, but that is too late for Spring
 * to find the value of spring.profiles.active as it's already started making beans.
 * So we load it also in this initializer.
 *
 * Also defines a few flags which in turn enable particular profiles.
 */
class ContextProfileInitializer extends ApplicationContextInitializer[ConfigurableWebApplicationContext] with Logging {

	var mainConfig = "/tabula.properties"
	var optionalConfig = "/tabula-instance.properties"
	val profilesProperty = "spring.profiles.active"

	override def initialize(ctx: ConfigurableWebApplicationContext): Unit = {
		logger.info("Initialising context")

		Wire.ignoreMissingContext = true

		val profiles = resolve()
		ctx.getEnvironment.setActiveProfiles(profiles: _*)
	}

	def resolve(): Seq[String] = {
		// get profile listed in spring.profiles.active property
		val profiles = config.getString(profilesProperty) match {
			case s: String => s.split(",").toList
			case _ => Nil
		}
		// add any additional profiles based on flags
		profiles ++ extraProfiles
	}

	def extraProfiles: Iterable[String] = scheduler ++ web ++ cm1Enabled ++ cm2Enabled

	def scheduler: Option[String] = extraProfile("scheduling.enabled", "scheduling", default = false)
	def web: Option[String] = extraProfile("web.enabled", "web", default = true)
	def cm1Enabled: Option[String] = extraProfile("cm1.enabled", "cm1Enabled", default = true)
	def cm2Enabled: Option[String] = extraProfile("cm2.enabled", "cm2Enabled", default = false)

	/**
	 * Function that checks a config property and returns an Option[String] of
	 * a profile name if it should be enabled.
	 */
	def extraProfile(prop: String, profileName: String, default: Boolean): Option[String] =
		config.getBoolean(prop, default) match {
			case true => Some(profileName)
			case false => None
		}

	var testConfig: PropertySource[_] = _

	lazy val config: CompositePropertySource = {
		val properties = new CompositePropertySource("config")
		if (testConfig == null) {
			properties.addRequiredSource(propertySource(mainConfig))
			properties.addOptionalSource(propertySource(optionalConfig))
		} else {
			properties.addRequiredSource(Option(testConfig))
		}
		properties
	}

	private def propertySource(name: String) = {
		val props = new Properties
		val resource = new ClassPathResource(name)
		if (resource.exists) {
			props load resource.getInputStream
			Some(new PropertiesPropertySource(name, props))
		} else {
			None
		}
	}

}

class CompositePropertySource(name: String) extends PropertySource[Unit](name, ()) {
	val mutableSources = new MutablePropertySources

	def addRequiredSource(src: Option[PropertySource[_]]): Unit =
		mutableSources.addLast(src.getOrElse(throw new IllegalArgumentException("required property source missing")))

	def addOptionalSource(src: Option[PropertySource[_]]): Unit = src match {
		case Some(source) => mutableSources.addLast(source)
		case None =>
	}

	def getBoolean(prop: String, default: Boolean): Boolean = getString(prop) match {
		case "true" => true
		case "false" => false
		case _ => default
	}

	def getString(prop: String): Object = getProperty(prop) match {
		case value: Any => value.toString
		case _ => null
	}

	override def getProperty(prop: String): Object =
		(mutableSources.find { _.containsProperty(prop) } map { _.getProperty(prop) }).orNull

}