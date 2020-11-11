package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.data.model.Department._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod.{Manual, StudentSignUp}
import uk.ac.warwick.tabula.data.model.permissions.CustomRoleDefinition
import uk.ac.warwick.tabula.helpers.Tap.tap
import uk.ac.warwick.tabula.permissions.PermissionsSelector
import uk.ac.warwick.tabula.roles.{DepartmentalAdministratorRoleDefinition, ExtensionManagerRoleDefinition, StudentRelationshipAgentRoleDefinition}
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.{Fixtures, AcademicYear, Mockito, TestBase}

class DepartmentTest extends TestBase with Mockito {

  val permissionsService: PermissionsService = mock[PermissionsService]

  @Test def settings(): Unit = {
    val department = Fixtures.department("in")
    department.collectFeedbackRatings should be (false)
    department.allowExtensionRequests should be (false)
    department.canRequestExtension should be (false)
    department.extensionGuidelineSummary should be(null)
    department.formattedGuidelineSummary should be("")
    department.extensionGuidelineLink should be(null)
    department.showStudentName should be (false)
    department.plagiarismDetectionEnabled should be (true)
    department.defaultGroupAllocationMethod should be(Manual)
    department.autoGroupDeregistration should be (true)

    department.collectFeedbackRatings = true
    department.allowExtensionRequests = true
    department.extensionGuidelineSummary = "Here is my magic summary.\n\nDo everything good!"
    department.extensionGuidelineLink = "http://warwick.ac.uk"
    department.showStudentName = true
    department.plagiarismDetectionEnabled = false
    department.defaultGroupAllocationMethod = StudentSignUp
    department.autoGroupDeregistration = false

    department.collectFeedbackRatings should be (true)
    department.allowExtensionRequests should be (true)
    department.canRequestExtension should be (true)
    department.extensionGuidelineSummary should be("Here is my magic summary.\n\nDo everything good!")
    department.formattedGuidelineSummary should be("<p>\n    Here is my magic summary.\n  </p><p>\n    Do everything good!\n  </p>")
    department.extensionGuidelineLink should be("http://warwick.ac.uk")
    department.showStudentName should be (true)
    department.plagiarismDetectionEnabled should be (false)
    department.defaultGroupAllocationMethod should be(StudentSignUp)
    department.autoGroupDeregistration should be (false)
    department.autoMarkMissedMonitoringPoints should be (false)

  }

  @Test def groups(): Unit = {
    val department = Fixtures.department("in")
    department.permissionsService = permissionsService

    val ownersGroup = UserGroup.ofUsercodes
    val extmanGroup = UserGroup.ofUsercodes

    permissionsService.ensureUserGroupFor(department, DepartmentalAdministratorRoleDefinition) returns ownersGroup
    permissionsService.ensureUserGroupFor(department, ExtensionManagerRoleDefinition) returns extmanGroup

    department.isOwnedBy("cuscav") should be (false)

    department.owners.knownType.addUserId("cuscav")
    department.owners.knownType.addUserId("cusebr")
    department.owners.knownType.addUserId("curef")

    department.owners.knownType.removeUserId("cusebr")

    department.isOwnedBy("cuscav") should be (true)
    department.isOwnedBy("curef") should be (true)
    department.isOwnedBy("cusebr") should be (false)

    ownersGroup.members should be(Set("cuscav", "curef"))

    department.isExtensionManager("cuscav") should be (false)
    extmanGroup.addUserId("cuscav")
    department.isExtensionManager("cuscav") should be (true)
  }

  @Test
  def filterRuleDefaultsToAll(): Unit = {
    val department = Fixtures.department("in")
    department.filterRule should be(AllMembersFilterRule)
  }

