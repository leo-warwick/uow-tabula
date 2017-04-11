package uk.ac.warwick.tabula.jobs.zips

import uk.ac.warwick.tabula.jobs.Job
import uk.ac.warwick.tabula.services.jobs.JobInstance

object ZipFileJob {
	val ZipFilePathKey = "zipFilePath"
}

abstract class ZipFileJob extends Job {

	def zipFileName: String
	def itemDescription: String

	def updateZipProgress(item: Int, total: Int)(implicit job: JobInstance): Unit = {
		updateStatus(s"Adding file $item of $total")
		updateProgress((item.toFloat / total.toFloat * 100).toInt)
	}

}
