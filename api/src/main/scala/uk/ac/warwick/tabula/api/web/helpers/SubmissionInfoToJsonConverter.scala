package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.services.{ExtensionServiceComponent, SubmissionServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.{DateFormats, TopLevelUrlComponent}

import scala.collection.JavaConverters._
import scala.util.Try

trait SubmissionInfoToJsonConverter {
	self: TopLevelUrlComponent with ExtensionServiceComponent with UserLookupComponent with SubmissionServiceComponent =>

	def jsonSubmissionInfoObject(submission: Submission): Map[String, Any] = {

		val submitter = userLookup.getUserByUserId(submission.usercode)

		val submitterInfo = Map(
			"student" -> Map(
				"totalExtensionRequestsEver" -> extensionService.getAllExtensionRequests(submitter).size,
				"totalSubmissionsEver" -> submissionService.getAllSubmissions(submitter).size
			))

		val assignment = submission.assignment
		val assignmentBasicInfo = Map(
			"assignment" -> Map(
				"id" -> assignment.id,
				"module" -> Map(
					"code" -> assignment.module.code.toUpperCase,
					"name" -> assignment.module.name,
					"adminDepartment" -> Map(
						"code" -> assignment.module.adminDepartment.code.toUpperCase,
						"name" -> assignment.module.adminDepartment.name
					)
				),
				"academicYear" -> assignment.academicYear.toString,
				"name" -> assignment.name,
				"collectMarks" -> assignment.collectMarks,
				"markingWorkflow" -> {
					if (assignment.cm2Assignment)
						Option(assignment.cm2MarkingWorkflow).map { mw => Map(
							"id" -> mw.id,
							"name" -> mw.name
						)}.orNull
					else
						Option(assignment.markingWorkflow).map { mw => Map(
							"id" -> mw.id,
							"name" -> mw.name
						)}.orNull
				},
				"feedbackTemplate" -> Option(assignment.feedbackTemplate).map { ft => Map(
					"id" -> ft.id,
					"name" -> ft.name
				)}.orNull,
				"summative" -> assignment.summative,
				"dissertation" -> assignment.dissertation,
				"publishFeedback" -> assignment.publishFeedback
			)
		)

		val extension = assignment.extensions.asScala.find(e => e.isForUser(submission.usercode))
		val isWithinApprovedExtension = assignment.isWithinExtension(submission.usercode)
		val hasActiveExtension = extension.exists(_.approved)
		val extensionRequested = extension.isDefined && !extension.get.isManual

		val extensionInfo = Map("extension" -> extension.map { e =>
			Map(
				"id" -> e.id,
				"state" -> e.state.description,
				"isWithinApprovedExtension" -> isWithinApprovedExtension,
				"hasActiveExtension" -> hasActiveExtension,
				"extensionRequested" -> extensionRequested,
				"extensionRequestedOn" -> DateFormats.IsoDateTime.print(e.requestedOn),
				"requestedExpiryDate" -> e.requestedExpiryDate.map(DateFormats.IsoDateTime.print).orNull,
				"expiryDate" -> e.expiryDate.map(DateFormats.IsoDateTime.print).orNull,
				"attachments" -> e.attachments.asScala.map { attachment => Map(
					"url" -> (toplevelUrl + Routes.cm2.admin.assignment.extensionAttachment(e, attachment.name))
				)},
				"reason" -> e.reason,
				"reviewerComments" -> e.reviewerComments,
				"extensionRequestedExtraDaysDuration" -> e.requestedExtraDuration,
				"feedbackDueDate" -> e.feedbackDueDate.map(DateFormats.IsoDateTime.print).orNull
			)
		}.orNull)

		val submissionInfo = Map(
			"id" -> submission.id,
			"late" -> submission.isLate,
			"authorisedLate" -> submission.isAuthorisedLate,
			"attachments" -> submission.allAttachments.map { attachment => Map(
				"filename" -> attachment.name,
				"id" -> attachment.id,
				"attachmentUrl" -> (toplevelUrl + Routes.cm2.admin.assignment.submissionAttachmentDownload(submission, attachment.name)),
				"originalityReport" -> Option(attachment.originalityReport).map { report => Map(
					"similarity" -> JInteger(report.similarity),
					"overlap" -> JInteger(report.overlap),
					"webOverlap" -> JInteger(report.webOverlap),
					"studentOverlap" -> JInteger(report.studentOverlap),
					"publicationOverlap" -> JInteger(report.publicationOverlap),
					"reportUrl" -> (toplevelUrl + Routes.cm2.admin.assignment.turnitin.report(submission.assignment, attachment.originalityReport))
				)}.orNull
			)},
			"submittedDate" -> Option(submission.submittedDate).map(DateFormats.IsoDateTime.print).orNull,
			"wordCount" -> submission.assignment.wordCountField.flatMap(submission.getValue).map { formValue => JInteger(Try(formValue.value.toInt).toOption) }.orNull,
			"suspectPlagiarised" -> submission.suspectPlagiarised
		)

		submitterInfo ++ assignmentBasicInfo ++ extensionInfo ++ submissionInfo
	}

}
