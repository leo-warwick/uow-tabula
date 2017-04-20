package uk.ac.warwick.tabula.system

import java.util.Properties

import org.springframework.context.ApplicationContextInitializer
import org.springframework.core.env.{MutablePropertySources, PropertiesPropertySource, PropertySource, PropertySourcesPropertyResolver}
import org.springframework.core.io.ClassPathResource
import org.springframework.web.context.ConfigurableWebApplicationContext
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.Logging

/**
  * We load config.properties in our Spring config, but that is too late for Spring
  * to find the value of spring.profiles.active as it's already started making beans.
  * So we load it also in this initializer.
  *
  * Also defines a few flags which in turn enable particular profiles.
  */
class ContextProfileInitializer extends ApplicationContextInitializer[ConfigurableWebApplicationContext] with Logging {

  val defaultConfig = "/default.properties"
  val mainConfig = "/tabula.properties"
  val optionalConfig = "/tabula-instance.properties"
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
    if (config.getBoolean(prop, default)) Some(profileName)
    else None

  var testConfig: PropertySource[_] = _

  lazy val config: CompositePropertySource = {
    val properties = new CompositePropertySource("config")
    if (testConfig == null) {
      properties.addRequiredSource(propertySource(mainConfig))
      properties.addRequiredSource(propertySource(defaultConfig))
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
    case Some(s) => mutableSources.addLast(s)
    case None =>
  }

  lazy val resolver = new PropertySourcesPropertyResolver(mutableSources)

  def getBoolean(prop: String, default: Boolean): Boolean = resolver.getProperty(prop, classOf[Boolean], default)
  def getString(prop: String): Object = getProperty(prop)

  override def getProperty(prop: String): String = resolver.getProperty(prop)
}