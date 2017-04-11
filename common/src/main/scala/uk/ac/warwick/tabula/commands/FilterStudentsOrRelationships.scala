package uk.ac.warwick.tabula.commands

import org.hibernate.NullPrecedence
import org.hibernate.criterion.DetachedCriteria
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.ScalaRestriction._
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.ProfileServiceComponent
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods

import scala.collection.JavaConverters._

object FilterStudentsOrRelationships {

	val MaxStudentsPerPage = 100
	val DefaultStudentsPerPage = 50
	val MaxYearsOfStudy = 12

}

trait FilterStudentsOrRelationships extends FiltersStudentsBase with PermissionsCheckingMethods with ProfileServiceComponent {

	def getAliasPaths(sitsTable: String): Seq[(String, AliasAndJoinType)]

	protected def buildOrders(): Seq[ScalaOrder] =
		(sortOrder.asScala ++ defaultOrder).map { underlying =>
			underlying.getPropertyName match {
				case r"""([^\.]+)${aliasPath}\..*""" => ScalaOrder(underlying.nulls(NullPrecedence.LAST), getAliasPaths(aliasPath) : _*)
				case _ => ScalaOrder(underlying.nulls(NullPrecedence.LAST))
			}
		}

	def attendanceRestriction: Option[ScalaRestriction] = inIfNotEmpty(
		"studentCourseYearDetails.modeOfAttendance", modesOfAttendance.asScala,
		getAliasPaths("studentCourseYearDetails") : _*
	)

	def courseTypeRestriction: Option[ScalaRestriction] = startsWithIfNotEmpty(
		"course.code", courseTypes.asScala.map { _.courseCodeChar.toString },
		getAliasPaths("course") : _*
	)

	def routeRestriction: Option[ScalaRestriction]

	def yearOfStudyRestriction: Option[ScalaRestriction] = inIfNotEmpty(
		"studentCourseYearDetails.yearOfStudy", yearsOfStudy.asScala,
		getAliasPaths("studentCourseYearDetails") : _*
	)

	def registeredModulesRestriction(year: AcademicYear): Option[ScalaRestriction] = inIfNotEmptyMultipleProperties(
		Seq("moduleRegistration.module", "moduleRegistration.academicYear"),
		Seq(modules.asScala, Seq(year)),
		getAliasPaths("moduleRegistration") : _*
	)

	def sprStatusRestriction: Option[ScalaRestriction]

	def tier4Restriction: Option[ScalaRestriction] = atLeastOneIsTrue(
		"studentCourseYearDetails.casUsed", "studentCourseYearDetails.tier4Visa", otherCriteria.contains("Tier 4 only"),
		getAliasPaths("studentCourseYearDetails") : _*
	)

	def visitingRestriction: Option[ScalaRestriction] = isIfTicked(
		"department.code",
		"io",
		otherCriteria.contains("Visiting"),
		getAliasPaths("department") : _*
	)

	def enrolledOrCompletedRestriction: Option[ScalaRestriction] = isIfTicked(
		"studentCourseYearDetails.enrolledOrCompleted",
		true,
		otherCriteria.contains("Enrolled for year or course completed"),
		getAliasPaths("studentCourseYearDetails") : _*
	)

	protected def buildRestrictions(year: AcademicYear): Seq[ScalaRestriction] = {
		val restrictions = Seq(
			courseTypeRestriction,
			routeRestriction,
			attendanceRestriction,
			yearOfStudyRestriction,
			sprStatusRestriction,
			registeredModulesRestriction(year),
			tier4Restriction,
			visitingRestriction,
			enrolledOrCompletedRestriction
		).flatten

		if (restrictions.exists { _.aliases.keys.exists(key => key.contains("studentCourseYearDetails")) }) {
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
	lazy val allOtherCriteria: Seq[String] = Seq("Tier 4 only", "Visiting", "Enrolled for year or course completed")
}
