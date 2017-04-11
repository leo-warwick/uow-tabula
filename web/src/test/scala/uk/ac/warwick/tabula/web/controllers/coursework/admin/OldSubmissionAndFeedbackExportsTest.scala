package uk.ac.warwick.tabula.web.controllers.coursework.admin

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.coursework.assignments.ListSubmissionsCommand.SubmissionListItem
import uk.ac.warwick.tabula.commands.coursework.assignments.SubmissionAndFeedbackCommand._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.userlookup.User

import scala.collection.immutable.ListMap

class OldSubmissionAndFeedbackExportsTest extends TestBase with Mockito {

	val assignment: Assignment = newDeepAssignment()
	assignment.id = "123"
	assignment.openDate = dateTime(2012, 4)
	assignment.closeDate = dateTime(2012, 6)

	val plagiarisedReport = new OriginalityReport
	plagiarisedReport.overlap = Some(97)
	val plagiarisedFile = new SavedFormValue
	plagiarisedFile.name = "upload"
	plagiarisedFile.attachments = JSet(
		{
			val f = new FileAttachment()
			f.name = "carol.docx"
			f.originalityReport = plagiarisedReport
			f
		}
	)

	val submission1 = new Submission()
	submission1.assignment = assignment
	submission1._universityId = "HAVILAND"
	submission1.id = "10001"
	submission1.submittedDate = dateTime(2012, 3)
	submission1.values = JSet(
		plagiarisedFile
	)
	plagiarisedFile.submission = submission1

	val student66: User = newUser("1234566")
	val student67: User = newUser("1234567")

	val items = Seq(

		Student(student66, null, None, ListMap(),
			WorkflowItems (
				student66,
				enhancedSubmission = Some(SubmissionListItem(submission1, downloaded=false)),
				enhancedFeedback = None,
				enhancedExtension = None
			), assignment, None
		),
		Student(student67, null, None, ListMap(),
			WorkflowItems (
				student67,
				enhancedSubmission = None,
				enhancedFeedback = None,
				enhancedExtension = None
			), assignment, None
		)
	)

	private def newUser(warwickId: String) = {
		val user = new User()
		user.setWarwickId(warwickId)
		user
	}

	@Test
	def xmlBuilder() {

		val builder = new XMLBuilder(items, assignment, assignment.module)
		builder.topLevelUrl = "https://example.com"

		val xml = builder.toXML

		assertEqual(xml,
		<assignment open-ended="false"
								open-date="2012-04-01T00:00:00+01:00"
								close-date="2012-06-01T00:00:00+01:00" id="123" module-code="IN101"
								submissions-zip-url={"https://example.com/coursework/admin/module/IN101/assignments/123/submissions.zip"}>
			<generic-feedback/>
			<students>
				<student university-id="1234566">
					<submission submitted="true" markable="false" within-extension="false" late="false" downloaded="false" time="2012-03-01T00:00:00Z" id="10001">
						<field name="upload">
							<file name="carol.docx" zip-path="IN101 - HAVILAND - carol.docx"/>
						</field>
					</submission>
					<marking suspected-plagiarised="false" similarity-percentage="97" />
					<feedback/>
					<adjustment/>
				</student>
				<student university-id="1234567">
					<submission submitted="false" late="true"></submission>
					<marking/>
					<feedback/>
					<adjustment/>
				</student>
			</students>
		</assignment>
		)

	}

	/** Check that the XMLs are the same, ignoring empty text nodes (whitespace). */
	private def assertEqual(actual: xml.Node, expected: xml.Node) {

		def recurse(actual: xml.Node, expected: xml.Node) {
			// depth-first checks, to get specific failures
			for ((actualChild, expectedChild) <- actual.child zip expected.child) {
				recurse(actualChild, expectedChild)
			}
			actual should be (expected)
		}

		val trim = scala.xml.Utility.trim _
		recurse(trim(actual), trim(expected))

	}
}
