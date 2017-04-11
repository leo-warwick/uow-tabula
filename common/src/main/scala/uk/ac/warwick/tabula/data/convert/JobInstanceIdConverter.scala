package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.AssessmentService
import uk.ac.warwick.tabula.services.jobs.{JobService, JobInstance}
import uk.ac.warwick.tabula.system.TwoWayConverter

class JobInstanceIdConverter extends TwoWayConverter[String, JobInstance] {

	@Autowired var service: JobService = _

	override def convertRight(id: String): JobInstance = Option(id).flatMap(service.getInstance).orNull
	override def convertLeft(job: JobInstance): String = Option(job).map { _.id }.orNull

}