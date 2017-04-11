package uk.ac.warwick.tabula.services.jobs

import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.Daoisms
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.Transactions._

/**
 * Provides low level access to JobDefinitions in the database.
 */
trait JobDao {
	def findOutstandingInstances(max: Int): Seq[JobInstance]
	def findOutstandingInstance(example: JobInstance): Option[JobInstance]
	def saveJob(instance: JobInstance): JobInstance
	def getById(id: String): Option[JobInstance]
	def unfinishedInstances: Seq[JobInstance]
	def listRecent(start: Int, count: Int): Seq[JobInstance]
	def update(instance: JobInstance): Unit
	def listRunningJobs: Seq[JobInstance]
	def findRunningJobs(schedulerInstance: String): Seq[JobInstance]
}

trait HasJobDao {
	@Autowired var jobDao: JobDao = _
}

@Service
class JobDaoImpl extends JobDao with Daoisms {
	import org.hibernate.criterion.Order._

	def findOutstandingInstances(max: Int): Seq[JobInstance] =
		transactional(readOnly = true) {
			session.newCriteria[JobInstanceImpl]
				.add(is("started", false))
				.addOrder(asc("createdDate"))
				.setMaxResults(max)
				.seq
		}

	def findOutstandingInstance(example: JobInstance): Option[JobInstance] = transactional(readOnly = true) {
		/*
		 * TAB-724 and TAB 3872
		 *
		 * We only check unstarted jobs here because started jobs may take a long time. It's perfectly possible
		 * that a user genuinely wants to create ANOTHER Turnitin submission job while the other one has been running
		 * for half an hour, and we don't have to worry about that happening because they will execute in separate
		 * transactions - the job runner won't start an identical job until this one is finished.
		 */

		session.newCriteria[JobInstanceImpl]
				.add(is("started", false))
				.add(is("jobType", example.jobType))
				.seq
				.find(_.json == example.json) // Do the find in pure Scala because it's a CLOB field and we can't do (Hibernate) queries directly
	}

	def getById(id: String): Option[JobInstanceImpl] = transactional(readOnly = true) {
		getById[JobInstanceImpl](id)
	}

	def saveJob(instance: JobInstance): JobInstanceImpl = transactional() {
		instance match {
			case instance: JobInstanceImpl =>
				session.save(instance)
				instance
			case _ => throw new IllegalArgumentException("JobDaoImpl only accepts JobInstanceImpls")
		}
	}

	def update(instance: JobInstance): Unit = transactional() {
		instance match {
			case instance: JobInstanceImpl => session.update(instance)
			case _ => throw new IllegalArgumentException("JobDaoImpl only accepts JobInstanceImpls")
		}
	}

	def unfinishedInstances: Seq[JobInstance] =
		session.newCriteria[JobInstanceImpl]
			.add(is("finished", false))
			.addOrder(desc("createdDate"))
			.seq

	def listRecent(start: Int, count: Int): Seq[JobInstance] =
		session.newCriteria[JobInstanceImpl]
			.add(is("finished", true))
			.addOrder(desc("createdDate"))
			.setFirstResult(start)
			.setMaxResults(count)
			.seq

	def listRunningJobs: Seq[JobInstance] =
		transactional(readOnly = true) {
			session.newCriteria[JobInstanceImpl]
				.add(is("started", true))
				.add(is("finished", false))
				.seq
		}

	def findRunningJobs(schedulerInstance: String): Seq[JobInstance] =
		transactional(readOnly = true) {
			session.newCriteria[JobInstanceImpl]
				.add(is("started", true))
				.add(is("finished", false))
				.add(is("schedulerInstance", schedulerInstance))
				.seq
		}
}