package uk.ac.warwick.tabula.coursework.commands

import uk.ac.warwick.tabula.coursework.commands.assignments.EditAssignmentCommand
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.services.AssignmentMembershipService

class EditAssignmentCommandTest extends TestBase with Mockito {
  @Test def instantiate {
    val assignment = new Assignment()
		assignment.assignmentMembershipService = mock[AssignmentMembershipService]

    assignment.addDefaultFields()
    assignment.module = new Module()
    assignment.members = null // simulate a slightly older assignment that has no initial linked group
    assignment.name = "Big Essay"
    assignment.commentField.get.value = "Instructions"

    val command = new EditAssignmentCommand(assignment.module, assignment, currentUser)
    command.name should be ("Big Essay")
    command.members should be ('empty)
    command.comment should be ("Instructions")
  }
}