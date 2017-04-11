package uk.ac.warwick.tabula.profiles.profile

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest
import uk.ac.warwick.tabula.web.FeaturesDriver

class MemberNotesTest extends BrowserTest with GivenWhenThen with FeaturesDriver with StudentProfileFixture {

	"A student" should "be able to view their member notes" in {

		createMemberNote(P.Student1.warwickId, P.Admin1.warwickId, "This is a member note", "Member note title")

		Given("The profilesMemberNotes feature is enabled")
		enableFeature("profilesMemberNotes")

		When("Student1 views their profile")
		signIn as P.Student1 to Path("/profiles")
		currentUrl should endWith (s"/profiles/view/${P.Student1.warwickId}")

		val memberNoteSection: Option[Element] = find(cssSelector("section.member-notes"))
		memberNoteSection should be ('defined)
		cssSelector("section.member-notes table tbody tr").findAllElements.size should be (1)
		cssSelector("section.member-notes a[title='Edit note']").findAllElements.size should be (0)

	}

	"An admin" should "be able to delete but not view a member note they didn't create" in {

		createMemberNote(P.Student1.warwickId, P.Admin1.warwickId, "This is a member note", "Member note title")

		Given("The profilesMemberNotes feature is enabled")
		enableFeature("profilesMemberNotes")

		And("Admin2 is an admin of the student")
		createStaffMember(P.Admin2.usercode, deptCode = TEST_DEPARTMENT_CODE)

		When("Admin2 views the profile of Student1")
		signIn as P.Admin2 to Path(s"/profiles/view/${P.Student1.warwickId}")
		currentUrl should endWith (s"/profiles/view/${P.Student1.warwickId}")

		Then("They see the member note")
		val memberNoteSection: Option[Element] = find(cssSelector("section.member-notes"))
		memberNoteSection should be ('defined)
		cssSelector("section.member-notes table tbody tr").findAllElements.size should be (1)

		And("Can delete it")
		cssSelector("section.member-notes a.delete").findAllElements.size should be (1)
		And("But can't edit it")
		cssSelector("section.member-notes a[title='Edit note']").findAllElements.size should be (0)

	}

	"A note creator" should "be able to edit and delete a member note" in {

		createMemberNote(P.Student1.warwickId, P.Admin2.warwickId, "This is a member note", "Member note title")

		Given("The profilesMemberNotes feature is enabled")
		enableFeature("profilesMemberNotes")

		And("Admin2 is an admin of the student")
		createStaffMember(P.Admin2.usercode, deptCode = TEST_DEPARTMENT_CODE)

		When("Admin2 views the profile of Student1")
		signIn as P.Admin2 to Path(s"/profiles/view/${P.Student1.warwickId}")
		currentUrl should endWith (s"/profiles/view/${P.Student1.warwickId}")

		Then("They see the member note")
		val memberNoteSection: Option[Element] = find(cssSelector("section.member-notes"))
		memberNoteSection should be ('defined)
		cssSelector("section.member-notes table tbody tr").findAllElements.size should be (1)

		And("Can delete it")
		cssSelector("section.member-notes a.delete").findAllElements.size should be (1)
		And("Can edit it")
		cssSelector("section.member-notes a[title='Edit note']").findAllElements.size should be (1)

	}

}
