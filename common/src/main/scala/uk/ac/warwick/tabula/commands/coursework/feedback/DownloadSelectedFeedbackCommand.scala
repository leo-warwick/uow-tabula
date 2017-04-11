package uk.ac.warwick.tabula.commands.coursework.feedback

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Description, _}
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.jobs.zips.FeedbackZipFileJob
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.services.jobs.{JobInstance, JobService}
import uk.ac.warwick.tabula.services.{AssessmentService, ZipService}
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}

import scala.collection.JavaConverters._

/**
 * Download one or more submissions from an assignment, as a Zip.
 */
class DownloadSelectedFeedbackCommand(val module: Module, val assignment: Assignment, user: CurrentUser)
	extends Command[Either[RenderableFile, JobInstance]] with ReadOnly {

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.AssignmentFeedback.Read, assignment)

	var assignmentService: AssessmentService = Wire[AssessmentService]
	var zipService: ZipService = Wire[ZipService]
	var feedbackDao: FeedbackDao = Wire[FeedbackDao]
	var jobService: JobService = Wire[JobService]


	var filename: String = _

	var students: JList[String] = JArrayList()

	var feedbacks: JList[AssignmentFeedback] = _

	override def applyInternal(): Either[RenderableFile, JobInstance] = {
		if (students.isEmpty) throw new ItemNotFoundException

		feedbacks = (for (
			usercode <- students.asScala;
			feedback <- feedbackDao.getAssignmentFeedbackByUsercode(assignment, usercode)
		) yield feedback).asJava


		if (feedbacks.asScala.exists(_.assignment != assignment)) {
				throw new IllegalStateException("Selected feedback doesn't match the assignment")
		}

		if (feedbacks.size() < FeedbackZipFileJob.minimumFeedbacks) {
			val zip = zipService.getSomeFeedbacksZip(feedbacks.asScala)
			Left(zip)
		} else {
			Right(jobService.add(Option(user), FeedbackZipFileJob(feedbacks.asScala.map(_.id))))
		}
	}

	override def describe(d: Description): Unit = d
		.assignment(assignment)
		.studentUsercodes(students.asScala)

	override def describeResult(d: Description): Unit = d
		.assignment(assignment)
		.studentIds(feedbacks.asScala.flatMap(_.universityId))
		.studentUsercodes(students.asScala) // is usercodes
		.properties(
			"feedbackCount" -> Option(feedbacks).map(_.size).getOrElse(0))
}
