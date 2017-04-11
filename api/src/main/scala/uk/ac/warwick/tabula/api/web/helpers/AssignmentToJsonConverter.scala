package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.api.web.controllers.TopLevelUrlAware
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.Routes

trait AssignmentToJsonConverter {
	self: TopLevelUrlAware with AssessmentMembershipInfoToJsonConverter =>

	def jsonAssignmentObject(assignment: Assignment): Map[String, Any] = {
		val basicInfo = Map(
			"id" -> assignment.id,
			"archived" -> !assignment.isAlive, // TODO don't like this inferred value but don't want to change API spec
			"academicYear" -> assignment.academicYear.toString,
			"name" -> assignment.name,
			"studentUrl" -> (toplevelUrl + Routes.coursework.assignment(assignment)),
			"collectMarks" -> assignment.collectMarks,
			"markingWorkflow" -> Option(assignment.markingWorkflow).map { mw => Map(
				"id" -> mw.id,
				"name" -> mw.name
			)}.orNull,
			"feedbackTemplate" -> Option(assignment.feedbackTemplate).map { ft => Map(
				"id" -> ft.id,
				"name" -> ft.name
			)}.orNull,
			"summative" -> assignment.summative,
			"dissertation" -> assignment.dissertation
		)

		val submissionsInfo =
			if (assignment.collectSubmissions) {
				Map(
					"collectSubmissions" -> true,
					"displayPlagiarismNotice" -> assignment.displayPlagiarismNotice,
					"restrictSubmissions" -> assignment.restrictSubmissions,
					"allowLateSubmissions" -> assignment.allowLateSubmissions,
					"allowResubmission" -> assignment.allowResubmission,
					"allowExtensions" -> assignment.allowExtensions,
					"extensionAttachmentMandatory" -> assignment.extensionAttachmentMandatory,
					"allowExtensionsAfterCloseDate" -> assignment.allowExtensionsAfterCloseDate,
					"fileAttachmentLimit" -> assignment.attachmentLimit,
					"fileAttachmentTypes" -> assignment.fileExtensions,
					"individualFileSizeLimit" -> assignment.attachmentField.map { _.individualFileSizeLimit }.orNull,
					"submissionFormText" -> assignment.commentField.map { _.value }.getOrElse(""),
					"wordCountMin" -> assignment.wordCountField.map { _.min }.orNull,
					"wordCountMax" -> assignment.wordCountField.map { _.max }.orNull,
					"wordCountConventions" -> assignment.wordCountField.map { _.conventions }.getOrElse(""),
					"submissions" -> assignment.submissions.size(),
					"unapprovedExtensions" -> assignment.countUnapprovedExtensions
				)
			} else {
				Map(
					"collectSubmissions" -> false
				)
			}

		val membershipInfo = assignment.membershipInfo
		val studentMembershipInfo = jsonAssessmentMembershipInfoObject(membershipInfo, assignment.upstreamAssessmentGroups)

		val datesInfo =
			if (assignment.openEnded) {
				Map(
					"openEnded" -> true,
					"opened" -> assignment.isOpened,
					"closed" -> false,
					"openDate" -> DateFormats.IsoDateTime.print(assignment.openDate)
				)
			} else {
				Map(
					"openEnded" -> false,
					"opened" -> assignment.isOpened,
					"closed" -> assignment.isClosed,
					"openDate" -> DateFormats.IsoDateTime.print(assignment.openDate),
					"closeDate" -> DateFormats.IsoDateTime.print(assignment.closeDate),
					"feedbackDeadline" -> assignment.feedbackDeadline.map(DateFormats.IsoDate.print).orNull
				)
			}

		val countsInfo = Map(
			"feedback" -> assignment.countFullFeedback,
			"unpublishedFeedback" -> assignment.countUnreleasedFeedback
		)

		basicInfo ++ submissionsInfo ++ studentMembershipInfo ++ datesInfo ++ countsInfo
	}
}
