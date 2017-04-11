package uk.ac.warwick.tabula.system

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.springframework.web.method.HandlerMethod
import uk.ac.warwick.tabula.web.controllers.PreRequestHandler

class ControllerPreRequestInterceptor extends HandlerInterceptorAdapter {
	override def preHandle(request: HttpServletRequest, response: HttpServletResponse, obj: Any): Boolean = {
		obj match {
			case method: HandlerMethod => method.getBean match {
				case controller: PreRequestHandler => controller.preRequest
				case _ =>
			}
			case _ =>
		}
		true
	}
}