package uk.ac.warwick.tabula.services.turnitin

import uk.ac.warwick.tabula.TestBase

import scala.xml.XML

class TurnitinResponseTest extends TestBase {

	@Test def submissionsList() = {
		val response = TurnitinResponse.fromXml( XML.loadString(PartlyScoredSubmissions) )
		response.submissionsList.size should be (10)
		response.submissionsList.count(_.hasBeenChecked) should be (4)
	}

	val PartlyScoredSubmissions = """

<returndata>
	<object>
		<userid>-1</userid>
		<firstname>0123001</firstname>
		<lastname>Student</lastname>
		<title>file.doc</title>
		<objectID>17619584</objectID>
		<date_submitted>2012-06-18 14:42:10+0100</date_submitted>
		<similarityScore>4</similarityScore>
		<overlap>100</overlap>
		<web_overlap>94</web_overlap>
		<publication_overlap>0</publication_overlap>
		<student_paper_overlap>100</student_paper_overlap>
		<score></score>
		<anon>0</anon>
		<gradeMarkStatus>1</gradeMarkStatus>
		<markup_exists>0</markup_exists>
		<student_responses>
		</student_responses>
	</object>
	<object>
		<userid>-1</userid>
		<firstname>0123002</firstname>
		<lastname>Student</lastname>
		<title>file.doc</title>
		<objectID>17619585</objectID>
		<date_submitted>2012-06-18 14:42:13+0100</date_submitted>
		<similarityScore>4</similarityScore>
		<overlap>100</overlap>
		<web_overlap>94</web_overlap>
		<publication_overlap>0</publication_overlap>
		<student_paper_overlap>100</student_paper_overlap>
		<score></score>
		<anon>0</anon>
		<gradeMarkStatus>1</gradeMarkStatus>
		<markup_exists>0</markup_exists>
		<student_responses>
		</student_responses>
	</object>
	<object>
		<userid>-1</userid>
		<firstname>0123003</firstname>
		<lastname>Student</lastname>
		<title>file.doc</title>
		<objectID>17619586</objectID>
		<date_submitted>2012-06-18 14:42:15+0100</date_submitted>
		<similarityScore>4</similarityScore>
		<overlap>100</overlap>
		<web_overlap>94</web_overlap>
		<publication_overlap>0</publication_overlap>
		<student_paper_overlap>100</student_paper_overlap>
		<score></score>
		<anon>0</anon>
		<gradeMarkStatus>1</gradeMarkStatus>
		<markup_exists>0</markup_exists>
		<student_responses>
		</student_responses>
	</object>
	<object>
		<userid>-1</userid>
		<firstname>0123004</firstname>
		<lastname>Student</lastname>
		<title>file.doc</title>
		<objectID>17619587</objectID>
		<date_submitted>2012-06-18 14:42:17+0100</date_submitted>
		<similarityScore>4</similarityScore>
		<overlap>100</overlap>
		<web_overlap>94</web_overlap>
		<publication_overlap>0</publication_overlap>
		<student_paper_overlap>100</student_paper_overlap>
		<score></score>
		<anon>0</anon>
		<gradeMarkStatus>1</gradeMarkStatus>
		<markup_exists>0</markup_exists>
		<student_responses>
		</student_responses>
	</object>
	<object>
		<userid>-1</userid>
		<firstname>0123005</firstname>
		<lastname>Student</lastname>
		<title>file.doc</title>
		<objectID>17619588</objectID>
		<date_submitted>2012-06-18 14:42:20+0100</date_submitted>
		<similarityScore>-1</similarityScore>
		<overlap>0</overlap>
		<web_overlap></web_overlap>
		<publication_overlap></publication_overlap>
		<student_paper_overlap></student_paper_overlap>
		<score></score>
		<anon>0</anon>
		<gradeMarkStatus>1</gradeMarkStatus>
		<markup_exists>0</markup_exists>
		<student_responses>
		</student_responses>
	</object>
	<object>
		<userid>-1</userid>
		<firstname>0123006</firstname>
		<lastname>Student</lastname>
		<title>file.doc</title>
		<objectID>17619590</objectID>
		<date_submitted>2012-06-18 14:42:23+0100</date_submitted>
		<similarityScore>-1</similarityScore>
		<overlap>0</overlap>
		<web_overlap></web_overlap>
		<publication_overlap></publication_overlap>
		<student_paper_overlap></student_paper_overlap>
		<score></score>
		<anon>0</anon>
		<gradeMarkStatus>1</gradeMarkStatus>
		<markup_exists>0</markup_exists>
		<student_responses>
		</student_responses>
	</object>
	<object>
		<userid>-1</userid>
		<firstname>0123007</firstname>
		<lastname>Student</lastname>
		<title>file.doc</title>
		<objectID>17619591</objectID>
		<date_submitted>2012-06-18 14:42:25+0100</date_submitted>
		<similarityScore>-1</similarityScore>
		<overlap>0</overlap>
		<web_overlap></web_overlap>
		<publication_overlap></publication_overlap>
		<student_paper_overlap></student_paper_overlap>
		<score></score>
		<anon>0</anon>
		<gradeMarkStatus>1</gradeMarkStatus>
		<markup_exists>0</markup_exists>
		<student_responses>
		</student_responses>
	</object>
	<object>
		<userid>-1</userid>
		<firstname>0123008</firstname>
		<lastname>Student</lastname>
		<title>file.doc</title>
		<objectID>17619593</objectID>
		<date_submitted>2012-06-18 14:42:27+0100</date_submitted>
		<similarityScore>-1</similarityScore>
		<overlap>0</overlap>
		<web_overlap></web_overlap>
		<publication_overlap></publication_overlap>
		<student_paper_overlap></student_paper_overlap>
		<score></score>
		<anon>0</anon>
		<gradeMarkStatus>1</gradeMarkStatus>
		<markup_exists>0</markup_exists>
		<student_responses>
		</student_responses>
	</object>
	<object>
		<userid>-1</userid>
		<firstname>0123009</firstname>
		<lastname>Student</lastname>
		<title>file.doc</title>
		<objectID>17619594</objectID>
		<date_submitted>2012-06-18 14:42:29+0100</date_submitted>
		<similarityScore>-1</similarityScore>
		<overlap>0</overlap>
		<web_overlap></web_overlap>
		<publication_overlap></publication_overlap>
		<student_paper_overlap></student_paper_overlap>
		<score></score>
		<anon>0</anon>
		<gradeMarkStatus>1</gradeMarkStatus>
		<markup_exists>0</markup_exists>
		<student_responses>
		</student_responses>
	</object>
	<object>
		<userid>-1</userid>
		<firstname>0123010</firstname>
		<lastname>Student</lastname>
		<title>file.doc</title>
		<objectID>17619595</objectID>
		<date_submitted>2012-06-18 14:42:31+0100</date_submitted>
		<similarityScore>-1</similarityScore>
		<overlap>0</overlap>
		<web_overlap></web_overlap>
		<publication_overlap></publication_overlap>
		<student_paper_overlap></student_paper_overlap>
		<score></score>
		<anon>0</anon>
		<gradeMarkStatus>0</gradeMarkStatus>
		<markup_exists>0</markup_exists>
		<student_responses>
		</student_responses>
	</object>
	<rcode>72</rcode>
	<rmessage>Successful!</rmessage>
</returndata>

	"""
}

