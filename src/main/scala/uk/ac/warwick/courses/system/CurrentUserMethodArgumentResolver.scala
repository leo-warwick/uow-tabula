package uk.ac.warwick.courses.system

import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.core.MethodParameter
import uk.ac.warwick.courses.CurrentUser
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.context.request.RequestAttributes

/**
 * Allows you to put a CurrentUser argument in a @RequestMapping method, and it
 * will get resolved automagically by the dispatcher.
 * 
 * Configured in the XML with <mvc:argument-resolvers>.
 */
class CurrentUserMethodArgumentResolver extends HandlerMethodArgumentResolver {

  def supportsParameter(param:MethodParameter) = classOf[CurrentUser] isAssignableFrom param.getParameterType

  def resolveArgument(
	      param: MethodParameter, 
	      container: ModelAndViewContainer, 
	      req: NativeWebRequest, 
	      binderFactory: WebDataBinderFactory):Object = 
	  req.getAttribute(CurrentUser.keyName, RequestAttributes.SCOPE_REQUEST)
  

}