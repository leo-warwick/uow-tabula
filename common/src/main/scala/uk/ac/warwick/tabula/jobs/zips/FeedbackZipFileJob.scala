package uk.ac.warwick.tabula.jobs.zips

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.jobs.JobPrototype
import uk.ac.warwick.tabula.services.jobs.JobInstance
import uk.ac.warwick.tabula.services.{AutowiringFeedbackServiceComponent, AutowiringZipServiceComponent}
import collection.JavaConverters._
import uk.ac.warwick.tabula.data.Transactions._

object FeedbackZipFileJob {
	val identifier = "feedback-zip-file"
	val zipFileName = "feedback.zip"
	val minimumFeedbacks = 10
	val FeedbacksKey = "feedback"

	def apply(feedbacks: Seq[String]) = JobPrototype(identifier, Map(
		FeedbacksKey -> feedbacks.asJava
	))
}

@Component
class FeedbackZipFileJob extends ZipFileJob with AutowiringZipServiceComponent with AutowiringFeedbackServiceComponent {

	override val identifier: String = FeedbackZipFileJob.identifier
	override val zipFileName: String = FeedbackZipFileJob.zipFileName
	override val itemDescription = "feedback"

	override def run(implicit job: JobInstance): Unit = new Runner(job).run()

	class Runner(job: JobInstance) {
		implicit private val _job: JobInstance = job

		def run(): Unit = {
			 transactional() {
				val feedbacks = job.getStrings(FeedbackZipFileJob.FeedbacksKey).flatMap(feedbackService.getAssignmentFeedbackById)

				updateProgress(0)
				updateStatus("Initialising")

				val zipFile = zipService.getSomeFeedbacksZip(feedbacks, updateZipProgress)
				job.setString(ZipFileJob.ZipFilePathKey, zipFile.filename)

				updateProgress(100)
				job.succeeded = true
			}
		}
	}
}
