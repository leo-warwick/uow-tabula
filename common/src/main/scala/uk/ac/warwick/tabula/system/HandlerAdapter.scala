package uk.ac.warwick.tabula.system

import com.fasterxml.jackson.databind.{SerializationFeature, ObjectMapper}
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.springframework.core.convert.support.GenericConversionService
import org.springframework.format.support.DefaultFormattingConversionService
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer
import org.springframework.web.method.support.HandlerMethodReturnValueHandler
import collection.JavaConverters._
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import uk.ac.warwick.tabula.JavaImports._
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite
import org.springframework.web.method.support.InvocableHandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.ServletRequestDataBinderFactory
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter

/**
 * Extension of RequestMappingHandlerAdapter that allows you to place
 * custom HandlerMethodReturnValueHandlers _before_ the default ones, without having
 * to replace them entirely.
 */
class HandlerAdapter extends org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter {
	var customPreReturnValueHandlers: JList[HandlerMethodReturnValueHandler] = JArrayList()
	var customConverters: JSet[_] = JHashSet()

	/*
	 * There used to be a protected method we could override but now it's all private, so
	 * we use reflection to modify the private field on startup.
	 */
	private val returnValueHandlersField = {
		val field = classOf[RequestMappingHandlerAdapter].getDeclaredField("returnValueHandlers")
		field.setAccessible(true)
		field
	}

	override def afterPropertiesSet: Unit = {
		super.afterPropertiesSet()
		val defaultHandlers = returnValueHandlersField.get(this).asInstanceOf[HandlerMethodReturnValueHandlerComposite]
		val composite = new HandlerMethodReturnValueHandlerComposite
		composite.addHandlers(customPreReturnValueHandlers)
		composite.addHandler(defaultHandlers)
		returnValueHandlersField.set(this, composite)

		val converter = getMessageConverters.asScala.collectFirst {
			case c: MappingJackson2HttpMessageConverter => c
		}.getOrElse {
			val c = new MappingJackson2HttpMessageConverter()
			getMessageConverters.add(c)
			c
		}

		val mapper = new ObjectMapper
		mapper.registerModule(DefaultScalaModule)
		mapper.registerModule(new JodaModule)
		mapper.registerModule(new TwoWayConvertersJsonModule(customConverters.asScala.collect { case c: TwoWayConverter[String @unchecked, _] => c }))
		mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)

		getWebBindingInitializer
			.asInstanceOf[ConfigurableWebBindingInitializer]
			.getConversionService
			.asInstanceOf[DefaultFormattingConversionService]

		converter.setObjectMapper(mapper)
	}

	override def createDataBinderFactory(binderMethods: JList[InvocableHandlerMethod]): ServletRequestDataBinderFactory =
		new CustomDataBinderFactory(binderMethods.asScala.toList, getWebBindingInitializer())

}

class TwoWayConvertersJsonModule(converters: Iterable[TwoWayConverter[String, _]]) extends SimpleModule {
	converters.foreach { c =>
		addDeserializer(c.typeB.runtimeClass.asInstanceOf[Class[Any]], c.asJsonDeserializer)
	}

	override def getModuleName: String = getClass.getSimpleName
	override def hashCode: Int = getClass.hashCode
	override def equals(other: Any): Boolean =
		other match {
			case that: TwoWayConvertersJsonModule => this.eq(that) // Reference equality
			case _ => false
		}

}
