package uk.ac.warwick.tabula.services.scheduling.jobs

import org.quartz.{DisallowConcurrentExecution, JobExecutionContext}
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.{Profile, Scope}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.scheduling.turnitin.ProcessTurnitinLtiQueueCommand
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean

/**
	* Treat submissions to Turnitin Lti as a queue. Each job run handles the first of:
	* * Create assignment
	* * Request submission retrieval
	* * Retrieve originality report
	* plus the clean-up task.
	*
	* When a user requests as assignment be sent to Turnitin the submitToTurnitin flag is set (if not already)
	* and their user code is added to the notification list. The lastSubmittedToTurnitin date is set to zero
	* and the retries set to null. If the turnitinId is not null the assignment already exists on Turnitin.
	* As the callback will not be called in this circumstance blank originality reports are created for each
	* submission's valid attachments at this stage.
	*
	* The job looks for any assignment with:
	* * submitToTurnitin = true (only submit assignments that have been requested)
	* * lastSubmittedToTurnitin < now + 20s (wait 20s between retries)
	* * turnitinId = null (has not been created previously)
	* * submitToTurnitinRetries < 50
	* and chooses the one with the oldest lastSubmittedToTurnitin date.
	*
	* If an assignment matches it submits to Turnitin, setting the lastSubmittedToTurnitin date.
	* If the response is not a success the retry count is increased. Success or or not, the job then moves to
	* the clean-up task.
	*
	* Tabula populates the turnitinId via a callback. When this callback is called from Turnitin we:
	* * set the turnitinId to the one provided
	* * create empty originality reports for each submission's attachments that can be submitted
	*
	* If no assignment matches it looks for originality reports with:
	* * turnitinId = null (has not been created previously)
	* * lastSubmittedToTurnitin < now + 2s (wait 2s between retries)
	* * submitToTurnitinRetries < 20
	* and chooses the one with the oldest lastSubmittedToTurnitin date.
	*
	* If a report matches it submits to Turnitin, setting the lastSubmittedToTurnitin date.
	* If the response is a success the turnitinId is set from the response.
	* If the response is not a success the retry count is increased and the error stored.
	* Success or or not, the job then moves to the clean-up task.
	*
	* If no report matches it looks for originality reports with:
	* * turnitinId != null (it has been successully submitted)
	* * fileRequested != null (Turnitin requested the file)
	* * reportReceived = false (the report has already been received)
	* * lastReportRequest < now + 20s (wait 20s between retries)
	* * reportRequestRetries < 50
	* and chooses the one with the oldest lastReportRequest date.
	*
	* If a report matches it requests the report from Turnitin, setting the lastReportRequest date.
	* If the response is a success the report data is set from the response and reportReceived set to true.
	* If the response is not a success the retry count is increased and the error stored.
	* Success or or not, the job then moves to the clean-up task.
	*
	* The clean-up task looks for completed assignments, successful or not.
	*
	* A successful or partially successful assignment is one where:
	* * submitToTurnitin = true (it was requested by someone)
	* * turnitinId != null (the assignment was successfully created)
	* * for each submission's originality reports:
	* ** reportReceived = true (the report has been received) OR
	* ** submitToTurnitinRetries = 20 OR
	* ** reportRequestRetries = 50
	*
	* For any assignments matching, notify each user in the notification list for which submissions it was
	* successful (if not all) and set submitToTurnitin to false and empty the notification list.
	*
	* Finally it looks for failed assignment submissions that can be cleaned up, resulting from reaching maximum
	* retries or waiting too long:
	* * submitToTurnitin = true (it was requested by someone)
	* * turnitinId = null (assignment not created)
	* * submitToTurnitinRetries = 50 (the maximum)
	* Any matching assignment has received 50 errors or time-outs attempting creation. It then notifies any user
	* in the notification list of the failure (they need to contact us).
	*/
@Component
@Profile(Array("scheduling"))
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class ProcessTurnitinLtiQueueJob extends AutowiredJobBean {

	override def executeInternal(context: JobExecutionContext): Unit = {
		if (features.turnitinSubmissions)
			exceptionResolver.reportExceptions {
				ProcessTurnitinLtiQueueCommand().apply()
			}
	}

}

