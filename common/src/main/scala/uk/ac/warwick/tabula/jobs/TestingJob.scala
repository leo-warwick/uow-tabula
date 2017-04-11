package uk.ac.warwick.tabula.jobs

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.services.jobs._

object TestingJob {
	val id = "testing"
	val DefaultDelay = 500

	def apply(name: String, sleepTime: Int = 0) = JobPrototype(id, Map(
		"name" -> name,
		"sleepTime" -> sleepTime))
}

@Component
class TestingJob extends Job {
	val identifier: String = TestingJob.id

	def run(implicit job: JobInstance) {
		val name = job.getString("name")
		val sleepTime = job.getString("sleepTime").toInt
		updateStatus("Running the job with name %s." format name)
		for (i <- 1 to 50) {
			updateProgress(i*2)
			if (sleepTime != 0) Thread.sleep(10)
		}
		job.succeeded = true
		updateStatus("Finished the job!")
	}

}

