package uk.ac.warwick.tabula.groups

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.groups.pages.GroupsHomePage

class SelfSignUpTest extends SmallGroupsFixture with GivenWhenThen {

  val TEST_MODULE_CODE = "xxx999"
  val TEST_GROUPSET_NAME = "Test Tutorial"

  "A student" should "be able to sign up for a group" in {
    Given("A small groupset exists with 2 small groups and an allocation method of StudentSignUp")
    createModule("xxx", TEST_MODULE_CODE, "Self Sign Up Module")
    val setId = createSmallGroupSet(
      TEST_MODULE_CODE,
      TEST_GROUPSET_NAME,
      allocationMethodName = "StudentSignUp",
      groupCount = 2,
      academicYear = academicYearString
    )

    And("The student is a member of the groupset")
    addStudentToGroupSet(P.Student1.usercode, setId)

    When("I log in as the student and visit the groups home page")
    signIn as P.Student1 to Path("/groups/")
    // Ensure correct academic year
    click on linkText(academicYear.toString)

    Then("I should see the groupset listed with a radio button beside each group")
    val groupsPage = new GroupsHomePage
    groupsPage should be(Symbol("currentPage"))

    val groupsetInfo = groupsPage.getGroupsetInfo(TEST_MODULE_CODE, TEST_GROUPSET_NAME).get

    val group1Checkbox = groupsetInfo.findSelectGroupCheckboxFor("Group 1")
    group1Checkbox.isEnabled should be(true)

    val group2Checkbox = groupsetInfo.findSelectGroupCheckboxFor("Group 2")
    group2Checkbox.isEnabled should be(true)

    And("The 'sign up' button should be disabled")
    groupsetInfo.getSignupButton.isEnabled should not be true

    When("I select one of the groups")
    group1Checkbox.click()

    Then("The 'sign up' link button becomes enabled")
    groupsetInfo.getSignupButton.isEnabled should be(true)

    When("I click the sign up button")
    groupsetInfo.getSignupButton.submit()

    eventually {
      pageSource should include("1 student")
    }

    Then("The groups home page should be displayed")
    val updatedGroupsPage = new GroupsHomePage
    updatedGroupsPage should be(Symbol("currentPage"))

    And("The group I selected should be displayed")
    val updatedGroupsetInfo = updatedGroupsPage.getGroupsetInfo(TEST_MODULE_CODE, TEST_GROUPSET_NAME).get
    updatedGroupsetInfo.showsGroup("Group 1") should be (true)
    updatedGroupsetInfo.showsGroup("Group 2") should be (false)

  }

  "A student" should "be able to leave a group they signed up for" in {
    Given("A small groupset exists with 2 small groups and an allocation method of StudentSignUp")
    createModule("xxx", TEST_MODULE_CODE, "Self Sign Up Module")
    val setId = createSmallGroupSet(TEST_MODULE_CODE, TEST_GROUPSET_NAME, allocationMethodName = "StudentSignUp", groupCount = 2, academicYear = "2014")

    And("The student is a member of the groupset")
    addStudentToGroupSet(P.Student1.usercode, setId)

    And("The student is allocated to group 1")
    addStudentToGroup(P.Student1.usercode, setId, "Group 1")

    When("I Log in as the student and view the groups page")
    signIn as P.Student1 to Path("/groups/")
    // Ensure correct academic year
    click on linkText("14/15")

    val groupsPage = new GroupsHomePage

    go to groupsPage.url
    val groupInfo = groupsPage.getGroupsetInfo(TEST_MODULE_CODE, TEST_GROUPSET_NAME).get

    Then("The 'leave' button for group 1 should be enabled")
    val leaveButton = groupInfo.findLeaveButtonFor("Group 1").get
    leaveButton.isEnabled should be(true)

    When("I click the 'leave' button")
    leaveButton.submit()

    Then("The groups home page should be displayed")
    val updatedGroupsPage = new GroupsHomePage
    updatedGroupsPage should be(Symbol("currentPage"))

    And("Both groups should be displayed, with radio buttons for selection")
    val groupsetInfo = groupsPage.getGroupsetInfo(TEST_MODULE_CODE, TEST_GROUPSET_NAME)
      .getOrElse(fail(s"No group set info found for $TEST_MODULE_CODE - $TEST_GROUPSET_NAME"))

    val group1Checkbox = groupsetInfo.findSelectGroupCheckboxFor("Group 1")
    group1Checkbox.isEnabled should be(true)

    val group2Checkbox = groupsetInfo.findSelectGroupCheckboxFor("Group 2")
    group2Checkbox.isEnabled should be(true)
  }

