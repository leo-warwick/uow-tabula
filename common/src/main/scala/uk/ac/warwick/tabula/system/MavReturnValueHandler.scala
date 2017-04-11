package uk.ac.warwick.tabula.system

import scala.collection.JavaConversions._
import org.springframework.core.MethodParameter
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodReturnValueHandler
import org.springframework.web.method.support.ModelAndViewContainer
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.Ordered
import uk.ac.warwick.tabula.web.Mav

/**
 * Allows a controller method to return a Mav instead of a ModelAndView.
 */
class MavReturnValueHandler extends HandlerMethodReturnValueHandler with Logging with Ordered {

	override def supportsReturnType(methodParam: MethodParameter): Boolean = {
		classOf[Mav] isAssignableFrom methodParam.getMethod.getReturnType
	}

	override def handleReturnValue(returnValue: Object,
		returnType: MethodParameter,
		mavContainer: ModelAndViewContainer,
		webRequest: NativeWebRequest): Unit =
		returnValue match {
			case mav: Mav => {
				mavContainer.addAllAttributes(mav.toModel)
				if (mav.viewName != null) {
					mavContainer.setViewName(mav.viewName)
				} else {
					mavContainer.setView(mav.view)
				}
				mavContainer.setRequestHandled(false)
			}
		}

}