package uk.ac.warwick.tabula.jobs

/**
 * This exception can be thrown within a job to cause it to be marked as failed.
 * You don't have to throw this when a job fails - you can manually set it as failed
 * and set your own status message. This is mainly useful for aborting a job somewhere
 * deep in the middle of the stack.
 *
 * JobService will catch this exception and update the job status with the given status
 * and mark it as failed.
 */
class FailedJobException(val status:String) extends Exception(status)