  "A student" should "not see a non-self-sign-up groupset for which they have not been allocated a group" in {
    Given("A small groupset exists with 2 small groups and an allocation method of Manual")
    createModule("xxx", TEST_MODULE_CODE, "Non Self Sign Up Module")
    val setId = createSmallGroupSet(
      TEST_MODULE_CODE,
      TEST_GROUPSET_NAME,
      allocationMethodName = "Manual",
      groupCount = 2,
      academicYear = academicYearString
    )

    And("The student is a member of the groupset") // but is not allocated to any group!
    addStudentToGroupSet(P.Student1.usercode, setId)


    When("I log in as the student and visit the groups home page")
    signIn as P.Student1 to Path("/groups/")
    // Ensure correct academic year
    click on linkText(academicYear.toString)

    Then("The groupset is not displayed")
    val groupsPage = new GroupsHomePage
    val groupInfo = groupsPage.getGroupsetInfo(TEST_MODULE_CODE, TEST_GROUPSET_NAME)
    groupInfo should not be Symbol("defined")

  }

  "A student" should "not be able to leave a non-self-sign-up group" in {
    Given("A small groupset exists with 2 small groups and an allocation method of Manual")
    createModule("xxx", TEST_MODULE_CODE, "Non Self Sign Up Module")
    val setId = createSmallGroupSet(TEST_MODULE_CODE, TEST_GROUPSET_NAME, allocationMethodName = "Manual", groupCount = 2, academicYear = "2014")

    And("The student is a member of the groupset")
    addStudentToGroupSet(P.Student1.usercode, setId)

    And("The student is allocated to group 1")
    addStudentToGroup(P.Student1.usercode, setId, "Group 1")

    When("I log in as the student and visit the groups home page")
    signIn as P.Student1 to Path("/groups/")
    // Ensure correct academic year
    click on linkText("14/15")

    Then("The groupset is displayed, and group 1's information is present")
    val groupsPage = new GroupsHomePage
    val groupInfo = groupsPage.getGroupsetInfo(TEST_MODULE_CODE, TEST_GROUPSET_NAME).get

    And("The leave button is not shown ")
    groupInfo.findLeaveButtonFor("Group 1") should not be Symbol("defined")

    And("The group locked icon is shown")
    groupInfo.showsGroupLockedIcon() should be (true)

  }

  "A student" should "not see a self-sign-up groupset which is not open if they aren't allocated" in {
    Given("A small groupset exists which is not open for signup, with 2 small groups and an allocation method of StudentSignUp")
    createModule("xxx", TEST_MODULE_CODE, "Closed Self Sign Up Module")
    val setId = createSmallGroupSet(
      TEST_MODULE_CODE,
      TEST_GROUPSET_NAME,
      allocationMethodName = "StudentSignUp",
      groupCount = 2,
      openForSignups = false,
      academicYear = academicYearString
    )
    And("The student is a member of the groupset")
    addStudentToGroupSet(P.Student1.usercode, setId)

    When("I log in as the student and visit the groups home page")
    signIn as P.Student1 to Path("/groups/")
    // Ensure correct academic year
    click on linkText(academicYear.toString)

    Then("The groupset is not displayed")
    val groupsPage = new GroupsHomePage
    val groupInfo = groupsPage.getGroupsetInfo(TEST_MODULE_CODE, TEST_GROUPSET_NAME)
    groupInfo should not be Symbol("defined")
  }

