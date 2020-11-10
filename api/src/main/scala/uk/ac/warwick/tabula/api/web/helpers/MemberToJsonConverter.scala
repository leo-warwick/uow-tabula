package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.api.web.helpers.MemberApiFreemarkerHelper._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.web.views.ScalaFreemarkerConfigurationComponent

import scala.jdk.CollectionConverters._

/**
  * We use Freemarker to ensure that we use the same logic as in ScalaBeansWrapper, i.e. that we don't expose
  * any properties that the user doesn't have permissions to see.
  */
trait MemberToJsonConverter
  extends StudentCourseDetailsToJsonConverter
    with StudentCourseYearDetailsToJsonConverter
    with MemberApiFreemarkerHelper {
  self: ScalaFreemarkerConfigurationComponent =>

  def jsonMemberObject(member: Member, fieldRestriction: APIFieldRestriction, deep: Boolean = true): Map[String, Any] = {
    val memberProperties = Seq(
      str("universityId", member, "universityId", fieldRestriction),
      str("userId", member, "userId", fieldRestriction),
      str("firstName", member, "firstName", fieldRestriction),
      str("lastName", member, "lastName", fieldRestriction),
      str("email", member, "email", fieldRestriction),
      str("userType", member, "userType.description", fieldRestriction),
      str("fullName", member, "fullName", fieldRestriction),
      str("officialName", member, "fullName", fieldRestriction), // officialName no longer supported  - TAB-6621
      str("homeEmail", member, "homeEmail", fieldRestriction),
      str("fullFirstName", member, "firstName", fieldRestriction), // fullFirstName no longer supported - TAB-6621
      str("title", member, "title", fieldRestriction),
      str("inUseFlag", member, "inUseFlag", fieldRestriction),
      str("jobTitle", member, "jobTitle", fieldRestriction),
      str("phoneNumber", member, "phoneNumber", fieldRestriction),
      str("nationality", member, "nationality", fieldRestriction),
      str("secondNationality", member, "secondNationality", fieldRestriction),
      str("mobileNumber", member, "mobileNumber", fieldRestriction),
      str("groupName", member, "groupName", fieldRestriction),
      fieldRestriction.nested("affiliatedDepartments").flatMap { restriction =>
        if (canViewProperty(member, "affiliatedDepartments"))
          Some("affiliatedDepartments" -> member.affiliatedDepartments.map(departmentToJson(_, restriction)))
        else None
      },
      fieldRestriction.nested("touchedDepartments").flatMap { restriction =>
        if (canViewProperty(member, "touchedDepartments"))
          Some("touchedDepartments" -> member.touchedDepartments.map(departmentToJson(_, restriction)))
        else None
      },
      fieldRestriction.nested("homeDepartment").flatMap { restriction =>
        if (canViewProperty(member, "homeDepartment"))
          Some("homeDepartment", departmentToJson(member.homeDepartment, restriction))
        else None
      },
      date("inactivationDate", member, "inactivationDate", fieldRestriction),
      date("dateOfBirth", member, "dateOfBirth", fieldRestriction)
    ).flatten.toMap

    val applicantAndStudentProperties = member match {
      case m: ApplicantProperties if deep => Seq(
        fieldRestriction.nested("currentAddress").flatMap { restriction =>
          if (canViewProperty(m, "currentAddress"))
            Some("currentAddress" -> addressToJson(m.currentAddress, restriction))
          else None
        },
        fieldRestriction.nested("disability").flatMap { restriction =>
          if (canViewProperty(m, "disability"))
            m.disability.filter(_.reportable).map {
              "disability" -> disabilityToJson(_, restriction)
            }
          else None
        },
        fieldRestriction.nested("disabilityFundingStatus").flatMap { restriction =>
          if (canViewProperty(m, "disabilityFundingStatus"))
            m.disabilityFundingStatus.map {
              "disabilityFundingStatus" -> disabilityFundingStatusToJson(_, restriction)
            }
          else None
        },
      ).flatten.toMap
      case _ => Map()
    }

    val studentProperties = member match {
      case student: StudentMember if deep => Seq(
        fieldRestriction.nested("termtimeAddress").flatMap { restriction =>
          if (canViewProperty(student, "termtimeAddress"))
            Some("termtimeAddress" -> addressToJson(student.termtimeAddress, restriction))
          else None
        },
        fieldRestriction.nested("nextOfKins").flatMap { restriction =>
          if (canViewProperty(student, "nextOfKins"))
            Some("nextOfKins" -> student.nextOfKins.asScala.map(nextOfKinToJson(_, restriction)))
          else None
        },
        fieldRestriction.nested("studentCourseDetails").flatMap { restriction =>
          if (canViewProperty(student, "freshStudentCourseDetails"))
            Some("studentCourseDetails" -> student.freshStudentCourseDetails.map(jsonStudentCourseDetailsObject(_, restriction)))
          else None
        },
        boolean("tier4VisaRequirement", student, "tier4VisaRequirement", fieldRestriction)
      ).flatten.toMap
      case _ => Map()
    }

    val staffProperties = member match {
      case staff@(_: StaffMember | _: EmeritusMember) if deep => Map() // no additional properties
      case _ => Map()
    }

    memberProperties ++ applicantAndStudentProperties ++ studentProperties ++ staffProperties
  }

}
