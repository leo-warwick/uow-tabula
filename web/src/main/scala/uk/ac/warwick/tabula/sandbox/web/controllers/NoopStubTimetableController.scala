package uk.ac.warwick.tabula.sandbox.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, RequestParam}

import scala.xml.{Elem, XML}

/**
 * No-op responses for the sandbox, as there's never any real data to return.
 */
@Controller
class NoopStubTimetableController {

	@RequestMapping(value = Array("/stubTimetable/{year}"))
	def noop: Elem = {
		val xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><Data><Activities></Activities></Data>"
		XML.loadString(xml)
	}

}
