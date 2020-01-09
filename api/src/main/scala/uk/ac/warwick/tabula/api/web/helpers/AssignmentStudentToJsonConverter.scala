package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.data.model.forms.ExtensionState
import uk.ac.warwick.tabula.helpers.cm2.AssignmentSubmissionStudentInfo
import uk.ac.warwick.tabula.{DateFormats, WorkflowStageHealth}

import scala.jdk.CollectionConverters._

trait AssignmentStudentToJsonConverter extends SubmissionToJsonConverter {
  self: AssignmentStudentMessageResolver with ApiController =>

  def jsonAssignmentStudentObject(student: AssignmentSubmissionStudentInfo): Map[String, Any] = {
    val userDetails = Map("universityId" -> student.user.getWarwickId)

    // feedback ID -> assignment feedback
    val assignmentFeedbacks: Map[String, Feedback] = student.assignment.feedbacks.asScala.map { assignmentFeedback =>
      assignmentFeedback.id -> assignmentFeedback
    }.toMap


    val workflowDetails = Map(
      "state" -> Map(
        "progress" -> Map(
          "percentage" -> student.progress.percentage,
          "health" -> WorkflowStageHealth.fromCssClass(student.progress.t).toString,
          "stateCode" -> student.progress.messageCode,
          "state" -> getMessageForStudent(student, student.progress.messageCode)
        ),
        "nextStage" -> student.nextStage.map { stage =>
          Map(
            "name" -> stage.toString,
            "action" -> getMessageForStudent(student, stage.actionCode)
          )
        }.orNull,
        "stages" -> student.stages.values.map { progress =>
          Map(
            "stage" -> progress.stage.toString,
            "action" -> getMessageForStudent(student, progress.stage.actionCode),
            "stateCode" -> progress.messageCode,
            "state" -> getMessageForStudent(student, progress.messageCode),
            "health" -> progress.health.toString,
            "completed" -> progress.completed,
            "preconditionsMet" -> progress.preconditionsMet
          )
        }
      )
    )

    val submissionDetails = Map("submission" -> jsonSubmissionObject(student))

    val extensionDetails = Map(
      "extension" -> student.coursework.enhancedExtension.map { enhancedExtension =>
        val extension = enhancedExtension.extension

        Map(
          "id" -> extension.id,
          "state" -> extension.state.description,
          "expired" -> (extension.state == ExtensionState.Approved && !enhancedExtension.within),
          "expiryDate" -> extension.expiryDate.map(DateFormats.IsoDateTime.print).orNull,
          "requestedExpiryDate" -> extension.requestedExpiryDate.map(DateFormats.IsoDateTime.print).orNull
        )
      }.orNull
    )

    val feedbackDetails = Map(
      "feedback" -> student.coursework.enhancedFeedback.map { enhancedFeedback =>
        val feedback = enhancedFeedback.feedback
        Map(
          "id" -> feedback.id,
          "downloaded" -> enhancedFeedback.downloaded,
          "onlineViewed" -> enhancedFeedback.onlineViewed
        ) ++ assignmentFeedbacks.get(feedback.id).map(StudentAssignmentInfoHelper.makeFeedbackInfo(_, toplevelUrl)).getOrElse(Nil)
      }.orNull
    )

    val courseworkDetails = submissionDetails ++ extensionDetails ++ feedbackDetails

    userDetails ++ workflowDetails ++ courseworkDetails
  }
}
