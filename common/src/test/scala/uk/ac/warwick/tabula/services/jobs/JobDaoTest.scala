package uk.ac.warwick.tabula.services.jobs

import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.jobs.TestingJob

// scalastyle:off magic.number
class JobDaoTest extends AppContextTestBase with HasJobDao {

	@Autowired var service: JobService = _

	@Test def crud() = transactional { _ =>

		val anHourAgo = DateTime.now.minusHours(1)
		val twoHoursAgo = DateTime.now.minusHours(2)
		val lastWeek = DateTime.now.minusWeeks(1)

		val inst1 = service.add(None, TestingJob("job1"))
		inst1.createdDate = twoHoursAgo
		val oldestInst = service.add(None, TestingJob("job2"))
		oldestInst.createdDate = lastWeek
		val inst3 = service.add(None, TestingJob("job3"))
		inst3.createdDate = twoHoursAgo
		val newestInst = service.add(None, TestingJob("job4"))
		newestInst.createdDate = anHourAgo
		val inst5 = service.add(None, TestingJob("job5"))
		inst5.createdDate = twoHoursAgo
		Seq(inst1, oldestInst, inst3, newestInst, inst5).foreach(jobDao.update)

		session.flush()
		session.clear()

		jobDao.getById(inst1.id) should be (Some(inst1))
		jobDao.getById(inst5.id) should be (Some(inst5))

		jobDao.findOutstandingInstance(inst1) should be (Some(inst1))
		jobDao.findOutstandingInstance(inst5) should be (Some(inst5))

		jobDao.findOutstandingInstances(5).length should be (5)
		val threeOutstanding = jobDao.findOutstandingInstances(3)
		threeOutstanding.length should be (3)
		/* TAB-4302 - return oldest jobs first */
		threeOutstanding.head should be (oldestInst)
		threeOutstanding.contains(newestInst) should be {false}

		val inst = jobDao.getById(inst1.id).get

		withClue("jobDao.findOutstandingInstances(5).contains(inst)") { jobDao.findOutstandingInstances(5).contains(inst) should be {true} }

		inst.started = true
		jobDao.update(inst)

		jobDao.findOutstandingInstance(inst1) should be (None)

		jobDao.findOutstandingInstances(5).length should be (4)
		withClue("jobDao.findOutstandingInstances(5).contains(inst)") { jobDao.findOutstandingInstances(5).contains(inst) should be {false} }

		jobDao.unfinishedInstances.length should be (5)
		withClue("jobDao.unfinishedInstances.contains(inst)") { jobDao.unfinishedInstances.contains(inst) should be {true} }
		jobDao.listRecent(0, 5).length should be (0)
		withClue("jobDao.listRecent(0, 5).contains(inst)") { jobDao.listRecent(0, 5).contains(inst) should be {false} }

		inst.finished = true
		jobDao.update(inst)

		jobDao.unfinishedInstances.length should be (4)
		withClue("jobDao.unfinishedInstances.contains(inst)") { jobDao.unfinishedInstances.contains(inst) should be {false} }
		jobDao.listRecent(0, 5).length should be (1)
		withClue("jobDao.listRecent(0, 5).contains(inst)") { jobDao.listRecent(0, 5).contains(inst) should be {true} }
	}

}