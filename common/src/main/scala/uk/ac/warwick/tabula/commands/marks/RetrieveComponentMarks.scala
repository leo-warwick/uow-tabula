package uk.ac.warwick.tabula.commands.marks

import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand.StudentMarkRecord
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, AssessmentType, ModuleRegistration, UpstreamAssessmentGroupMember}


/**
 * For the given module registration lists which component marks should be considered when calculating the module mark for the most recent attempt
 */
trait RetrieveComponentMarks {

  val studentComponentMarkRecords: Seq[(AssessmentComponent, Seq[StudentMarkRecord])]

  def componentMarks(moduleRegistration: ModuleRegistration): Map[AssessmentComponent, (StudentMarkRecord, Option[BigDecimal])] = {
    def extractMarks(components: Seq[UpstreamAssessmentGroupMember]): Seq[(AssessmentType, String, Option[Int])] = components.flatMap { uagm =>
      uagm.upstreamAssessmentGroup.assessmentComponent.map { ac =>
        val mark: Option[Int] =
          studentComponentMarkRecords.find(_._1 == ac)
            .flatMap(_._2.find(_.upstreamAssessmentGroupMember == uagm).flatMap(_.mark))
            .orElse(uagm.firstDefinedMark)

        (ac.assessmentType, ac.sequence, mark)
      }
    }

    moduleRegistration.upstreamAssessmentGroupMembersAllAttempts(extractMarks)
      .last // Use the weightings and components from the most recent attempt
      .flatMap { case (uagm, weighting) =>
        studentComponentMarkRecords.flatMap { case (ac, allStudents) =>
          // If a student has had multiple attempts at the same assessment, use the attempt with the highest mark
          val mostRecentAttempt = allStudents.find(_.upstreamAssessmentGroupMember == uagm)

          // If the most recent attempt has a mark, allow using a previous attempt if it has a higher mark
          // (Don't need to worry about pass/fail modules here)
          val attempt =
          if (mostRecentAttempt.exists(_.mark.nonEmpty)) {
            // Same student and assessment
            val attemptWithHighestMark = allStudents.filter(s => s.upstreamAssessmentGroupMember.upstreamAssessmentGroup == uagm.upstreamAssessmentGroup && s.universityId == uagm.universityId)
              .maxByOption(_.mark)

            // make sure to return the most recent attempt if the highest marks are tied
            if (mostRecentAttempt.flatMap(_.mark) == attemptWithHighestMark.flatMap(_.mark)) {
              mostRecentAttempt
            } else {
              attemptWithHighestMark
            }
          } else {
            mostRecentAttempt
          }

          attempt.map(ac -> (_, weighting))
        }
      }
      .toMap
  }
}
