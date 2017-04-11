package uk.ac.warwick.tabula.jobs

/**
 * An exception that a Job should throw if it's getting ready
 * and finds that it's now obsolete, e.g. the assignment that it's
 * supposed to be working on no longer exists. The job processor
 * should know to catch this and clear up the task.
 */
class ObsoleteJobException extends Exception