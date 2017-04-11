package uk.ac.warwick.tabula.jobs

/**
 * Initial information generated to give to the JobService to
 * create a JobInstance that then gets saved. Usually created
 * by a factory method in a particular Job.
 */
case class JobPrototype(identifier: String, map: Map[String, Any])