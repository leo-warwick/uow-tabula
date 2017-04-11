package uk.ac.warwick.tabula.web.controllers.common

import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, MediaType, ResponseEntity}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, ResponseBody}
import uk.ac.warwick.tabula.JsonObjectMapperFactory
import uk.ac.warwick.tabula.services.healthchecks.ServiceHealthcheckProvider

@Controller
class ServiceHealthcheckController {

	/** Spring should wire in all beans that extend ServiceHealthcheckProvider */
	@Autowired(required = false) var healthcheckProviders: Array[ServiceHealthcheckProvider] = Array()

	@RequestMapping(value = Array("/service/healthcheck"), produces = Array(MediaType.APPLICATION_JSON_VALUE))
	@ResponseBody def healthcheck: ResponseEntity[String] = {
		// We don't return any checks older than 15 minutes, which will force the monitoring check into UNKNOWN
		val fifteenMinutesAgo = DateTime.now.minusMinutes(15)

		val json = Map(
			"data" -> healthcheckProviders.flatMap(_.latest).filter(_.testedAt.isAfter(fifteenMinutesAgo)).map(_.asMap)
		)

		new ResponseEntity[String](JsonObjectMapperFactory.instance.writeValueAsString(json), HttpStatus.OK)
	}

}
