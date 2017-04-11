package uk.ac.warwick.tabula.system

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import org.springframework.core.MethodParameter
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.{ModelAndViewContainer, HandlerMethodReturnValueHandler}
import org.springframework.web.servlet.View
import uk.ac.warwick.tabula.AutowiringFeaturesComponent
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.{Ordered, Logging}
import uk.ac.warwick.tabula.services.fileserver.{StreamsFiles, RenderableFile}

/**
 * Allows a controller method to return a RenderableFile
 */
class RenderableFileReturnValueHandler extends HandlerMethodReturnValueHandler with Logging with Ordered {

	override def supportsReturnType(methodParam: MethodParameter): Boolean =
		classOf[RenderableFile] isAssignableFrom methodParam.getMethod.getReturnType

	override def handleReturnValue(
		returnValue: Object,
		returnType: MethodParameter,
		mavContainer: ModelAndViewContainer,
		webRequest: NativeWebRequest): Unit = returnValue match {
			case file: RenderableFile => mavContainer.setView(new RenderableFileView(file))
		}
}

class RenderableFileView(file: RenderableFile) extends View with StreamsFiles with AutowiringFeaturesComponent {
	def getContentType: String = file.contentType
	def render(model: JMap[String, _], in: HttpServletRequest, out: HttpServletResponse) {
		stream(file)(in, out)
	}
}
