package uk.ac.warwick.tabula.system

import org.springframework.core.MethodParameter
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.{HandlerMethodReturnValueHandler, ModelAndViewContainer}
import uk.ac.warwick.tabula.web.views.XmlView

import scala.xml.Elem

/**
 * Allows you to return a Scala XML Elem object from a controller RequestMapping,
 * and it will render it.
 */
class XmlReturnValueHandler extends HandlerMethodReturnValueHandler {

	override def supportsReturnType(methodParam: MethodParameter): Boolean = {
		classOf[Elem] isAssignableFrom methodParam.getMethod.getReturnType
	}

	override def handleReturnValue(returnValue: Object,
		returnType: MethodParameter,
		mavContainer: ModelAndViewContainer,
		webRequest: NativeWebRequest): Unit =
		returnValue match {
			case xml: Elem => mavContainer.setView(new XmlView(xml))
		}
}
