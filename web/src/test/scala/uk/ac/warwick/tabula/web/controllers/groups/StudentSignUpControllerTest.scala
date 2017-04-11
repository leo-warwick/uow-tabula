package uk.ac.warwick.tabula.web.controllers.groups

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

class StudentSignUpControllerTest extends TestBase with Mockito {

	val smallGroupSet: SmallGroupSet = Fixtures.smallGroupSet("test")
	smallGroupSet.academicYear = AcademicYear(2015)

	@Test
	def signupControllerConstructsAppropriateCommand() {
		withUser("test") {
			val controller = new StudentSignUpController
			val command = controller.command(new SmallGroupSet, currentUser)
			command should be(anInstanceOf[AllocateSelfToGroupCommand])
			command should be(anInstanceOf[AllocateSelfToGroupValidator])
			command should be(anInstanceOf[StudentSignupCommandPermissions])
			command should be(anInstanceOf[Appliable[SmallGroupSet]])
		}
	}

	@Test
	def signUpAppliesCommand(){
		val controller = new StudentSignUpController
		val command = mock[Appliable[SmallGroupSet]]
		command.apply() returns smallGroupSet
		controller.signUp(command, new BindException(command, "command")).viewName should be(s"redirect:/groups/${smallGroupSet.academicYear.startYear.toString}")
		verify(command, times(1)).apply()
	}

	@Test
	def unSignupControllerConstructsAppropriateCommand() {
		withUser("test") {
			val controller = new StudentUnSignUpController
			val command = controller.command(new SmallGroupSet, currentUser)
			command should be(anInstanceOf[DeallocateSelfFromGroupCommand])
			command should be(anInstanceOf[DeallocateSelfFromGroupValidator])
			command should be(anInstanceOf[StudentSignupCommandPermissions])
			command should be(anInstanceOf[Appliable[SmallGroupSet]])
		}
	}

	@Test
	def unSignUpAppliesCommand(){
		val controller = new StudentUnSignUpController
		val command = mock[Appliable[SmallGroupSet]]
		command.apply() returns smallGroupSet
		controller.signUp(command, new BindException(command, "command")).viewName should be(s"redirect:/groups/${smallGroupSet.academicYear.startYear.toString}")
		verify(command, times(1)).apply()
	}
}
