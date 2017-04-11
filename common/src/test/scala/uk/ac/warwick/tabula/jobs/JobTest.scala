package uk.ac.warwick.tabula.jobs

import uk.ac.warwick.tabula.TestBase
import org.junit.Before
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.CurrentUser

import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.Transactions

// scalastyle:off magic.number
class JobTest extends TestBase with Mockito with JobTestHelp {

	val testingJob = new TestingJob
	override def createJobs: Array[Job] = Array[Job](testingJob)

	@Before def setup() {
		dao.clear
		val realUser = new User("real")
		val fakeUser = new User("apparent")
		currentUser = new CurrentUser(realUser, fakeUser)
	}

	@Test def testingJobTest() {
		Transactions.disable {
			dao.findOutstandingInstances(10).size should be (0)
			val id = service.add(Some(currentUser), TestingJob("Magic")).id
			dao.findOutstandingInstances(10).size should be (1)
			val myInstance = service.getInstance(id).get
			myInstance.started should be (right = false)
			service.run()
			myInstance.finished should be (right = true)
			myInstance.succeeded should be (right = true)
			dao.findOutstandingInstances(10).size should be (0)
		}
	}

}