package uk.ac.warwick.tabula.services.scheduling

import org.junit.After
import org.springframework.jdbc.core.namedparam.{MapSqlParameterSource, NamedParameterUtils}
import org.springframework.jdbc.datasource.embedded.{EmbeddedDatabase, EmbeddedDatabaseBuilder}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, UpstreamAssessmentGroup, UpstreamModuleRegistration}
import uk.ac.warwick.tabula.{AcademicYear, Mockito, TestBase}

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.reflect._

trait EmbeddedSits {
	val sits: EmbeddedDatabase = new EmbeddedDatabaseBuilder().addScript("sits.sql").build()

	@After def afterTheFeast() {
		sits.shutdown()
	}
}

// scalastyle:off magic.number
class AssignmentImporterTest extends TestBase with Mockito with EmbeddedSits {

	val assignmentImporter = new AssignmentImporterImpl
	assignmentImporter.sits = sits
	AssignmentImporter.sitsSchema = "public"
	AssignmentImporter.sqlStringCastFunction = ""
	AssignmentImporter.dialectRegexpLike = "regexp_matches"
	assignmentImporter.afterPropertiesSet()

	val NONE = AssessmentComponent.NoneAssessmentGroup

	@Test def groupImportSql() {
		// Not really testing AssignmentImporter but the behaviour of the query class for IN(..)
		// parameters. The SQL has to have the brackets, and the parameter value has to be a
		// Java List - a Scala collection will not be recognised and won't be expanded into multiple
		// question marks.
		val paramMap = Map(
				"module_code" -> "md101",
				"academic_year_code" -> JArrayList("10/11","11/12"),
				"mav_occurrence" -> "A",
				"assessment_group" -> "A"
		)
		val paramSource = new MapSqlParameterSource(paramMap.asJava)
		val sqlToUse = NamedParameterUtils.substituteNamedParameters(AssignmentImporter.GetAllAssessmentGroups, paramSource)
		sqlToUse.trim should endWith ("(?, ?)")
	}

	@Test def importMembers() { withFakeTime(dateTime(2012, 5)) {
		assignmentImporter.yearsToImport = Seq(AcademicYear(2011), AcademicYear(2012))
		var members = ArrayBuffer[UpstreamModuleRegistration]()
		assignmentImporter.allMembers { mr =>
			members += mr
		}

		members.size should be (5)
	}}

	@Test def allAssessmentGroups() { withFakeTime(dateTime(2012, 5)) {
		assignmentImporter.yearsToImport = Seq(AcademicYear(2011), AcademicYear(2012))
		val allGroups = sorted(assignmentImporter.getAllAssessmentGroups)
		val tuples = allGroups.map(asTuple)

		/* We currently get the NONE assessmentgroups even for groups
		   that aren't empty. We do only generate AssessmentComponents
		   when we need to, so these groups shouldnt' appear in the UI
		   unnecessarily - but we could change this query to filter it
		   down a bit more. */
		tuples should be (Seq(
			("CH115-30","A","A","A01"),
			("CH115-30",NONE,NONE,NONE),
			("CH120-15","A","A","A01"),
			("CH120-15",NONE,NONE,NONE),
			("CH130-15","A","A","A01"),
			("CH130-15",NONE,NONE,NONE),
			("CH130-20","A","A","A01"),
			("CH130-20",NONE,NONE,NONE)
		))

	}}

	@Test def allAssessmentComponents() { withFakeTime(dateTime(2012, 5)) {
		assignmentImporter.yearsToImport = Seq(AcademicYear(2011), AcademicYear(2012))
		val components = sorted(assignmentImporter.getAllAssessmentComponents)
		val tuples = components map asTuple

		tuples should be (Seq(
			("CH115-30","A","Chemicals Essay"),
			("CH115-30","NONE","Students not registered for assessment"),
			("CH120-15","A","Chemistry Dissertation"),
			("CH130-15","A","Chem 130 A01"),
			("CH130-20","A","Chem 130 A01 (20 CATS)")
		))

	}}

	private def asTuple(component: AssessmentComponent) = (component.moduleCode, component.assessmentGroup, component.name)
	private def sorted(components: Seq[AssessmentComponent])(implicit ev: ClassTag[AssessmentComponent]) = components sortBy asTuple

	// Converting to tuples for easier sorting/comparing
	private def asTuple(group: UpstreamAssessmentGroup) = (group.moduleCode, group.assessmentGroup, group.occurrence, group.sequence)
	private def sorted(groups: Seq[UpstreamAssessmentGroup]) = groups sortBy asTuple

}