  "A student" should "see a self-sign-up groupset which is not open if they are allocated to a group" in {
    Given("A small groupset exists which is not open for signup, with 2 small groups and an allocation method of StudentSignUp")
    createModule("xxx", TEST_MODULE_CODE, "Closed Self Sign Up Module with allocation")
    val setId = createSmallGroupSet(TEST_MODULE_CODE, TEST_GROUPSET_NAME, allocationMethodName = "StudentSignUp", groupCount = 2, openForSignups = false, academicYear = "2014")

    And("The student is a member of the groupset")
    addStudentToGroupSet(P.Student1.usercode, setId)

    And("The student is allocated to group 1")
    addStudentToGroup(P.Student1.usercode, setId, "Group 1")

    When("I log in as the student and visit the groups home page")
    signIn as P.Student1 to Path("/groups/")
    // Ensure correct academic year
    click on linkText("14/15")

    Then("The groupset is displayed, and group 1's information is present")
    val groupsPage = new GroupsHomePage
    val groupInfo = groupsPage.getGroupsetInfo(TEST_MODULE_CODE, TEST_GROUPSET_NAME).get

    And("The leave button is not shown ")
    groupInfo.findLeaveButtonFor("Group 1") should not be Symbol("defined")

    And("The group locked icon is shown")
    groupInfo.showsGroupLockedIcon() should be (true)
  }

  "A student" should "not be able to select a group which is full" in {
    Given("A small groupset exists with a group size of 1, with 2 small groups and an allocation method of StudentSignUp")
    createModule("xxx", TEST_MODULE_CODE, "Full group test")
    val setId = createSmallGroupSet(
      TEST_MODULE_CODE,
      TEST_GROUPSET_NAME,
      allocationMethodName = "StudentSignUp",
      groupCount = 2,
      maxGroupSize = 1,
      academicYear = academicYearString
    )
    And("The student is a member of the groupset")
    addStudentToGroupSet(P.Student1.usercode, setId)
    And("Another student is a member of the groupset and allocated to group1")
    addStudentToGroupSet(P.Student2.usercode, setId)
    addStudentToGroup(P.Student2.usercode, setId, "Group 1")


    When("I log in as the student and visit the groups home page")
    signIn as P.Student1 to Path("/groups/")
    // Ensure correct academic year
    click on linkText(academicYear.toString)

    Then("I should see the groupset listed with a radio button beside each group")
    val groupsPage = new GroupsHomePage
    groupsPage should be(Symbol("currentPage"))

    val groupsetInfo = groupsPage.getGroupsetInfo(TEST_MODULE_CODE, TEST_GROUPSET_NAME).get

    And("Group 1's checkbox should be disabled")
    val group1Checkbox = groupsetInfo.findSelectGroupCheckboxFor("Group 1")
    group1Checkbox.isEnabled should not be true

    And("Group 2's checkbox should be enabled")
    val group2Checkbox = groupsetInfo.findSelectGroupCheckboxFor("Group 2")
    group2Checkbox.isEnabled should be(true)

    And("The group locked icon is not shown")
    groupsetInfo.showsGroupLockedIcon() should be (false)
  }

  "A student" should "not be able to leave a self-signup group which doesn't allow switching" in {
    Given("A small groupset exists with 2 small groups and an allocation method of Manual and allowSwitching is false")
    createModule("xxx", TEST_MODULE_CODE, "No Switching Module")
    val setId = createSmallGroupSet(TEST_MODULE_CODE, TEST_GROUPSET_NAME, allocationMethodName = "StudentSignUp", groupCount = 2, allowSelfGroupSwitching = false, academicYear = "2014")

    And("The student is a member of the groupset")
    addStudentToGroupSet(P.Student1.usercode, setId)

    And("The student is allocated to group 1")
    addStudentToGroup(P.Student1.usercode, setId, "Group 1")

    When("I log in as the student and visit the groups home page")
    signIn as P.Student1 to Path("/groups/")
    // Ensure correct academic year
    click on linkText("14/15")

    Then("The groupset is displayed, and group 1's information is present")
    val groupsPage = new GroupsHomePage
    val groupInfo = groupsPage.getGroupsetInfo(TEST_MODULE_CODE, TEST_GROUPSET_NAME).get

    And("The leave button is not shown ")
    groupInfo.findLeaveButtonFor("Group 1") should not be Symbol("defined")

    And("The group locked icon is shown")
    groupInfo.showsGroupLockedIcon() should be (true)
  }

}
