package uk.ac.warwick.courses.web.controllers
import org.apache.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import uk.ac.warwick.courses.system.ExceptionResolver

@Controller
class ErrorController {

  // TODO some extra error reporting 
  val logger = Logger.getLogger("Exceptions")
  
  @Autowired var exceptionResolver:ExceptionResolver =_
  
  @RequestMapping(Array("/error"))
  def generalError(request:HttpServletRequest):Mav = {
	exceptionResolver.doResolve(request.getAttribute("javax.servlet.error.exception").asInstanceOf[Throwable])
  }
  
  @RequestMapping(Array("/error/404"))
  def pageNotFound(@RequestHeader("X-Requested-Uri") requestedUri:String) = {
    Mav("errors/404", "requestedUri"->requestedUri)
  }
  
}