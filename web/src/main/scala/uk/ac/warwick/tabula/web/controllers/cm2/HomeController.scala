package uk.ac.warwick.tabula.web.controllers.cm2

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller

@Profile(Array("cm2Enabled")) @Controller
class HomeController extends CourseworkController {

	@RequestMapping(Array("/${cm2.prefix}"))
	def home() = Mav(s"$urlPrefix/home/view")

}
