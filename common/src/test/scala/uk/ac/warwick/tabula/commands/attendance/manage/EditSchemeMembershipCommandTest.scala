package uk.ac.warwick.tabula.commands.attendance.manage

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.MemberOrUser
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._

import scala.jdk.CollectionConverters._

class EditSchemeMembershipCommandTest extends TestBase with Mockito {

  @Test
  def addUsers(): Unit = {
    withUser("cusfal") {
      val command = new AddsUsersToEditSchemeMembershipCommand with ProfileServiceComponent with UserLookupComponent
        with SecurityServiceComponent with EditSchemeMembershipCommandState {

        val profileService: ProfileService = smartMock[ProfileService]
        val userLookup = new MockUserLookup
        val securityService: SecurityService = smartMock[SecurityService]

        val scheme = null
        val user: CurrentUser = currentUser
      }

      val validStudent = Fixtures.student("1111111", "abcd")
      val validStudentWithUsercode = Fixtures.student("2222222", "cdef")
      val validStudentAlreadyIncluded = Fixtures.student("3333333", "dcba")
      val invalidStaff = Fixtures.staff("4444444", "abcd")
      val invalidNoone = "unknown"
      val invalidNoPermission = Fixtures.student("5555555")

      command.includedStudentIds.add(validStudentAlreadyIncluded.universityId)

      command.massAddUsers = "%s\n%s\n%s\n%s\n%s\n%s".format(
        validStudent.universityId,
        validStudentAlreadyIncluded.universityId,
        validStudentWithUsercode.userId,
        invalidStaff.universityId,
        invalidNoone,
        invalidNoPermission.universityId
      )


      command.profileService.getMemberByUniversityId(validStudent.universityId) returns Option(validStudent)
      command.profileService.getMemberByUniversityId(validStudentAlreadyIncluded.universityId) returns Option(validStudentAlreadyIncluded)
      command.profileService.getMemberByUniversityId(invalidStaff.universityId) returns Option(invalidStaff)
      command.profileService.getMemberByUniversityId(invalidNoPermission.universityId) returns Option(invalidNoPermission)
      command.userLookup.registerUserObjects(MemberOrUser(validStudentWithUsercode).asUser)
      command.profileService.getMemberByUser(MemberOrUser(validStudentWithUsercode).asUser) returns Option(validStudentWithUsercode)

      command.securityService.can(currentUser, Permissions.MonitoringPoints.Manage, validStudent) returns true
      command.securityService.can(currentUser, Permissions.MonitoringPoints.Manage, validStudentAlreadyIncluded) returns true
      command.securityService.can(currentUser, Permissions.MonitoringPoints.Manage, validStudentWithUsercode) returns true
      command.securityService.can(currentUser, Permissions.MonitoringPoints.Manage, invalidNoPermission) returns false

      val result = command.addUsers()
      result.missingMembers.size should be(2)
      result.missingMembers.contains(invalidNoone) should be (true)
      result.missingMembers.contains(invalidStaff.universityId) should be (true)
      result.noPermissionMembers.size should be(1)
      result.noPermissionMembers.head should be(invalidNoPermission)
      command.includedStudentIds.size should be(3)
      // TAB-2531 The same student shouldn't be added multiple times
      command.includedStudentIds.asScala.count(_ == validStudentAlreadyIncluded.universityId) should be (1)
      command.includedStudentIds.contains(validStudentAlreadyIncluded.universityId) should be (true)
      command.includedStudentIds.contains(validStudentWithUsercode.universityId) should be (true)
    }
  }

}
