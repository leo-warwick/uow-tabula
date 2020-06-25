package uk.ac.warwick.tabula.services.jobs

import java.net.InetAddress
import java.util.UUID

import javax.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.events.JobNotificationHandling
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.jobs._
import uk.ac.warwick.tabula.{CurrentUser, EarlyRequestInfo}
import uk.ac.warwick.userlookup.User

import scala.collection.parallel.CollectionConverters._

trait JobServiceComponent {
  def jobService: JobService
}

trait AutowiringJobServiceComponent extends JobServiceComponent {
  var jobService: JobService = Wire[JobService]
}

@Service
class JobService extends HasJobDao with Logging with JobNotificationHandling {

  import uk.ac.warwick.tabula.data.Transactions._

  // How many jobs to load and run each time
  val RunBatchSize = 25

  var stopping = false

  lazy val schedulerInstance: String = {
    try {
      InetAddress.getLocalHost.getHostName
    }
    catch {
      case e: Exception =>
        logger.warn("Couldn't get host name for scheduler instance; returning random ID")
        UUID.randomUUID().toString
    }
  }

  /** Spring should wire in all beans that extend Job */
  @Autowired var jobs: Array[Job] = Array()

  def run()(implicit earlyRequestInfo: EarlyRequestInfo): Unit = {
    val runningJobs = jobDao.listRunningJobs
    if (runningJobs.size < RunBatchSize) {
      val jobsToRun = jobDao.findOutstandingInstances(RunBatchSize - runningJobs.size)
      if (jobsToRun.nonEmpty) {
        logger.info(s"Found ${jobsToRun.size} jobs to run")
        val nonIdenticalJobsToRun = jobsToRun.filterNot(prospectiveJob => runningJobs.exists(runningJob =>
          runningJob.jobType == prospectiveJob.jobType && runningJob.json == prospectiveJob.json
        ))
        logger.info(s"Found ${nonIdenticalJobsToRun.size} jobs not matching jobs already running")
        nonIdenticalJobsToRun.par.foreach(processInstance)
      }
    }
  }

  def getInstance(id: String): Option[JobInstance] = jobDao.getById(id)

  def processInstance(instance: JobInstance)(implicit earlyRequestInfo: EarlyRequestInfo): Unit =
    EarlyRequestInfo.wrap(earlyRequestInfo) {
      findJob(instance.jobType) match {
        case Some(job) => processInstance(instance, job)
        case _ => logger.warn("Couldn't find a job matching for this instance: " + instance)
      }
    }

  def processInstance(instance: JobInstance, job: Job): Unit = {
    logger.info(s"Running job ${instance.id}")
    start(instance)
    run(instance, job)
  }

  def kill(instance: JobInstance): Unit = {
    /**
      * TODO no handle on thread to actually kill it if it's running
      * right now.
      */

    // Don't fail if the job is finished
    if (!instance.finished) {
      transactional() {
        instance.succeeded = false
        instance.finished = true
        instance.status = "Killed"
        jobDao.update(instance)
      }
    }
  }

  def unfinishedInstances: Seq[JobInstance] = jobDao.unfinishedInstances

  def listRecent(start: Int, count: Int): Seq[JobInstance] = jobDao.listRecent(start, count)

  def update(instance: JobInstance): Unit = jobDao.update(instance)

  def findJob(identifier: String): Option[Job] =
    jobs.find(identifier == _.identifier)

  def add(user: Option[CurrentUser], prototype: JobPrototype): JobInstance = {
    add(user.map(_.realId), user.map(_.apparentId), prototype)
  }

  def add(user: User, prototype: JobPrototype): JobInstance = {
    add(Some(user.getUserId), Some(user.getUserId), prototype)
  }

  private def add(realUserId: Option[String], apparentUserId: Option[String], prototype: JobPrototype) = {
    if (findJob(prototype.identifier).isEmpty) {
      throw new IllegalArgumentException("No Job found to handle '%s'" format prototype.identifier)
    }

    val instance = JobInstanceImpl.fromPrototype(prototype)
    realUserId.foreach(id => instance.realUser = id)
    apparentUserId.foreach(id => instance.apparentUser = id)

    // Do we already have an outstanding job with these details? Outstanding; just return that.
    jobDao.findOutstandingInstance(instance) getOrElse {
      jobDao.saveJob(instance)

      instance
    }
  }

  def run(instance: JobInstance, job: Job): Unit = {
    try job.run(instance)
    catch {
      case e: Exception if stopping =>
        logger.info(s"Caught exception from job ${instance.id} during restart handling", e)
      case _: KilledJobException =>
        logger.info(s"Job ${instance.id} was killed")
        fail(instance)
      case _: ObsoleteJobException =>
        logger.info(s"Job ${instance.id} obsolete")
        fail(instance)
      case failed: FailedJobException =>
        logger.info(s"Job ${instance.id} failed: ${failed.status}")
        instance.status = failed.status
        fail(instance)
      case e: Throwable =>
        logger.info(s"Job ${instance.id} failed", e)
        instance.status = s"Sorry, there was an error: ${e.getMessage.safeSubstring(0, 1000)}"
        fail(instance)
    }

    if (!stopping) {
      transactional() {
        notify(instance, job) // send any notifications generated by the job
      }

      // Don't call `finish` if the service is stopping.
      // Otherwise, the status set by `cleanUp` is overwritten.
      finish(instance)
    }
  }

  private def start(instance: JobInstance): Unit = {
    transactional() {
      instance.started = true
      instance.schedulerInstance = this.schedulerInstance
      jobDao.update(instance)
    }
  }

  private def finish(instance: JobInstance): Unit = {
    transactional() {
      instance.finished = true
      jobDao.update(instance)
    }
  }

  private def fail(instance: JobInstance): Unit = {
    transactional() {
      instance.succeeded = false
      instance.finished = true
      jobDao.update(instance)
    }
  }

  @PreDestroy
  def cleanUp(): Unit = transactional() {
    stopping = true

    val runningJobs = jobDao.findRunningJobs(this.schedulerInstance)
    runningJobs.foreach(job => {
      logger.warn(s"Job ${job.id} is still running; it will be restarted after the restart")
      job.started = false
      job.progress = 0
      job.status = "Tabula is restarting. This job will begin again once the restart is complete."
      jobDao.update(job)
    })
  }

}
