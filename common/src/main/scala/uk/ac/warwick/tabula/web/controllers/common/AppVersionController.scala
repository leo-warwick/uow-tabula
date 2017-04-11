package uk.ac.warwick.tabula.web.controllers.common

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.{HttpHeaders, HttpStatus, MediaType, ResponseEntity}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class AppVersionController {

	@Value("${build.time}") var buildTime: String = _

	@RequestMapping(Array("/version"))
	def showTime = new ResponseEntity(buildTime, plainTextHeaders, HttpStatus.OK)

	def plainTextHeaders = new HttpHeaders {
		setContentType(MediaType.TEXT_PLAIN)
	}

}