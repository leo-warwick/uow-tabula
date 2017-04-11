package uk.ac.warwick.tabula.api.web.controllers.jobs

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.helpers.JobInstanceToJsonConverter
import uk.ac.warwick.tabula.services.jobs.JobInstance
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/v1/job/{job}"))
class JobController extends ApiController
	with GetJobInformationApi
	with JobInstanceToJsonConverter

trait GetJobInformationApi {
	self: ApiController with JobInstanceToJsonConverter =>

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def getJob(@PathVariable job: JobInstance): Mav = {
		Mav(new JSONView(Map(
			"success" -> true,
			"status" -> "ok",
			"job" -> jsonJobInstanceObject(mandatory(job))
		)))
	}

}
