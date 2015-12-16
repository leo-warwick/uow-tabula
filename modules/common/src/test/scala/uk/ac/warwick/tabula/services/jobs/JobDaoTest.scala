package uk.ac.warwick.tabula.services.jobs

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.data.HasJobDao
import uk.ac.warwick.tabula.jobs.TestingJob

// scalastyle:off magic.number
class JobDaoTest extends AppContextTestBase with HasJobDao {

	@Autowired var service: JobService = _

	@Test def crud() = transactional { t =>
		val inst1 = service.add(None, TestingJob("job1"))
		val inst2 = service.add(None, TestingJob("job2"))
		val inst3 = service.add(None, TestingJob("job3"))
		val inst4 = service.add(None, TestingJob("job4"))
		val inst5 = service.add(None, TestingJob("job5"))

		session.flush()
		session.clear()

		jobDao.getById(inst1.id) should be (Some(inst1))
		jobDao.getById(inst5.id) should be (Some(inst5))

		jobDao.findOutstandingInstance(inst1) should be (Some(inst1))
		jobDao.findOutstandingInstance(inst5) should be (Some(inst5))

		jobDao.findOutstandingInstances(5).length should be (5)
		jobDao.findOutstandingInstances(3).length should be (3)

		val inst = jobDao.getById(inst1.id).get

		jobDao.findOutstandingInstances(5).contains(inst) should be {true}

		inst.started = true
		jobDao.update(inst)

		jobDao.findOutstandingInstance(inst1) should be (None)

		jobDao.findOutstandingInstances(5).length should be (4)
		jobDao.findOutstandingInstances(5).contains(inst) should be {false}

		jobDao.unfinishedInstances.length should be (5)
		jobDao.unfinishedInstances.contains(inst) should be {true}
		jobDao.listRecent(0, 5).length should be (0)
		jobDao.listRecent(0, 5).contains(inst) should be {false}

		inst.finished = true
		jobDao.update(inst)

		jobDao.unfinishedInstances.length should be (4)
		jobDao.unfinishedInstances.contains(inst) should be {false}
		jobDao.listRecent(0, 5).length should be (1)
		jobDao.listRecent(0, 5).contains(inst) should be {true}
	}

}