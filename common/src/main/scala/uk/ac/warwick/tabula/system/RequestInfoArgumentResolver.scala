package uk.ac.warwick.tabula.system

import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.core.MethodParameter
import org.springframework.web.method.support.ModelAndViewContainer
import uk.ac.warwick.tabula.RequestInfo

/**
 * Allows specifying a RequestInfo object as a method argument which will be
 * resolved by Spring.
 */
class RequestInfoArgumentResolver extends HandlerMethodArgumentResolver {

	def supportsParameter(parameter: MethodParameter): Boolean =
		classOf[RequestInfo] isAssignableFrom parameter.getParameterType

	def resolveArgument(
		param: MethodParameter,
		container: ModelAndViewContainer,
		req: NativeWebRequest,
		binderFactory: WebDataBinderFactory): Object = RequestInfo.fromThread

}