  private trait FilterRuleFixture {
    val department = Fixtures.department("its1")
    val otherDepartment = Fixtures.department("its2")
    val ugRoute: Route = new Route().tap(r => {
      r.degreeType = DegreeType.Undergraduate
      r.adminDepartment = department
    })
    val pgRoute: Route = new Route().tap(r => {
      r.degreeType = DegreeType.Postgraduate
      r.adminDepartment = otherDepartment
    })
    val undergraduate: StudentMember = new StudentMember().tap(m => {
      val scd = new StudentCourseDetails().tap(s => {
        s.mostSignificant = true
        s.attachStudentCourseYearDetails(new StudentCourseYearDetails().tap(_.yearOfStudy = 1))
        s.currentRoute = ugRoute
      })
      m.attachStudentCourseDetails(scd)
      m.mostSignificantCourse = scd
    })
    val postgraduate: StudentMember = new StudentMember().tap(m => {
      val scd = new StudentCourseDetails().tap(s => {
        s.mostSignificant = true
        s.attachStudentCourseYearDetails(new StudentCourseYearDetails().tap(_.yearOfStudy = 7))
        s.currentRoute = pgRoute
      })
      m.attachStudentCourseDetails(scd)
      m.mostSignificantCourse = scd
    })

    val notStudentMember = new StaffMember()
  }

  @Test
  def AllMembersFilterRuleLetsAnyoneIn(): Unit = {
    new FilterRuleFixture {
      val rule = AllMembersFilterRule
      rule.matches(notStudentMember, None, None) should be (true)
      rule.matches(undergraduate, None, None) should be (true)
      rule.matches(postgraduate, None, None) should be (true)
    }
  }

  /**
    * The undergraduate / postgraduate filter rules use the Course Type enum
    * from the StudentCourseDetails to determine degree type.
    */
  @Test
  def UGFilterRuleAllowsUndergrads(): Unit = {
    new FilterRuleFixture {
      val rule = UndergraduateFilterRule
      rule.matches(notStudentMember, None, None) should be (false)
      rule.matches(undergraduate, None, None) should be (true)
      rule.matches(postgraduate, None, None) should be (false)

      // test the various remaining different route types
      postgraduate.mostSignificantCourseDetails.get.currentRoute.degreeType = DegreeType.InService
      rule.matches(postgraduate, None, None) should be (false)

      postgraduate.mostSignificantCourseDetails.get.currentRoute.degreeType = DegreeType.PGCE
      rule.matches(postgraduate, None, None) should be (false)
    }
  }

  @Test
  def PGFilterRuleAllowsPostgrads(): Unit = {
    new FilterRuleFixture {
      val rule = PostgraduateFilterRule
      rule.matches(notStudentMember, None, None) should be (false)
      rule.matches(undergraduate, None, None) should be (false)
      rule.matches(postgraduate, None, None) should be (true)

      // test the various remaining different course types
      postgraduate.mostSignificantCourseDetails.get.currentRoute.degreeType = DegreeType.InService
      rule.matches(postgraduate, None, None) should be (true)

      postgraduate.mostSignificantCourseDetails.get.currentRoute.degreeType = DegreeType.PGCE
      rule.matches(postgraduate, None, None) should be (true)

    }
  }

  @Test
  def YearOfStudyRuleAllowsMatchingYear(): Unit = {
    new FilterRuleFixture {
      val firstYearRule = new InYearFilterRule(1)
      val secondYearRule = new InYearFilterRule(2)
      firstYearRule.matches(undergraduate, None, None) should be (true)
      firstYearRule.matches(postgraduate, None, None) should be (false)
      firstYearRule.matches(notStudentMember, None, None) should be (false)

      secondYearRule.matches(undergraduate, None, None) should be (false)
      undergraduate.mostSignificantCourseDetails.get.latestStudentCourseYearDetails.yearOfStudy = 2
      secondYearRule.matches(undergraduate, None, None) should be (true)
      firstYearRule.matches(undergraduate, None, None) should be (false)
    }
  }

