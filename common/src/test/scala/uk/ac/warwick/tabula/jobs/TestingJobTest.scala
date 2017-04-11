package uk.ac.warwick.tabula.jobs

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.services.jobs.JobInstanceImpl
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.services.jobs.JobService

class TestingJobTest extends TestBase with Mockito {

	@Test def run {
		val instance = JobInstanceImpl.fromPrototype(TestingJob("test job"))

		withClue("Succeeded") { instance.succeeded should be (false) }

		instance.progress should be (0)
		instance.status should be (null)

		val job = new TestingJob()
		job.jobService = mock[JobService]

		job.run(instance)

		// Check that the flags have been updated.
		withClue("Succeeded") { instance.succeeded should be (true) }

		instance.progress should be (100)
		instance.status should be ("Finished the job!")
	}

}