package uk.ac.warwick.tabula.api.web.helpers

import org.joda.time.DateTime
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.api.web.controllers.TopLevelUrlAware
import uk.ac.warwick.tabula.commands.coursework.assignments.CourseworkCommandTypes
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.{AssignmentFeedback, Submission, Assignment}
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConverters._

import scala.util.Try

trait StudentAssignmentInfoToJsonConverter extends CourseworkCommandTypes {
	self: TopLevelUrlAware =>

	def jsonAssignmentInfoObject(info: AssignmentInfo): Map[String, Any] = {
		val assignment: Assignment = info("assignment").asInstanceOf[Assignment]
		val submission: Option[Submission] = info("submission").asInstanceOf[Option[Submission]]
		val feedback: Option[AssignmentFeedback] = info("feedback").asInstanceOf[Option[AssignmentFeedback]]
		val extension: Option[Extension] = info("extension").asInstanceOf[Option[Extension]]

		val basicInfo = Map(
			"hasSubmission" -> info("hasSubmission"),
			"hasFeedback" -> info("hasFeedback"),

			"hasExtension" -> info("hasExtension"),
			"hasActiveExtension" -> info("hasActiveExtension"),

			"extended" -> info("isExtended"),
			"extensionRequested" -> info("extensionRequested"),
			"studentDeadline" -> DateFormats.IsoDateTime.print(info("studentDeadline").asInstanceOf[DateTime]),
			"submittable" -> info("submittable"),
			"resubmittable" -> info("resubmittable"),
			"closed" -> info("closed"),
			"summative" -> info("summative"),

			"late" -> (!info("isExtended").asInstanceOf[Boolean] && info("closed").asInstanceOf[Boolean]),

			"module" -> Map(
				"code" -> assignment.module.code.toUpperCase,
				"name" -> assignment.module.name,
				"adminDepartment" -> Map(
					"code" -> assignment.module.adminDepartment.code.toUpperCase,
					"name" -> assignment.module.adminDepartment.name
				)
			),

			"id" -> assignment.id,
			"academicYear" -> assignment.academicYear.toString,
			"name" -> assignment.name,
			"studentUrl" -> (toplevelUrl + Routes.coursework.assignment(assignment))
		)

		val assignmentSubmissionInfo =
			if (assignment.collectSubmissions) {
				Map(
					"allowsLateSubmissions" -> assignment.allowLateSubmissions,
					"allowsResubmission" -> assignment.allowResubmission,
					"allowsExtensions" -> assignment.allowExtensions,
					"extensionAttachmentMandatory" -> assignment.extensionAttachmentMandatory,
					"allowExtensionsAfterCloseDate" -> assignment.allowExtensionsAfterCloseDate,
					"fileAttachmentLimit" -> assignment.attachmentLimit,
					"fileAttachmentTypes" -> assignment.fileExtensions,
					"individualFileSizeLimit" -> assignment.attachmentField.map { _.individualFileSizeLimit }.orNull,
					"submissionFormText" -> assignment.commentField.map { _.value }.getOrElse(""),
					"wordCountMin" -> assignment.wordCountField.map { _.min }.orNull,
					"wordCountMax" -> assignment.wordCountField.map { _.max }.orNull,
					"wordCountConventions" -> assignment.wordCountField.map { _.conventions }.getOrElse("")
				)
			} else {
				Map()
			}

		val datesInfo =
			if (assignment.openEnded) {
				Map(
					"openEnded" -> true,
					"opened" -> assignment.isOpened,
					"openDate" -> DateFormats.IsoDateTime.print(assignment.openDate)
				)
			} else {
				Map(
					"openEnded" -> false,
					"opened" -> assignment.isOpened,
					"openDate" -> DateFormats.IsoDateTime.print(assignment.openDate),
					"closeDate" -> DateFormats.IsoDateTime.print(assignment.closeDate)
				)
			}

		val submissionInfo = Map("submission" -> submission.map { s =>
			Map(
				"id" -> s.id,
				"late" -> s.isLate,
				"authorisedLate" -> s.isAuthorisedLate,
				"attachments" -> s.allAttachments.map { attachment => Map(
					"filename" -> attachment.name,
					"id" -> attachment.id
				)},
				"submittedDate" -> Option(s.submittedDate).map(DateFormats.IsoDateTime.print).orNull,
				"wordCount" -> assignment.wordCountField.flatMap(s.getValue).map { formValue => JInteger(Try(formValue.value.toInt).toOption) }.orNull
			)
		}.orNull)

		val feedbackInfo = Map("feedback" -> feedback.map { f =>
			Map(
				"id" -> f.id,
				"mark" -> JInteger(f.latestMark),
				"grade" -> f.latestGrade.orNull,
				"adjustments" -> f.studentViewableAdjustments.map { mark =>
					Map(
						"reason" -> mark.reason,
						"comments" -> mark.comments
					)
				},
				"genericFeedback" -> assignment.genericFeedback,
				"comments" -> f.comments.orNull,
				"attachments" -> f.attachments.asScala.map { attachment => Map(
					"filename" -> attachment.name,
					"id" -> attachment.id
				)},
				"downloadZip" -> (toplevelUrl + Routes.coursework.assignment.feedback(assignment)),
				"downloadPdf" -> (toplevelUrl + Routes.coursework.assignment.feedbackPdf(assignment, f))
			)
		}.orNull)

		val extensionInfo = Map("extension" -> extension.map { e =>
			Map(
				"id" -> e.id,
				"state" -> e.state.description,
				"requestedExpiryDate" -> e.requestedExpiryDate.map(DateFormats.IsoDateTime.print).orNull,
				"expiryDate" -> e.expiryDate.map(DateFormats.IsoDateTime.print).orNull
			)
		}.orNull)

		basicInfo ++ assignmentSubmissionInfo ++ datesInfo ++ submissionInfo ++ feedbackInfo ++ extensionInfo
	}

}
