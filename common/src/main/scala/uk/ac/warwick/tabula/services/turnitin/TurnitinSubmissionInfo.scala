package uk.ac.warwick.tabula.services.turnitin

import uk.ac.warwick.tabula.data.model.FileAttachment

/*
 * Typical <object> response when listing submissions
 *
   <object>
		<userid>-1</userid>
		<firstname>0123001</firstname>
		<lastname>Abcdef</lastname>
		<title>file.doc</title>
		<objectID>17619079</objectID>
		<date_submitted>2012-06-18 13:57:37+0100</date_submitted>
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
 *
 */

/**
 * Information about a submission as returned by the Turnitin API.
 */
case class TurnitinSubmissionInfo(
	val objectId: DocumentId,
	val title: String, // paper title
	val universityId: String, // stored under student's first name in Turnitin
	val similarityScore: Int,
	val overlap: Option[Int],
	val webOverlap: Option[Int],
	val publicationOverlap: Option[Int],
	val studentPaperOverlap: Option[Int]) {
	/** If plagiarism checking hasn't been done yet, it will have a score of -1. */
	def hasBeenChecked: Boolean = similarityScore != -1

	/** Whether this appears to match up with the given FileAttachment according to our naming conventions,
	 * which is to extract the ID from the paper title and compare that against the FileAttachment ID.
	 */
	def matches(attachment: FileAttachment): Boolean = title == attachment.id
}