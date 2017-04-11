package uk.ac.warwick.tabula.commands.profiles

import org.hamcrest.Description
import org.hamcrest.Matchers._
import org.hibernate.NullPrecedence
import org.hibernate.criterion.{Order, Restrictions}
import org.joda.time.DateTime
import org.mockito.ArgumentMatcher
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.ScalaRestriction._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AliasAndJoinType, ScalaOrder, ScalaRestriction}
import uk.ac.warwick.tabula.services.{ProfileService, ProfileServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

import scala.collection.JavaConverters._


class FilterStudentsCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends ProfileServiceComponent {
		val profileService: ProfileService = mock[ProfileService]

		// this seems to need the 'anArgThat(anything)' matcher to correctly set up a catch-all mocked method, 'any' just isn't good enough
		profileService.findStudentsByRestrictions(anArgThat(anything), anArgThat(anything), anArgThat(anything), anArgThat(anything), anArgThat(anything), anArgThat(anything)) returns ((0, Seq(new StudentMember)))
	}

	trait Fixture {
		val department: Department = Fixtures.department("arc", "School of Architecture")
		val subDept: Department = Fixtures.department("arc-ug", "Architecture Undergraduates")
		subDept.parent = department
		department.children.add(subDept)

		val mod1: Module = Fixtures.module("ac101", "Introduction to Architecture")
		val mod2: Module = Fixtures.module("ac102", "Architecture Basics")
		val mod3: Module = Fixtures.module("ac901", "Postgraduate Thesis")

		department.modules.add(mod3)
		subDept.modules.add(mod2)
		subDept.modules.add(mod1)

		val route1: Route = Fixtures.route("a501", "Architecture BA")
		val route2: Route = Fixtures.route("a502", "Architecture BA with Intercalated year")
		val route3: Route = Fixtures.route("a9p1", "Architecture MA")

		department.routes.add(route3)
		subDept.routes.add(route2)
		subDept.routes.add(route1)

		val sprF: SitsStatus = Fixtures.sitsStatus("F", "Fully Enrolled", "Fully Enrolled for this Session")
		val sprP: SitsStatus = Fixtures.sitsStatus("P", "Permanently Withdrawn", "Permanently Withdrawn")

		val moaFT: ModeOfAttendance = Fixtures.modeOfAttendance("F", "FT", "Full time")
		val moaPT: ModeOfAttendance = Fixtures.modeOfAttendance("P", "PT", "Part time")

		val year: AcademicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

	}

	@Test
	def bindLoadsNonWithdrawnStatuses() { new Fixture {
		val command = new FilterStudentsCommand(department, year) with CommandTestSupport

		command.profileService.allSprStatuses(department) returns Seq(sprF, sprP)

		command.sprStatuses.asScala should be ('empty)

		command.onBind(null)

		command.sprStatuses.asScala should be (Seq(sprF))
	}}

	@Test
	def commandApplyDefaults() { new Fixture {
		val command = new FilterStudentsCommand(department, year) with CommandTestSupport
		command.applyInternal()

		val expectedRestrictions = Seq()

		verify(command.profileService, times(1)).findStudentsByRestrictions(
			isEq(department),
			isEq(year),
			anArgThat(seqToStringMatches(expectedRestrictions)),
			anArgThat(seqToStringMatches(Seq(ScalaOrder(Order.asc("lastName").nulls(NullPrecedence.LAST)), ScalaOrder(Order.asc("firstName").nulls(NullPrecedence.LAST))))),
			isEq(50),
			isEq(0)
		)
	}}

	@Test
	def commandApplyComplicated() { new Fixture {
		val command = new FilterStudentsCommand(department, year) with CommandTestSupport

		command.studentsPerPage = 10
		command.page = 3

		command.courseTypes = JArrayList(CourseType.UG, CourseType.PreSessional)
		command.routes = JArrayList(route1, route3)
		command.modesOfAttendance = JArrayList(moaFT)
		command.yearsOfStudy = JArrayList(1, 5)
		command.sprStatuses = JArrayList(sprP)
		command.modules = JArrayList(mod2, mod3)

		command.applyInternal()

		val courseTypeRestriction = new ScalaRestriction(
			Restrictions.disjunction()
				.add(Restrictions.like("course.code", "U%"))
				.add(Restrictions.like("course.code", "N%"))
		)
		courseTypeRestriction.alias("mostSignificantCourse", AliasAndJoinType("mostSignificantCourse"))
		courseTypeRestriction.alias("mostSignificantCourse.course", AliasAndJoinType("course"))

		val routeRestriction = new ScalaRestriction(Restrictions.in("mostSignificantCourse.currentRoute.code", JArrayList(route1.code, route3.code)))
		routeRestriction.alias("mostSignificantCourse", AliasAndJoinType("mostSignificantCourse"))

		val moaRestriction = new ScalaRestriction(Restrictions.in("studentCourseYearDetails.modeOfAttendance", JArrayList(moaFT)))
		moaRestriction.alias("mostSignificantCourse", AliasAndJoinType("mostSignificantCourse"))
		moaRestriction.alias("mostSignificantCourse.studentCourseYearDetails", AliasAndJoinType("studentCourseYearDetails"))

		val yosRestriction = new ScalaRestriction(Restrictions.in("studentCourseYearDetails.yearOfStudy", JArrayList(1, 5)))
		yosRestriction.alias("mostSignificantCourse", AliasAndJoinType("mostSignificantCourse"))
		yosRestriction.alias("mostSignificantCourse.studentCourseYearDetails", AliasAndJoinType("studentCourseYearDetails"))

		val sprRestriction = new ScalaRestriction(Restrictions.in("mostSignificantCourse.statusOnRoute", JArrayList(sprP)))
		sprRestriction.alias("mostSignificantCourse", AliasAndJoinType("mostSignificantCourse"))

		// no need to test ScalaRestriction.inIfNotEmptyMultipleProperties - it's tested in ScalaRestrictionTest
		val modRestriction: ScalaRestriction = inIfNotEmptyMultipleProperties(
			Seq("moduleRegistration.module", "moduleRegistration.academicYear"),
			Seq(Seq(mod2, mod3), Seq(AcademicYear.guessSITSAcademicYearByDate(DateTime.now)))
		).get

		modRestriction.alias("mostSignificantCourse", AliasAndJoinType("mostSignificantCourse"))
		modRestriction.alias("mostSignificantCourse._moduleRegistrations", AliasAndJoinType("moduleRegistration"))

		val expectedRestrictions: Seq[ScalaRestriction] = Seq(
			courseTypeRestriction,
			routeRestriction,
			moaRestriction,
			yosRestriction,
			sprRestriction,
			modRestriction
		) ++ command.latestStudentCourseYearDetailsForYearRestrictions(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))

		verify(command.profileService, times(1)).findStudentsByRestrictions(
			isEq(department),
			isEq(year),
			anArgThat(seqToStringMatches(expectedRestrictions)),
			anArgThat(seqToStringMatches(Seq(ScalaOrder(Order.asc("lastName").nulls(NullPrecedence.LAST)), ScalaOrder(Order.asc("firstName").nulls(NullPrecedence.LAST))))),
			isEq(10),
			isEq(20)
		)
	}}

	@Test
	def commandApplyDefaultsWithAliasedSort() { new Fixture {

		val command = new FilterStudentsCommand(department, year) with CommandTestSupport
		command.sortOrder = JArrayList(Order.desc("studentCourseYearDetails.yearOfStudy"))

		command.applyInternal()

		val expectedRestrictions = Seq()

		val expectedOrders = Seq(
			ScalaOrder(
				Order.desc("studentCourseYearDetails.yearOfStudy").nulls(NullPrecedence.LAST),
				"mostSignificantCourse" -> AliasAndJoinType("mostSignificantCourse"),
				"mostSignificantCourse.studentCourseYearDetails" -> AliasAndJoinType("studentCourseYearDetails")
			),
			ScalaOrder(Order.asc("lastName").nulls(NullPrecedence.LAST)),
			ScalaOrder(Order.asc("firstName").nulls(NullPrecedence.LAST))
		)

		verify(command.profileService, times(1)).findStudentsByRestrictions(
			isEq(department),
			isEq(year),
			anArgThat(seqToStringMatches(expectedRestrictions)),
			anArgThat(seqToStringMatches(expectedOrders)),
			isEq(50),
			isEq(0)
		)
	}}

	def seqToStringMatches[A](o: Seq[A]) = new ArgumentMatcher[Seq[A]] {
		def matches(that: Any): Boolean = that match {
			case s: Seq[_] => s.length == o.length && (o, s).zipped.forall { case (l, r) => l.toString == r.toString }
			case _ => false
		}

		override def describeTo(description: Description): Unit = description.appendText(o.toString())
	}

}