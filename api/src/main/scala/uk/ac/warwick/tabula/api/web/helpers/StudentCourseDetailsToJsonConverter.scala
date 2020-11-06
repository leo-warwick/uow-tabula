package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.api.web.helpers.MemberApiFreemarkerHelper._
import uk.ac.warwick.tabula.data.model.{ModuleRegistration, StudentCourseDetails, StudentRelationship}
import uk.ac.warwick.tabula.web.views.ScalaFreemarkerConfigurationComponent

import scala.jdk.CollectionConverters._

/**
  * We use Freemarker to ensure that we use the same logic as in ScalaBeansWrapper, i.e. that we don't expose
  * any properties that the user doesn't have permissions to see.
  */
trait StudentCourseDetailsToJsonConverter
  extends StudentCourseYearDetailsToJsonConverter
    with MemberApiFreemarkerHelper {
  self: ScalaFreemarkerConfigurationComponent with MemberToJsonConverter =>

  private def studentRelationshipToJson(rel: StudentRelationship, fieldRestriction: APIFieldRestriction): Map[String, Any] = Seq(
    date("startDate", rel, "startDate", fieldRestriction),
    date("endDate", rel, "endDate", fieldRestriction),
    double("percentage", rel, "percentage", fieldRestriction),
    fieldRestriction.nested("agent").flatMap { restriction =>
      rel.agentMember.map { agent => "agent" -> jsonMemberObject(agent, restriction, deep = false) }
    }
  ).flatten.toMap

  private def moduleRegistrationToJson(reg: ModuleRegistration, fieldRestriction: APIFieldRestriction): Map[String, Any] = Seq(
    fieldRestriction.nested("module").flatMap { restriction =>
      if (canViewProperty(reg, "module"))
        Some("module", moduleToJson(reg.module, restriction))
      else None
    },
    double("cats", reg, "cats", fieldRestriction),
    str("academicYear", reg, "academicYear.toString", fieldRestriction),
    str("assessmentGroup", reg, "assessmentGroup", fieldRestriction),
    str("occurrence", reg, "occurrence", fieldRestriction),
    double("mark", reg, "agreedMark", fieldRestriction),
    str("grade", reg, "agreedGrade", fieldRestriction),
    str("status", reg, "selectionStatus.description", fieldRestriction),
    str("sitsModuleCode", reg, "sitsModuleCode", fieldRestriction)
  ).flatten.toMap

  def jsonStudentCourseDetailsObject(scd: StudentCourseDetails, fieldRestriction: APIFieldRestriction): Map[String, Any] = Seq(
    str("scjCode", scd, "scjCode", fieldRestriction),
    str("sprCode", scd, "sprCode", fieldRestriction),
    str("levelCode", scd, "levelCode", fieldRestriction),
    date("beginDate", scd, "beginDate", fieldRestriction),
    date("endDate", scd, "endDate", fieldRestriction),
    date("expectedEndDate", scd, "expectedEndDate", fieldRestriction),
    int("courseYearLength", scd, "courseYearLength", fieldRestriction),
    boolean("mostSignificant", scd, "mostSignificant", fieldRestriction),
    str("reasonForTransferCode", scd, "reasonForTransferCode", fieldRestriction),
    boolean("specialExamArrangements", scd, "specialExamArrangements", fieldRestriction),
    fieldRestriction.nested("specialExamArrangementsLocation").flatMap { restriction =>
      Option(scd.specialExamArrangementsLocation)
        .filter(_ => canViewProperty(scd, "specialExamArrangementsLocation"))
        .map(l => "specialExamArrangementsLocation" -> locationToJson(l, restriction))
    },
    fieldRestriction.restrict("specialExamArrangementsExtraTime") {
      Option(scd.specialExamArrangementsExtraTime)
        .filter(_ => canViewProperty(scd, "specialExamArrangementsExtraTime"))
        .map(d => "specialExamArrangementsExtraTime" -> d.toString)
    },
    fieldRestriction.restrict("specialExamArrangementsHourlyRestMinutes") {
      Option(scd.specialExamArrangementsHourlyRestMinutes)
        .filter(_ => canViewProperty(scd, "specialExamArrangementsHourlyRestMinutes"))
        .map(d => "specialExamArrangementsHourlyRestMinutes" -> d.toString)
    },
    fieldRestriction.nested("course").flatMap { restriction =>
      if (canViewProperty(scd, "course"))
        Some("course", courseToJson(scd.course, restriction))
      else None
    },
    fieldRestriction.nested("currentRoute").flatMap { restriction =>
      if (canViewProperty(scd, "currentRoute"))
        Some("currentRoute", routeToJson(scd.currentRoute, restriction))
      else None
    },
    fieldRestriction.nested("department").flatMap { restriction =>
      if (canViewProperty(scd, "department"))
        Some("department", departmentToJson(scd.department, restriction))
      else None
    },
    fieldRestriction.nested("award").flatMap { restriction =>
      if (canViewProperty(scd, "award"))
        Some("award", awardToJson(scd.award, restriction))
      else None
    },
    fieldRestriction.nested("statusOnRoute").flatMap { restriction =>
      if (canViewProperty(scd, "statusOnRoute"))
        Some("statusOnRoute", sitsStatusToJson(scd.statusOnRoute, restriction))
      else None
    },
    fieldRestriction.nested("statusOnCourse").flatMap { restriction =>
      if (canViewProperty(scd, "statusOnCourse"))
        Some("statusOnCourse", sitsStatusToJson(scd.statusOnCourse, restriction))
      else None
    },
    fieldRestriction.nested("relationships").flatMap { restriction =>
      if (canViewProperty(scd, "relationships"))
        Some("relationships" -> scd.allRelationships.asScala.filter(_.isCurrent).groupBy(_.relationshipType).map { case (relType, relationships) =>
          relType.urlPart -> relationships.toSeq.map(studentRelationshipToJson(_, restriction))
        })
      else None
    },
    fieldRestriction.nested("studentCourseYearDetails").flatMap { restriction =>
      if (canViewProperty(scd, "freshStudentCourseYearDetails"))
        Some("studentCourseYearDetails" -> scd.freshStudentCourseYearDetails.map(jsonStudentCourseYearDetailsObject(_, restriction)))
      else None
    },
    fieldRestriction.nested("moduleRegistrations").flatMap { restriction =>
      if (canViewProperty(scd, "moduleRegistrations"))
        Some("moduleRegistrations", scd.moduleRegistrations.map(moduleRegistrationToJson(_, restriction)))
      else None
    }
  ).flatten.toMap

}
