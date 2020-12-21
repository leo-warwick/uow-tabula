package uk.ac.warwick.tabula.commands

import org.hibernate.NullPrecedence
import org.hibernate.`type`.{StringType, Type}
import org.hibernate.criterion.{DetachedCriteria, Restrictions}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.data.ScalaRestriction._
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.{Permission, PermissionsTarget}
import uk.ac.warwick.tabula.permissions.Permissions.Profiles
import uk.ac.warwick.tabula.services.{ProfileServiceComponent, SecurityServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods

import scala.jdk.CollectionConverters._

object FilterStudentsOrRelationships {

  val MaxStudentsPerPage = 100
  val DefaultStudentsPerPage = 50
  val MaxYearsOfStudy = 12

}

trait FilterStudentsOrRelationships extends FiltersStudentsBase with PermissionsCheckingMethods with ProfileServiceComponent with SecurityServiceComponent {

  def getAliasPaths(sitsTable: String): Seq[(String, AliasAndJoinType)]

  protected def buildOrders(): Seq[ScalaOrder] =
    (sortOrder.asScala.toSeq ++ defaultOrder).map { underlying =>
      underlying.getPropertyName match {
        case r"""([^\.]+)${aliasPath}\..*""" => ScalaOrder(underlying.nulls(NullPrecedence.LAST), getAliasPaths(aliasPath): _*)
        case _ => ScalaOrder(underlying.nulls(NullPrecedence.LAST))
      }
    }

  def attendanceRestriction: Option[ScalaRestriction] = inIfNotEmpty(
    "studentCourseYearDetails.modeOfAttendance", modesOfAttendance.asScala,
    getAliasPaths("studentCourseYearDetails"): _*
  )

  def courseTypeRestriction: Option[ScalaRestriction] = startsWithIfNotEmpty(
    "course.code", courseTypes.asScala.flatMap(_.courseCodeChars.map(_.toString)),
    getAliasPaths("course"): _*
  )

  def specificCourseTypeRestriction: Option[ScalaRestriction] = startsWithIfNotEmpty(
    "course.code", specificCourseTypes.asScala.map(_.code.toString),
    getAliasPaths("course"): _*
  )

  def routeRestriction: Option[ScalaRestriction]

  def courseRestriction: Option[ScalaRestriction] = inIfNotEmpty(
    "course.code", courses.asScala.map(_.code),
    getAliasPaths("course"): _*
  )

  def yearOfStudyRestriction: Option[ScalaRestriction] = inIfNotEmpty(
    "studentCourseYearDetails.yearOfStudy", yearsOfStudy.asScala,
    getAliasPaths("studentCourseYearDetails"): _*
  )

  def levelCodeRestriction: Option[ScalaRestriction]

  def studyLevelCodeRestriction: Option[ScalaRestriction] = inIfNotEmpty(
    "studentCourseYearDetails.studyLevel", studyLevelCodes.asScala,
    getAliasPaths("studentCourseYearDetails"): _*
  )

  def registeredModulesRestriction(year: AcademicYear): Option[ScalaRestriction] = inIfNotEmptyMultipleProperties(
    Seq("moduleRegistration.module", "moduleRegistration.academicYear", "moduleRegistration.deleted"),
    Seq(modules.asScala, Seq(year), Seq(false)),
    getAliasPaths("moduleRegistration"): _*
  )

  def sprStatusRestriction: Option[ScalaRestriction]

  def tier4Restriction: Option[ScalaRestriction] = atLeastOneIsTrue(
    "studentCourseYearDetails.casUsed", "studentCourseYearDetails.tier4Visa", otherCriteria.contains("Tier 4 only"),
    getAliasPaths("studentCourseYearDetails"): _*
  )

  def notTier4Restriction: Option[ScalaRestriction] = neitherIsTrue(
    "studentCourseYearDetails.casUsed", "studentCourseYearDetails.tier4Visa", otherCriteria.contains("Not Tier 4 only"),
    getAliasPaths("studentCourseYearDetails"): _*
  )

  def visitingRestriction: Option[ScalaRestriction] = isIfTicked(
    "department.code",
    "io",
    otherCriteria.contains("Visiting"),
    getAliasPaths("department"): _*
  )

  def enrolledOrCompletedRestriction: Option[ScalaRestriction] = isIfTicked(
    "studentCourseYearDetails.enrolledOrCompleted",
    true,
    otherCriteria.contains("Enrolled for year or course completed"),
    getAliasPaths("studentCourseYearDetails"): _*
  )

  def isFinalistRestriction: Option[ScalaRestriction]

  def isNotFinalistRestriction: Option[ScalaRestriction]

  def hallOfResidenceRestriction: Option[ScalaRestriction] = inIfNotEmpty(
    "termtimeAddress.line2", hallsOfResidence.asScala,
    getAliasPaths("termtimeAddress"): _*
  )

  def postcodeRestriction: Option[ScalaRestriction] = inIfNotEmpty(
    "currentAddress._normalisedPostcode", postcodes.asScala.map(_.replaceAll("[^A-Za-z0-9]+", "").toUpperCase),
    getAliasPaths("currentAddress"): _*
  )

  protected def buildRestrictions(
    user: CurrentUser,
    departments: Seq[Department],
    year: AcademicYear,
    additionalRestrictions: Seq[ScalaRestriction] = Seq.empty
  ): Seq[ScalaRestriction] = {
    def hasPermissionsOnAllDepartments(permission: Permission): Boolean =
      if (departments.nonEmpty) departments.forall(d => securityService.can(user, permission, d))
      else securityService.can(user, permission, PermissionsTarget.Global)

    val tier4Restrictions: Seq[ScalaRestriction] = {
      lazy val filteringOnTier4 = otherCriteria.contains("Tier 4 only") ||  otherCriteria.contains("Not Tier 4 only")
      lazy val hasTier4Permissions = hasPermissionsOnAllDepartments(Profiles.Read.Tier4VisaRequirement)
      if (filteringOnTier4 && hasTier4Permissions) {
        Seq(tier4Restriction, notTier4Restriction).flatten
      } else {
        Seq()
      }
    }

    lazy val hasAddressPermissions = hasPermissionsOnAllDepartments(Profiles.Read.HomeAndTermTimeAddresses)
    val addressRestrictions: Seq[ScalaRestriction] = Seq(
      hallOfResidenceRestriction,
      postcodeRestriction,
    ).filter(_ => hasAddressPermissions).flatten

    buildRestrictionsInternal(year: AcademicYear, additionalRestrictions ++ tier4Restrictions ++ addressRestrictions)
  }

  // have to give this a different name to the method above as both have a default value for additionalRestrictions
  protected def buildRestrictionsNoTier4(year: AcademicYear, additionalRestrictions: Seq[ScalaRestriction] = Seq.empty): Seq[ScalaRestriction] = {
    buildRestrictionsInternal(year: AcademicYear, additionalRestrictions: Seq[ScalaRestriction])
  }

  private def buildRestrictionsInternal(year: AcademicYear, additionalRestrictions: Seq[ScalaRestriction]): Seq[ScalaRestriction] = {

    val restrictions = Seq(
      courseTypeRestriction,
      specificCourseTypeRestriction,
      routeRestriction,
      courseRestriction,
      attendanceRestriction,
      yearOfStudyRestriction,
      levelCodeRestriction,
      studyLevelCodeRestriction,
      sprStatusRestriction,
      registeredModulesRestriction(year),
      visitingRestriction,
      enrolledOrCompletedRestriction,
      isFinalistRestriction,
      isNotFinalistRestriction,
    ).flatten ++ additionalRestrictions

    if (restrictions.exists(_.aliases.keys.exists(key => key.contains("studentCourseYearDetails")))) {
      // We need to restrict the studentCourseYearDetails to the latest one by year
      restrictions ++ latestStudentCourseYearDetailsForYearRestrictions(year)
    } else restrictions
  }

  protected def latestYearDetailsForYear(year: AcademicYear): DetachedCriteria

  def latestStudentCourseYearDetailsForYearRestrictions(year: AcademicYear): Seq[ScalaRestriction] = {
    // We need to restrict the studentCourseYearDetails to the latest one by year
    Seq(
      new ScalaRestriction(HibernateHelpers.isSubquery("studentCourseYearDetails.sceSequenceNumber", latestYearDetailsForYear(year))),
      new ScalaRestriction(HibernateHelpers.is("studentCourseYearDetails.academicYear", year))
    )
  }

  lazy val allYearsOfStudy: Seq[Int] = 1 to FilterStudentsOrRelationships.MaxYearsOfStudy

  def includeTier4Filters: Boolean
  lazy val allOtherCriteria: Seq[String] = (if(includeTier4Filters) Seq("Tier 4 only") else Seq()) ++ Seq("Visiting", "Enrolled for year or course completed")
}