  @Test
  def DepartmentRoutesRuleAllowsStudentInRoute(): Unit = {
    new FilterRuleFixture {
      val deptRoutesRule = DepartmentRoutesFilterRule
      deptRoutesRule.matches(undergraduate, Option(department), None) should be (true)
      deptRoutesRule.matches(postgraduate, Option(department), None) should be (false)
      deptRoutesRule.matches(notStudentMember, Option(department), None) should be (false)

      deptRoutesRule.matches(undergraduate, Option(otherDepartment), None) should be (false)
      deptRoutesRule.matches(postgraduate, Option(otherDepartment), None) should be (true)
      deptRoutesRule.matches(notStudentMember, Option(otherDepartment), None) should be (false)

    }
  }

  @Test
  def replacedRoleDefinitionFor(): Unit = {
    val department = Fixtures.department("in")

    val roleDefinition = DepartmentalAdministratorRoleDefinition

    department.replacedRoleDefinitionFor(roleDefinition) should be(Symbol("empty"))

    val customDefinition = new CustomRoleDefinition
    customDefinition.baseRoleDefinition = roleDefinition

    department.customRoleDefinitions.add(customDefinition)

    customDefinition.replacesBaseDefinition = false

    department.replacedRoleDefinitionFor(roleDefinition) should be(Symbol("empty"))

    customDefinition.replacesBaseDefinition = true

    department.replacedRoleDefinitionFor(roleDefinition) should be(Some(customDefinition))
  }

  @Test
  def replacedRoleDefinitionForSelector(): Unit = {
    val department = Fixtures.department("in")

    val selector = new StudentRelationshipType

    val customDefinition = new CustomRoleDefinition
    customDefinition.baseRoleDefinition = StudentRelationshipAgentRoleDefinition(selector)

    assert(StudentRelationshipAgentRoleDefinition(selector) === StudentRelationshipAgentRoleDefinition(selector))

    department.customRoleDefinitions.add(customDefinition)

    customDefinition.replacesBaseDefinition = true

    department.replacedRoleDefinitionFor(StudentRelationshipAgentRoleDefinition(selector)) should be(Some(customDefinition))
    department.replacedRoleDefinitionFor(StudentRelationshipAgentRoleDefinition(new StudentRelationshipType)) should be(Symbol("empty"))
    department.replacedRoleDefinitionFor(StudentRelationshipAgentRoleDefinition(PermissionsSelector.Any)) should be(Symbol("empty"))

    customDefinition.baseRoleDefinition = StudentRelationshipAgentRoleDefinition(PermissionsSelector.Any)

    department.replacedRoleDefinitionFor(StudentRelationshipAgentRoleDefinition(selector)) should be(Some(customDefinition))
    department.replacedRoleDefinitionFor(StudentRelationshipAgentRoleDefinition(new StudentRelationshipType)) should be(Some(customDefinition))
    department.replacedRoleDefinitionFor(StudentRelationshipAgentRoleDefinition(PermissionsSelector.Any)) should be(Some(customDefinition))
  }

  @Test
  def testUploadMarksSettings(): Unit = {
    val department = Fixtures.department("in")
    val year = AcademicYear(2014)
    var degreeType = DegreeType.Undergraduate

    val module = Fixtures.module("AM903", "Test module")
    module.degreeType = degreeType

    // no department settings created - marks upload is open by default
    department.canUploadMarksToSitsForYear(year, module) should be(true)

    // set to false and make sure it can be read back
    department.setUploadMarksToSitsForYear(year, degreeType, false)
    department.canUploadMarksToSitsForYear(year, module) should be(false)

    // now set to true and test again
    department.setUploadMarksToSitsForYear(year, degreeType, true)
    department.canUploadMarksToSitsForYear(year, module) should be(true)

    // make PG false and see if we can still load UG
    department.setUploadMarksToSitsForYear(year, DegreeType.Postgraduate, false)
    department.canUploadMarksToSitsForYear(year, module) should be(true)

    // now make the module PG - upload should fail
    module.degreeType = DegreeType.Postgraduate
    department.canUploadMarksToSitsForYear(year, module) should be(false)

    // set pg to be uploadable so it passes
    department.setUploadMarksToSitsForYear(year, DegreeType.Postgraduate, true)
    department.canUploadMarksToSitsForYear(year, module) should be(true)

  }

}
