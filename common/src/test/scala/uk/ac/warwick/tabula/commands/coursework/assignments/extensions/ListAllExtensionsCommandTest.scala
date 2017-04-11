package uk.ac.warwick.tabula.commands.coursework.assignments.extensions

import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.data.AssessmentDao
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.{Assignment, Department}
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.helpers.coursework.ExtensionGraph
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.userlookup.User

class ListAllExtensionsCommandTest extends TestBase with Mockito {

	trait Environment {
		val dept: Department = Fixtures.department("fi", "Film")
		val year: AcademicYear = AcademicYear.guessSITSAcademicYearByDate(new DateTime())

		val user1: User = Fixtures.user("0123456", "cuspxp")
		val userLookup: UserLookupService = smartMock[UserLookupService]
		userLookup.getUserByUserId("cuspxp") returns user1

		val extension1: Extension = Fixtures.extension("0123456", "cuspxp")

		val assignment1: Assignment = Fixtures.assignment("assignment 1")
		assignment1.extensions.add(extension1)
		extension1.assignment = assignment1

		val assignmentDao: AssessmentDao = smartMock[AssessmentDao]
		assignmentDao.getAssignments(dept, year) returns Seq(assignment1)

	}

	@Test
	def testApply() {
		new Environment {
			val command = new ListAllExtensionsCommand(dept, year)
			command.assignmentDao = assignmentDao
			command.userLookup = userLookup

			val graph: ExtensionGraph = command.apply().head
			graph.user should be (user1)
			graph.isAwaitingReview should be (extension1.awaitingReview)
			graph.hasApprovedExtension should be (extension1.approved)
			graph.hasRejectedExtension should be (extension1.rejected)
			graph.duration should be (extension1.duration)
			graph.requestedExtraDuration should be (extension1.requestedExtraDuration)
			graph.extension should be (Some(extension1))
		}
	}
}
