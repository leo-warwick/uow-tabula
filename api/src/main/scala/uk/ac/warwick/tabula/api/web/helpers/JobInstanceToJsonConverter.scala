package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.services.jobs.JobInstance

trait JobInstanceToJsonConverter {

	def jsonJobInstanceObject(job: JobInstance): Map[String, Any] = Map(
		"id" -> job.id,
		"jobType" -> job.jobType,
		"status" -> job.status,
		"data" -> job.json,
		"started" -> job.started,
		"finished" -> job.finished,
		"successful" -> job.succeeded,
		"progress" -> job.progress,
		"createdDate" -> DateFormats.IsoDateTime.print(job.createdDate),
		"updatedDate" -> DateFormats.IsoDateTime.print(job.updatedDate),
		"user" -> job.userId
	)

}
