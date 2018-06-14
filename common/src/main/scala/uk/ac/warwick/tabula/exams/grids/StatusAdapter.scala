package uk.ac.warwick.tabula.exams.grids

import uk.ac.warwick.tabula.jobs.Job
import uk.ac.warwick.tabula.services.jobs.JobInstance

trait StatusAdapter {
	var stageNumber = 1
	var stageCount = 1

	def setMessage(message: String): Unit

	def setGlobalProgress(percentage: Int): Unit

	def setProgress(percentage: Int): Unit = {
		val completedStages = (((stageNumber - 1).toFloat / stageCount.toFloat) * 100).toInt
		val currentStage = (((percentage.toFloat / 100.0) / stageCount.toFloat) * 100).toInt

		setGlobalProgress(completedStages + currentStage)
	}

	def setProgress(current: Int, max: Int): Unit = setProgress(((current.toFloat / max.toFloat) * 100).toInt)
}

object NullStatusAdapter extends StatusAdapter {
	override def setMessage(message: String): Unit = ()

	override def setGlobalProgress(percentage: Int): Unit = ()
}

class JobInstanceStatusAdapter(job: Job, instance: JobInstance) extends StatusAdapter {
	override def setMessage(message: String): Unit = job.updateStatus(message)(instance)

	override def setGlobalProgress(percentage: Int): Unit = job.updateProgress(percentage)(instance)
}
