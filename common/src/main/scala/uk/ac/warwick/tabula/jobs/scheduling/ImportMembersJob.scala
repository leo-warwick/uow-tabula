package uk.ac.warwick.tabula.jobs.scheduling

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.scheduling.imports.ImportProfilesCommand
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.jobs.{Job, JobPrototype}
import uk.ac.warwick.tabula.services.jobs.JobInstance

import scala.collection.JavaConverters._

object ImportMembersJob {
	val identifier = "import-members"
	val MembersKey = "members"

	def status(universityId: String) = s"Importing $universityId"

	def apply(universityIds: Seq[String]) = JobPrototype(identifier, Map(
		MembersKey -> universityIds.asJava
	))
}

@Component
class ImportMembersJob extends Job {

	val identifier: String = ImportMembersJob.identifier

	override def run(implicit job: JobInstance): Unit = new Runner(job).run()

	class Runner(job: JobInstance) {
		implicit private val _job: JobInstance = job

		def run(): Unit = {
			transactional() {
				val memberIds = job.getStrings(ImportMembersJob.MembersKey)

				updateProgress(0)

				memberIds.zipWithIndex.foreach{case (universityId, index) =>
					updateStatus(ImportMembersJob.status(universityId))

					val command = new ImportProfilesCommand
					command.refresh(universityId, None)

					updateProgress(index + 1, memberIds.size)
				}

				updateStatus("Import complete")
				updateProgress(100)

				job.succeeded = true
			}
		}
	}
}
