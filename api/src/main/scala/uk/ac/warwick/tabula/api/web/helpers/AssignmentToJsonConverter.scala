package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.{DateFormats, TopLevelUrlComponent}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.Routes

trait AssignmentToJsonConverter {
  self: TopLevelUrlComponent with AssessmentMembershipInfoToJsonConverter =>

  def jsonAssignmentObject(assignment: Assignment, includePWD: Boolean = false): Map[String, Any] = {
    val basicInfo = Map(
      "id" -> assignment.id,
      "module" -> Map(
        "code" -> assignment.module.code.toUpperCase,
        "name" -> assignment.module.name,
        "adminDepartment" -> Map(
          "code" -> assignment.module.adminDepartment.code.toUpperCase,
          "name" -> assignment.module.adminDepartment.name
        )
      ),
      "archived" -> !assignment.isAlive, // TODO don't like this inferred value but don't want to change API spec
      "academicYear" -> assignment.academicYear.toString,
      "resitAssessment" -> assignment.resitAssessment,
      "name" -> assignment.name,
      "studentUrl" -> (toplevelUrl + Routes.cm2.assignment(assignment)),
      "summaryUrl" -> (toplevelUrl + Routes.cm2.admin.assignment.submissionsandfeedback.summary(assignment)),
      "collectMarks" -> assignment.collectMarks,
      "useMarkPoints" -> assignment.useMarkPoints,
      "markingWorkflow" -> {
        Option(assignment.cm2MarkingWorkflow).map { mw =>
          Map(
            "id" -> mw.id,
            "name" -> mw.name
          )
        }.orNull
      },
      "feedbackTemplate" -> Option(assignment.feedbackTemplate).map { ft =>
        Map(
          "id" -> ft.id,
          "name" -> ft.name
        )
      }.orNull,
      "summative" -> assignment.summative,
      "dissertation" -> assignment.dissertation,
      "publishFeedback" -> assignment.publishFeedback
    )

    val submissionsInfo =
      if (assignment.collectSubmissions) {
        Map(
          "collectSubmissions" -> true,
          "displayPlagiarismNotice" -> assignment.displayPlagiarismNotice,
          "restrictSubmissions" -> assignment.restrictSubmissions,
          "allowLateSubmissions" -> assignment.allowLateSubmissions,
          "allowResubmission" -> assignment.allowResubmission,
          "fileAttachmentLimit" -> assignment.attachmentLimit,
          "fileAttachmentTypes" -> assignment.attachmentField.map(_.attachmentTypes).orNull,
          "individualFileSizeLimit" -> assignment.attachmentField.map(_.individualFileSizeLimit).orNull,
          "submissionFormText" -> assignment.commentField.map(_.value).getOrElse(""),
          "wordCountMin" -> assignment.wordCountField.map(_.min).orNull,
          "wordCountMax" -> assignment.wordCountField.map(_.max).orNull,
          "wordCountConventions" -> assignment.wordCountField.map(_.conventions).getOrElse(""),
          "submissions" -> assignment.submissions.size(),
        )
      } else {
        Map(
          "collectSubmissions" -> false,
        )
      }

    val extensionInfo =
      Map(
        "allowExtensions" -> assignment.allowExtensions,
        "extensionAttachmentMandatory" -> assignment.extensionAttachmentMandatory,
        "allowExtensionsAfterCloseDate" -> assignment.allowExtensionsAfterCloseDate,
        "unapprovedExtensions" -> assignment.countUnapprovedExtensions,
      )

    val membershipInfo = assignment.membershipInfoWith(includePWD)
    val studentMembershipInfo = jsonAssessmentMembershipInfoObject(membershipInfo, assignment.upstreamAssessmentGroupInfos.groupBy(_.upstreamAssessmentGroup).keys.toSeq)

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

    basicInfo ++ submissionsInfo ++ extensionInfo ++ studentMembershipInfo ++ datesInfo ++ countsInfo
  }
}
