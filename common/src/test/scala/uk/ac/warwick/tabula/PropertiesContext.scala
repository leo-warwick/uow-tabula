package uk.ac.warwick.tabula

import java.util.Properties

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer

/**
 * FunctionalContext class for defining a set of properties in a
 * placeholder configurer, so that Wired string properties are picked
 * up. Subclass this with some properties.
 */
abstract class PropertiesContext(properties: Map[String, String]) extends FunctionalContext {
	bean[PropertyPlaceholderConfigurer]("properties") {
		val cfg = new PropertyPlaceholderConfigurer
		val props = new Properties()
		properties.foreach{ case (k,v) => props.put(k,v) }
		cfg.setProperties(props)
		cfg
	}
}

