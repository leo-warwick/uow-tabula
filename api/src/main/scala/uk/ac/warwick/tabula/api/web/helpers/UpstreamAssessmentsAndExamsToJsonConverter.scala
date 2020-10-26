package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{DateFormats, TopLevelUrlComponent}

import scala.jdk.CollectionConverters._

trait UpstreamAssessmentsAndExamsToJsonConverter {
  self: TopLevelUrlComponent =>

  def jsonExamPaperObject(ac: AssessmentComponent, schedule: Seq[AssessmentComponentExamSchedule]): (String, Any) =
    "examPaper" -> (ac.examPaperCode match {
      case Some(examPaperCode) => Map(
        "code" -> examPaperCode,
        "duration" -> ac.examPaperDuration.map(_.toString).orNull,
        "title" -> ac.examPaperTitle,
        "readingTime" -> ac.examPaperReadingTime.map(_.toString).orNull,
        "section" -> ac.examPaperSection,
        "type" -> ac.examPaperType,
        "schedule" -> schedule.map(jsonExamScheduleObject)
      )
      case _ => null
    })

  def jsonExamScheduleObject(schedule: AssessmentComponentExamSchedule): Map[String, Any] =
    Map(
      "examProfileCode" -> schedule.examProfileCode,
      "academicYear" -> schedule.academicYear.toString,
      "slotId" -> schedule.slotId,
      "sequence" -> schedule.sequence,
      "locationSequence" -> schedule.locationSequence,
      "startTime" -> DateFormats.IsoDateTime.print(schedule.startTime),
      "location" -> schedule.location.map {
        case NamedLocation(name) => Map("name" -> name)
        case MapLocation(name, locationId, syllabusPlusName) =>
          Map("name" -> name, "locationId" -> locationId) ++ syllabusPlusName.map(n => Map("syllabusPlusName" -> n)).getOrElse(Map())
        case AliasedMapLocation(name, MapLocation(_, locationId, syllabusPlusName)) =>
          Map("name" -> name, "locationId" -> locationId) ++ syllabusPlusName.map(n => Map("syllabusPlusName" -> n)).getOrElse(Map())
      }.orNull,
      "students" -> schedule.students.asScala.map { student =>
        Map(
          "seatNumber" -> JInteger(student.seatNumber),
          "universityId" -> student.universityId,
          "sprCode" -> student.sprCode,
          "occurrence" -> student.occurrence,
          "specialExamArrangements" -> student.studentCourseDetails.flatMap(scd => Option(scd.specialExamArrangements)).orNull,
          "specialExamArrangementsLocation" -> student.studentCourseDetails.flatMap(scd => Option(scd.specialExamArrangementsLocation)).map {
            case NamedLocation(name) => Map("name" -> name)
            case MapLocation(name, locationId, syllabusPlusName) =>
              Map("name" -> name, "locationId" -> locationId) ++ syllabusPlusName.map(n => Map("syllabusPlusName" -> n)).getOrElse(Map())
            case AliasedMapLocation(name, MapLocation(_, locationId, syllabusPlusName)) =>
              Map("name" -> name, "locationId" -> locationId) ++ syllabusPlusName.map(n => Map("syllabusPlusName" -> n)).getOrElse(Map())
          }.orNull,
          "specialExamArrangementsExtraTime" -> student.studentCourseDetails.flatMap(scd => Option(scd.specialExamArrangementsExtraTime)).map(_.toString).orNull,
          "specialExamArrangementsHourlyRestMinutes" -> student.studentCourseDetails.flatMap(scd => Option(scd.specialExamArrangementsHourlyRestMinutes)).map(_.toString).orNull,
        )
      }
    )

  def jsonUpstreamAssessmentGroupKeyObject(key: UpstreamAssessmentGroupKey): Map[String, Any] = Map(
    "moduleCode" -> key.moduleCode,
    "academicYear" -> key.academicYear,
    "sequence" -> key.sequence,
    "occurrence" -> key.occurrence
  )

  def jsonUpstreamAssessmentObject(assessmentComponent: AssessmentComponent): Map[String, Any] =
    Map(
      "id" -> assessmentComponent.id,
      "name" -> assessmentComponent.name,
      "moduleCode" -> assessmentComponent.moduleCode,
      "cats" -> assessmentComponent.cats,
      "sequence" -> assessmentComponent.sequence,
      "type" -> assessmentComponent.assessmentType,
      "assessmentGroup" -> assessmentComponent.assessmentGroup,
      "marksCode" -> assessmentComponent.marksCode,
      "weighting" -> assessmentComponent.rawWeighting,
      "module" -> Map(
        "code" -> assessmentComponent.module.code.toUpperCase,
        "name" -> assessmentComponent.module.name,
        "adminDepartment" -> Map(
          "code" -> assessmentComponent.module.adminDepartment.code.toUpperCase,
          "name" -> assessmentComponent.module.adminDepartment.name
        )
      ),
      jsonExamPaperObject(assessmentComponent, assessmentComponent.scheduledExams(None))
    )

  def jsonUpstreamAssessmentGroupInfoObject(info: UpstreamAssessmentGroupInfo): Map[String, Any] =
    Map(
      "moduleCode" -> info.upstreamAssessmentGroup.moduleCode,
      "assessmentGroup" -> info.upstreamAssessmentGroup.assessmentGroup,
      "occurrence" -> info.upstreamAssessmentGroup.occurrence,
      "sequence" -> info.upstreamAssessmentGroup.sequence,
      "academicYear" -> info.upstreamAssessmentGroup.academicYear.toString,
      "currentMembers" -> info.currentMembers.map(_.universityId).sorted
    )
}
