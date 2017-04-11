package uk.ac.warwick.tabula.data.model

import java.sql.Types

import com.fasterxml.jackson.databind.ObjectMapper
import org.hibernate.`type`.StandardBasicTypes
import uk.ac.warwick.tabula.JsonObjectMapperFactory
import uk.ac.warwick.tabula.helpers.Logging

/**
 * Stores a Map[String, Any] as JSON and inflates it back out.
 * &lt;Paul Daniels&gt;Magic!&lt;/Paul Daniels&gt;
  */
class JsonMapUserType extends AbstractBasicUserType[Map[String, Any], String] with Logging {

	/** Sad face, Hibernate user types are instantiated in a weird way that make dependency injection hard */
	lazy val jsonMapper: ObjectMapper = JsonObjectMapperFactory.instance

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String): Map[String, Any] =
		if (jsonMapper != null) jsonMapper.readValue(string, classOf[Map[String, Any]])
		else {
			logger.warn("No JSON mapper defined. This should only happen in unit tests!")
			nullObject
		}

	override def convertToValue(map: Map[String, Any]): String =
		if (jsonMapper != null) jsonMapper.writeValueAsString(map)
		else {
			logger.warn("No JSON mapper defined. This should only happen in unit tests!")
			nullValue
		}

}