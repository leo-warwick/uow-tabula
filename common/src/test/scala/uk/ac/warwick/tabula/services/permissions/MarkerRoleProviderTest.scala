package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.{Assignment, Department, Module, SeenSecondMarkingLegacyWorkflow}
import uk.ac.warwick.tabula.helpers.MutablePromise
import uk.ac.warwick.tabula.helpers.Promises._
import uk.ac.warwick.tabula.roles.Marker
import uk.ac.warwick.tabula.services.AssessmentService
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

class MarkerRoleProviderTest extends TestBase with Mockito {

  val mockAssignmentService: AssessmentService = smartMock[AssessmentService]
  val provider = new MarkerRoleProvider {
    override val assignmentService: MutablePromise[AssessmentService] = promise {
      mockAssignmentService
    }
  }

  val mw1: SeenSecondMarkingLegacyWorkflow = Fixtures.seenSecondMarkingLegacyWorkflow("workflow is marker")
  mw1.firstMarkers.knownType.addUserId("cuscav")

  val mw2: SeenSecondMarkingLegacyWorkflow = Fixtures.seenSecondMarkingLegacyWorkflow("workflow not marker")
  mw2.firstMarkers.knownType.addUserId("cusebr")

  val assignmentIsMarker1: Assignment = Fixtures.assignment("assignment is marker 1")
  assignmentIsMarker1.markingWorkflow = mw1

  val assignmentIsMarker2: Assignment = Fixtures.assignment("assignment is marker 2")
  assignmentIsMarker2.markingWorkflow = mw1

  val assignmentNotMarker: Assignment = Fixtures.assignment("not marker")
  assignmentNotMarker.markingWorkflow = mw2

  val mod1: Module = Fixtures.module("mod1", "mod 1")
  val mod2: Module = Fixtures.module("mod2", "mod 2")
  val mod3: Module = Fixtures.module("mod3", "mod 3")

  val dept: Department = Fixtures.department("dept", "department")

  mod1.adminDepartment = dept
  mod2.adminDepartment = dept
  mod3.adminDepartment = dept

  assignmentIsMarker1.module = mod1
  assignmentIsMarker2.module = mod2
  assignmentNotMarker.module = mod3

  dept.modules.addAll(Seq(mod1, mod2, mod3).asJava)
  mod1.assignments.addAll(Seq(assignmentIsMarker1, assignmentNotMarker).asJava)
  mod2.assignments.addAll(Seq(assignmentIsMarker2).asJava)
  mod3.assignments.addAll(Seq(assignmentNotMarker).asJava)

  val cuscavUser = new CurrentUser(new User("cuscav"), new User("cuscav"))

  mockAssignmentService.getAssignmentsByDepartmentAndMarker(dept, cuscavUser, None) returns Seq(assignmentIsMarker1, assignmentIsMarker2)
  mockAssignmentService.getAssignmentsByModuleAndMarker(mod1, cuscavUser, None) returns Seq(assignmentIsMarker1)
  mockAssignmentService.getAssignmentsByModuleAndMarker(mod2, cuscavUser, None) returns Seq(assignmentIsMarker2)
  mockAssignmentService.getAssignmentsByModuleAndMarker(mod3, cuscavUser, None) returns Seq()

  mockAssignmentService.getCM2AssignmentsByDepartmentAndMarker(dept, cuscavUser, None) returns Seq()
  mockAssignmentService.getCM2AssignmentsByModuleAndMarker(mod1, cuscavUser, None) returns Seq()
  mockAssignmentService.getCM2AssignmentsByModuleAndMarker(mod2, cuscavUser, None) returns Seq()
  mockAssignmentService.getCM2AssignmentsByModuleAndMarker(mod3, cuscavUser, None) returns Seq()

  @Test def forAssignment() = withCurrentUser(cuscavUser) {
    provider.getRolesFor(currentUser, assignmentIsMarker1) should be(Seq(Marker(assignmentIsMarker1)))
    provider.getRolesFor(currentUser, assignmentIsMarker2) should be(Seq(Marker(assignmentIsMarker2)))
    provider.getRolesFor(currentUser, assignmentNotMarker) should be(Seq())
  }

  @Test def forModule() = withCurrentUser(cuscavUser) {
    // Since MarkerRole is only ever granted at the Assessment level, we shouldn't be returning any for Modules
    provider.getRolesFor(currentUser, mod1) should be(Nil)
    provider.getRolesFor(currentUser, mod2) should be(Nil)
    provider.getRolesFor(currentUser, mod3) should be(Nil)
  }

  @Test def forDepartment() = withCurrentUser(cuscavUser) {
    // Since MarkerRole is only ever granted at the Assessment level, we shouldn't be returning any for Departments
    provider.getRolesFor(currentUser, dept) should be(Nil)
  }

  @Test def handlesDefault() = withCurrentUser(cuscavUser) {
    provider.getRolesFor(currentUser, Fixtures.assignmentFeedback()) should be(Seq())
  }

}