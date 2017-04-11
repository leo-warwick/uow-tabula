package uk.ac.warwick.tabula.services.jobs
import uk.ac.warwick.tabula.jobs.TestingJob
import uk.ac.warwick.tabula.{Mockito, TestBase}

// scalastyle:off magic.number
class JobServiceTest extends TestBase with Mockito {

	val service = new JobService
	val jobDao: JobDao = smartMock[JobDao]
	service.jobDao = jobDao

	@Test def add() {
		service.jobs = Array(new TestingJob)
		jobDao.findOutstandingInstance(any[JobInstanceImpl]) returns None
		val inst = service.add(None, TestingJob("job"))
		verify(jobDao, times(1)).saveJob(inst)
	}

	@Test def run() = withUser("cuscav") {
		jobDao.listRunningJobs returns Nil
		jobDao.findOutstandingInstance(any[JobInstance]) returns None

		val job = new TestingJob
		job.jobService = service

		service.jobDao = jobDao
		service.jobs = Array(job)

		val inst = service.add(Some(currentUser), TestingJob("job", sleepTime = 50))
		verify(jobDao, times(1)).saveJob(inst)

		jobDao.findOutstandingInstances(10) returns Seq(inst)

		service.run()
		jobDao.listRunningJobs returns Seq(inst)

		verify(jobDao, atLeast(1)).update(inst)

		inst.finished should be {true}
		inst.progress should be (100)
		inst.started should be {true}
		inst.status should be ("Finished the job!")
		inst.succeeded should be {true}
		inst.updatedDate should not be null

		// If we try and kill the instance now, then nothing will happen

	}

	@Test def kill() = withUser("cuscav") {
		jobDao.findOutstandingInstance(any[JobInstance]) returns None

		val job = new TestingJob
		job.jobService = service

		service.jobDao = jobDao
		service.jobs = Array(job)

		val inst = service.add(Some(currentUser), TestingJob("job"))
		verify(jobDao, times(1)).saveJob(inst)

		jobDao.findOutstandingInstances(10) returns Seq(inst)

		service.kill(inst)

		verify(jobDao, atLeast(1)).update(inst)

		inst.progress should be (0)
		inst.status should be ("Killed")
		inst.finished should be {true}
		inst.succeeded should be {false}
		inst.updatedDate should not be null
	}

	@Test def keep10JobsRunning() = withUser("cuscav") {
		val job = new TestingJob
		job.jobService = service
		service.jobDao = jobDao
		service.jobs = Array(job)

		jobDao.findOutstandingInstance(any[JobInstance]) returns None

		val runningJob1 = TestingJob("runningJob1", sleepTime = 1)
		val runningJob2 = TestingJob("runningJob2", sleepTime = 1)
		val runningJob3 = TestingJob("runningJob3", sleepTime = 1)
		val runningJob4 = TestingJob("runningJob4", sleepTime = 1)
		val runningJob5 = TestingJob("runningJob5", sleepTime = 1)
		val runningJobs = Seq(runningJob1, runningJob2, runningJob3, runningJob4, runningJob5)
		val runningJobInstances = runningJobs.map(job => service.add(Some(currentUser), job))
		runningJobInstances.foreach(instance => verify(jobDao, times(1)).saveJob(instance))

		jobDao.listRunningJobs returns Nil
		jobDao.findOutstandingInstances(10) returns runningJobInstances

		service.run()

		// Above jobs now running (they'll actually finish in the test, but the mocked DAO will return them as running)
		runningJobInstances.foreach(job => job.started should be {true})

		val outstandingJob1 = TestingJob("outstandingJob1", sleepTime = 1)
		val outstandingJob2 = TestingJob("outstandingJob2", sleepTime = 1)
		val outstandingJob3 = TestingJob("outstandingJob3", sleepTime = 1)
		val outstandingJob4 = TestingJob("outstandingJob4", sleepTime = 1)
		val outstandingJob5 = TestingJob("outstandingJob5", sleepTime = 1)
		val outstandingJobs = Seq(outstandingJob1, outstandingJob2, outstandingJob3, outstandingJob4, outstandingJob5)
		val outstandingJobInstances = outstandingJobs.map(job => service.add(Some(currentUser), job))
		outstandingJobInstances.foreach(instance => verify(jobDao, times(1)).saveJob(instance))

		jobDao.listRunningJobs returns runningJobInstances
		jobDao.findOutstandingInstances(5) returns outstandingJobInstances

		service.run()

		verify(jobDao, times(1)).findOutstandingInstances(10) // Looks for 10 the first time
		verify(jobDao, times(1)).findOutstandingInstances(5) // Looks for 5 the second time (10 less 5 running)
		outstandingJobInstances.foreach(job => job.started should be {true})

	}

	@Test def dontStartIdenticalInParallel() = withUser("cuscav") {
		val job = new TestingJob
		job.jobService = service
		service.jobDao = jobDao
		service.jobs = Array(job)

		jobDao.findOutstandingInstance(any[JobInstance]) returns None

		val runningJob1 = TestingJob("runningJob1", sleepTime = 1)
		val runningJobInstances = Seq(runningJob1).map(job => service.add(Some(currentUser), job))
		runningJobInstances.foreach(instance => verify(jobDao, times(1)).saveJob(instance))

		jobDao.listRunningJobs returns Nil
		jobDao.findOutstandingInstances(10) returns runningJobInstances

		service.run()

		// Above jobs now running (they'll actually finish in the test, but the mocked DAO will return them as running)
		runningJobInstances.foreach(job => job.started should be {true})

		val identicalJob = TestingJob("runningJob1", sleepTime = 1)
		val outstandingJobs = Seq(identicalJob)
		val outstandingJobInstances = outstandingJobs.map(job => service.add(Some(currentUser), job))
		outstandingJobInstances.foreach(instance => verify(jobDao, times(1)).saveJob(instance))

		jobDao.listRunningJobs returns runningJobInstances
		jobDao.findOutstandingInstances(9) returns outstandingJobInstances

		service.run()

		verify(jobDao, times(1)).findOutstandingInstances(10) // Looks for 10 the first time
		verify(jobDao, times(1)).findOutstandingInstances(9) // Looks for 9 the second time (10 less 1 running)
		outstandingJobInstances.foreach(job => job.started should be {false}) // Identical job should not be started

	}

}