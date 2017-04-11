package uk.ac.warwick.tabula

import scala.reflect._

import org.springframework.beans.factory.config.AbstractFactoryBean
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.{DeserializationFeature, SerializationFeature, ObjectMapper}
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

abstract class ScalaFactoryBean[A : ClassTag] extends AbstractFactoryBean[A] {
	override def getObjectType: Class[_] = classTag[A].runtimeClass
}

class JsonObjectMapperFactory extends ScalaFactoryBean[ObjectMapper] {
	override def createInstance: ObjectMapper = JsonObjectMapperFactory.createInstance
}

object JsonObjectMapperFactory {
	val instance: ObjectMapper = createInstance
	def createInstance: ObjectMapper = {
		val mapper = new ObjectMapper
		mapper.registerModule(DefaultScalaModule)
		mapper.registerModule(new JodaModule)
		mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
		mapper
	}
}

object JsonHelper {
	val mapper = new ObjectMapper with ScalaObjectMapper
	mapper.registerModule(DefaultScalaModule)
	mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

	def toJson(value: Map[Symbol, Any]): String = toJson(value map { case (k,v) => k.name -> v})

	def toJson(value: Any): String = mapper.writeValueAsString(value)

	def toMap[V](json: String)(implicit m: Manifest[V]): Map[String, V] = fromJson[Map[String,V]](json)

	def fromJson[T](json: String)(implicit m : Manifest[T]): T = mapper.readValue[T](json)
}

