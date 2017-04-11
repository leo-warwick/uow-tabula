package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.data.model.permissions.CustomRoleDefinition
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.roles.{HistoricStudentRelationshipAgentRoleDefinition, HistoricStudentRelationshipAgent}

class HistoricStudentRelationshipRoleProviderTest extends StudentRelationshipRoleTestBase {

	val provider = new HistoricStudentRelationshipAgentRoleProvider {
		this.relationshipService = relService
	}

	relService.getAllPastAndPresentRelationships(student) returns Seq(oldRel, rel)

	@Test
	def agent() = {
		withUser("cuslaj", "0123456") {
			// the current tutor is still a HistoricStudentRelationshipAgent incase the student becomes perminantly withdrawn or similar
			provider.getRolesFor(currentUser, student).force should be(Seq(HistoricStudentRelationshipAgent(student, personalTutor)))
		}
		withUser("cusxad", "7891011") {
			provider.getRolesFor(currentUser, student).force should be(Seq(HistoricStudentRelationshipAgent(student, personalTutor)))
		}
	}

	@Test
	def notAgent() = withUser("cuscav", "9999999") {
		provider.getRolesFor(currentUser, student) should be (Nil)
	}

	@Test
	def handlesDefault() = withUser("cuslaj", "0123456") {
		provider.getRolesFor(currentUser, Fixtures.department("in", "IN202")) should be (Nil)
	}

	@Test
	def departmentOverride() = withUser("cuslaj", "0123456") {

		val heronStudies = Fixtures.department("hz", "Heronz")
		student.mostSignificantCourse = Fixtures.studentCourseDetails(student, heronStudies)

		val heronOverlordRoleDefinition = new CustomRoleDefinition
		heronOverlordRoleDefinition.department = heronStudies
		heronOverlordRoleDefinition.name = "Mighty heron overlord"
		heronOverlordRoleDefinition.builtInBaseRoleDefinition = HistoricStudentRelationshipAgentRoleDefinition(personalTutor)
		heronOverlordRoleDefinition.replacesBaseDefinition = true
		heronStudies.customRoleDefinitions = JArrayList(heronOverlordRoleDefinition)

		provider.getRolesFor(currentUser, student).head.getName should be ("Mighty heron overlord")
	}

}
