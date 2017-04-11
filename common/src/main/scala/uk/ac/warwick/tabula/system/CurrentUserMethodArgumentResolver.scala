package uk.ac.warwick.tabula.system

import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

import uk.ac.warwick.tabula.CurrentUser

/**
 * Allows you to put a CurrentUser argument in a @RequestMapping method, and it
 * will get resolved automagically by the dispatcher.
 *
 * Configured in the XML with <mvc:argument-resolvers>.
 */
class CurrentUserMethodArgumentResolver extends HandlerMethodArgumentResolver {

	def supportsParameter(param: MethodParameter): Boolean = classOf[CurrentUser] isAssignableFrom param.getParameterType

	def resolveArgument(
		param: MethodParameter,
		container: ModelAndViewContainer,
		req: NativeWebRequest,
		binderFactory: WebDataBinderFactory): Object =
		req.getAttribute(CurrentUser.keyName, RequestAttributes.SCOPE_REQUEST)

}