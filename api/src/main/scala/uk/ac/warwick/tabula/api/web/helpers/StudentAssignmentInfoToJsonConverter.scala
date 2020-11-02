package uk.ac.warwick.tabula.api.web.helpers

import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.coursework.assignments.CourseworkCommandTypes
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback, Submission}
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.{DateFormats, TopLevelUrlComponent}

import scala.jdk.CollectionConverters._
import scala.util.Try

object StudentAssignmentInfoHelper {
  /**
   * SAF object / StudentAssignmentFeedback object
   *
   * id	A unique identifier for the feedback
   * mark	The given mark for the feedback, or null
   * grade	The given grade for the feedback, or null
   * adjustments	An array of adjustments applied to the submission, including reason and comments
   * genericFeedback	Feedback given to anybody who submitted to the assignment
   * comments	Feedback given specifically to this student
   * attachments	An array of JSON objects with two properties, id with a unique identifier and filename
   * downloadZip	The URL that a student would go to to download all feedback attachments
   * downloadPdf	The URL that a student would go to to download a PDF version of the feedback
   */
  def makeFeedbackInfo(
    feedback: Feedback,
    toplevelUrl: String,
  ): Map[String, Any] = {
    val assignment: Assignment = feedback.assignment
    Map(
      "id" -> feedback.id,
      "mark" -> JInteger(feedback.latestMark),
      "grade" -> feedback.latestGrade.orNull,
      "adjustments" -> feedback.studentViewableAdjustments.map { mark =>
        Map(
          "reason" -> mark.reason,
          "comments" -> mark.comments
        )
      },
      "genericFeedback" -> assignment.genericFeedback,
      "comments" -> feedback.comments.orNull,
      "attachments" -> feedback.attachments.asScala.map { attachment =>
        Map(
          "filename" -> attachment.name,
          "id" -> attachment.id
        )
      },
      "downloadZip" -> (toplevelUrl + Routes.cm2.assignment.feedback(assignment)),
      "downloadPdf" -> (toplevelUrl + Routes.cm2.assignment.feedbackPdf(assignment, feedback))
    )
  }
}

trait StudentAssignmentInfoToJsonConverter extends CourseworkCommandTypes {
  self: TopLevelUrlComponent =>

  def jsonAssignmentInfoObject(info: AssignmentInfo): Map[String, Any] = {
    val assignment: Assignment = info("assignment").asInstanceOf[Assignment]
    val submission: Option[Submission] = info("submission").asInstanceOf[Option[Submission]]
    val feedback: Option[Feedback] = info("feedback").asInstanceOf[Option[Feedback]]
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
      "studentUrl" -> (toplevelUrl + Routes.cm2.assignment(assignment))
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
          "fileAttachmentTypes" -> assignment.attachmentField.map(_.attachmentTypes).orNull,
          "individualFileSizeLimit" -> assignment.attachmentField.map(_.individualFileSizeLimit).orNull,
          "submissionFormText" -> assignment.commentField.map(_.value).getOrElse(""),
          "wordCountMin" -> assignment.wordCountField.map(_.min).orNull,
          "wordCountMax" -> assignment.wordCountField.map(_.max).orNull,
          "wordCountConventions" -> assignment.wordCountField.map(_.conventions).getOrElse("")
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
        "attachments" -> s.allAttachments.map { attachment =>
          Map(
            "filename" -> attachment.name,
            "id" -> attachment.id
          )
        },
        "submittedDate" -> Option(s.submittedDate).map(DateFormats.IsoDateTime.print).orNull,
        "wordCount" -> assignment.wordCountField.flatMap(s.getValue).map { formValue => JInteger(Try(formValue.value.toInt).toOption) }.orNull
      )
    }.orNull)

    val feedbackInfo = Map("feedback" -> feedback.map(f => StudentAssignmentInfoHelper.makeFeedbackInfo(f, toplevelUrl)).orNull)

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
