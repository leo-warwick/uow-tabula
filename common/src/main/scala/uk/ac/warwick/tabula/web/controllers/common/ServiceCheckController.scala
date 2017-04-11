package uk.ac.warwick.tabula.web.controllers.common

import org.springframework.context.Lifecycle
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, ResponseBody}

/**
	* Provides an endpoint for a service check to verify the state the app is in.
	*/
@Controller
class ServiceCheckController extends Lifecycle {

	private var shutdown = false

	@RequestMapping(value = Array("/service/gtg"), produces = Array(MediaType.TEXT_PLAIN_VALUE))
	@ResponseBody def gtg: ResponseEntity[String] =
		if (shutdown) {
			new ResponseEntity[String](HttpStatus.INTERNAL_SERVER_ERROR)
		} else {
			new ResponseEntity[String]("\"OK\"", HttpStatus.OK)
		}

	override def isRunning: Boolean = !shutdown

	override def start(): Unit = {}

	override def stop(): Unit = {
		shutdown = true
	}

}
