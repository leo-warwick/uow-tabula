package uk.ac.warwick.tabula.commands.coursework.assignments

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Description, _}
import uk.ac.warwick.tabula.data.model.{Assignment, Module, Submission}
import uk.ac.warwick.tabula.jobs.zips.SubmissionZipFileJob
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.services.jobs.{JobInstance, JobService}
import uk.ac.warwick.tabula.services.{SubmissionService, ZipService}
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}

import scala.collection.JavaConverters._


/**
 * Download one or more submissions from an assignment, as a Zip.
 */
class DownloadSubmissionsCommand(val module: Module, val assignment: Assignment, user: CurrentUser)
	extends Command[Either[RenderableFile, JobInstance]] with ReadOnly {

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Submission.Read, assignment)

	var zipService: ZipService = Wire[ZipService]
	var submissionService: SubmissionService = Wire[SubmissionService]
	var jobService: JobService = Wire[JobService]

	var filename: String = _
	var submissions: JList[Submission] = JArrayList()
	var students: JList[String] = JArrayList()

	override def applyInternal(): Either[RenderableFile, JobInstance] = {
		if (submissions.isEmpty && students.isEmpty) throw new ItemNotFoundException
		else if (!submissions.isEmpty && !students.isEmpty) throw new IllegalStateException("Only expecting one of students and submissions to be set")
		else if (!students.isEmpty && submissions.isEmpty) {
			submissions = (for (
				uniId <- students.asScala;
				submission <- submissionService.getSubmissionByUsercode(assignment, uniId)
			) yield submission).asJava
		}

		if (submissions.asScala.exists(_.assignment != assignment)) {
			throw new IllegalStateException("Submissions don't match the assignment")
		}

		if (submissions.size() < SubmissionZipFileJob.minimumSubmissions) {
			val zip = zipService.getSomeSubmissionsZip(submissions.asScala)
			Left(zip)
		} else {
			Right(jobService.add(Option(user), SubmissionZipFileJob(submissions.asScala.map(_.id))))
		}

	}

	override def describe(d: Description) {

		val downloads: Seq[Submission] = {
			if (students.asScala.nonEmpty) students.asScala.flatMap(submissionService.getSubmissionByUsercode(assignment, _))
			else submissions.asScala
		}

		d.assignment(assignment)
		.submissions(downloads)
		.studentIds(downloads.flatMap(_.universityId))
		.studentUsercodes(downloads.map(_.usercode))
		.properties("submissionCount" -> Option(downloads).map(_.size).getOrElse(0))
	}

}