package uk.ac.warwick.tabula.commands.coursework.departments

import java.util.Date

import scala.collection.JavaConverters._
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.xssf.usermodel.XSSFRow
import org.joda.time.base.AbstractInstant
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.services.coursework.feedbackreport.FeedbackReport

// scalastyle:off
class FeedbackReportTest extends TestBase with ReportWorld {
	import FeedbackReport._

	@Test
	def simpleGetFeedbackTest() {
		val publishes = auditEventQueryMethods.publishFeedbackForStudent(assignmentOne, "u"+idFormat(1), None)
		publishes.futureValue.size should be (1)
	}

	@Test
	def feedbackCountsTest() {
		val report = getTestFeedbackReport

		//For all below assignment submissions late submissions are excluded from 20 day calculation rule and treated on time in the report (similar to what
		//report does for dissertation)

		/*assignmentOne has 10 submissions and among those 10 submissions 2 submitted late but those 2 will be excuded from late feedback check and treated as on time in
		FeedbackCount (Rule simiar to dissertation -true for all late submissions) */
		report.getFeedbackCount(assignmentOne) should be (FeedbackCount(10,0, dateTime(2013, 3, 25), dateTime(2013, 3, 25))) // 10 on time

		//assignmentTwo has 29 submissions and among those 29 submissions 5 submitted late. Feedback provided late for all of submissions but 5 late submissions excluded from late check
		report.getFeedbackCount(assignmentTwo) should be (FeedbackCount(5,24, dateTime(2013, 5, 15), dateTime(2013, 5, 15))) // all feedbacks late

		//assignmentThree has 13 submissions and among those 13 submissions 2 submitted late(excluded from late check). Feedback provided on time for 4 submissions.
		report.getFeedbackCount(assignmentThree) should be (FeedbackCount(6,7, dateTime(2013, 5, 20), dateTime(2013, 6, 14))) // 4 on time - 9 late

		//assignmentFour has 35 submissions and among those 35 submissions 7 submitted late.Late feedback for all remaining 28 submissions
		report.getFeedbackCount(assignmentFour) should be (FeedbackCount(7,28, dateTime(2013, 6, 28), dateTime(2013, 6, 28))) // 7 on time - 28 late

		//assignmentFive has 100 submissions and among those 100 submissions 2 submitted late. Feedback late for 98 submissions
		report.getFeedbackCount(assignmentFive) should be (FeedbackCount(2,98, dateTime(2013, 9, 23), dateTime(2013, 9, 23))) // 2 on time - 98 late

		//assignmentSix has 73 submissions, 23 with late submission, 1 submission  with approved extension (within deadline).
		//There are 8 late feedbacks (66-73)-  Among those 8, 3 late submissions ignored and remaining 5 were treated as actual late
		report.getFeedbackCount(assignmentSix) should be (FeedbackCount(68,5, dateTime(2013, 7, 16), dateTime(2013, 8, 1))) // 5 late
	}

	@Test
	def outstandingFeedbackCountTest() {
		val report = getTestFeedbackReport

		assignmentOne.feedbacks.get(9).released = false
		report.getFeedbackCount(assignmentOne) should be (FeedbackCount(9,0, dateTime(2013, 3, 25), dateTime(2013, 3, 25))) // 10 on time
		val outstandingFeedbackCount = assignmentOne.submissions.size() - report.getFeedbackCount(assignmentOne).onTime
		outstandingFeedbackCount should be (1)
	}



	/**
	 * Checks that the dissertation feedback is counted as on time
	 *
	 **/

	@Test
	def feedbackCountsDissertationTest() {
		val report = getTestFeedbackReport

		// assignmentSeven is a dissertation (100 submissions with 50 feedbacks), assignmentEight isn't
		report.getFeedbackCount(assignmentSeven) should be (FeedbackCount(50, 0, dateTime(2013, 8, 1), dateTime(2013, 8, 1))) // everything on time
		//assignmenteight has 100 submissions, 2 submitted late. Among 50 feedbacks published, one was for late submission(id with 50) so ignored and remaining 49 treated as late
		report.getFeedbackCount(assignmentEight) should be (FeedbackCount(1, 49, dateTime(2013, 8, 1), dateTime(2013, 8, 1))) // is not a dissertation, 50 late
	}

	/**
	 * Checks that feedback published on deadline day is marked as on-time
	 *
	 **/

	@Test
	def deadlineDayTest() {

		// For close date of 28/3/2013
		// if 20 working days allowed for feedback
		// 32 days later = deadline day, which should be marked as on-time

		val report = getTestFeedbackReport
		val assignmentEight = addAssignment("1007", "test deadline day - 1", dateTime(2013, 3, 28), 10, 0, moduleOne)
		createPublishEvent(assignmentEight, 31, studentData(0, 10))	// on-time
		var feedbackCount = report.getFeedbackCount(assignmentEight)
		feedbackCount should be (FeedbackCount(10, 0, dateTime(2013, 4, 28), dateTime(2013, 4, 28))) // 10 on time

		val assignmentNine = addAssignment("1008", "test deadline day", dateTime(2013, 3, 28), 10, 0, moduleOne)
		createPublishEvent(assignmentNine, 32, studentData(0, 10))	// on time
		feedbackCount = report.getFeedbackCount(assignmentNine)
		feedbackCount should be (FeedbackCount(10, 0, dateTime(2013, 4, 29), dateTime(2013, 4, 29))) // 10 on time

		val assignmentTen = addAssignment("1009", "test deadline day + 1", dateTime(2013, 3, 28), 10, 0, moduleOne)
		createPublishEvent(assignmentTen, 33, studentData(0, 10))	// late
		feedbackCount = report.getFeedbackCount(assignmentTen)
		feedbackCount should be (FeedbackCount(0, 10, dateTime(2013, 4, 30), dateTime(2013, 4, 30))) // 10 late

	}

	@Test
	def deadlineDayTest2() {
		val report = getTestFeedbackReport
		val assignmentEleven = addAssignment("1010", "test deadline day - 1", dateTime(2013, 5, 29), 10, 0, moduleOne)
		createPublishEvent(assignmentEleven, 27, studentData(0, 10))	// on time
		var feedbackCount = report.getFeedbackCount(assignmentEleven)
		feedbackCount should be (FeedbackCount(10, 0, dateTime(2013, 6, 25), dateTime(2013, 6, 25))) // 10 on time

		val assignmentTwelve = addAssignment("1010", "test deadline day", dateTime(2013, 5, 29), 10, 0, moduleOne)
		createPublishEvent(assignmentTwelve, 28, studentData(0, 10))	// on time
		feedbackCount = report.getFeedbackCount(assignmentTwelve)
		feedbackCount should be (FeedbackCount(10, 0, dateTime(2013, 6, 26), dateTime(2013, 6, 26))) // 10 on time

		val assignmentThirteen = addAssignment("1011", "test deadline day + 1", dateTime(2013, 5, 29), 10, 0, moduleOne)
		createPublishEvent(assignmentThirteen, 29, studentData(0, 10))	// late
		feedbackCount = report.getFeedbackCount(assignmentThirteen)
		feedbackCount should be (FeedbackCount(0, 10, dateTime(2013, 6, 27), dateTime(2013, 6, 27))) // 10 late
	}


	// Move this up and out if it'd be useful in other tests.
	object SpreadsheetTester {
		/** Assert that this cell is the expected value. */
		def compare(cell: Cell, expected: Any): Unit = expected match {
			case string: String => cell.getStringCellValue should be (string)
			case dt: AbstractInstant => cell.getDateCellValue should be (dt.toDate)
			case date: Date => cell.getDateCellValue should be (date)
			case number: Number => cell.getNumericCellValue should be (number)
			case null => cell.getDateCellValue should be (null)
			case x => fail("This value type is not handled by compare(): " + x)
		}

		/** Check each cell in the row against the corresponding item in expected. */
		def check(description:String, row:XSSFRow, expected: Seq[Any]) {
			for ((cell, (expectedValue, i)) <- row.cellIterator().asScala.toSeq zip expected.zipWithIndex) {
				withClue(s"$description column index $i:") {
					compare(cell, expectedValue)
				}
			}
		}
	}

	@Test
	def sheetTest() {
		import SpreadsheetTester._

		val report = getTestFeedbackReport
		report.buildAssignmentData()

		val assignmentSheet = report.generateAssignmentSheet(department)
		report.populateAssignmentSheet(assignmentSheet)

		check("Row 1",
			assignmentSheet.getRow(1),
			Seq("test one", "IN101", dateTime(2013, 3, 10), dateTime(2013, 4, 9), "Summative", "", 10, 10, 0, 2, 0, 10, 10, 1, 0, 0))

		check("Row 2",
			assignmentSheet.getRow(2),
			Seq("test two", "IN101", dateTime(2013, 4, 10), dateTime(2013, 5, 9), "Summative", "", 29, 29, 0, 5, 0, 29, 5, 0.1724137931034483, 24, 0.8275862068965517))

		check("Row 3",
			assignmentSheet.getRow(3),
			Seq("test three", "IN101", dateTime(2013, 5, 10), dateTime(2013, 6, 10), "Formative", "",  13, 13, 0, 2, 0, 13, 6, 0.46153846153846156, 7, 0.5384615384615384))

		check("Row 4",
			assignmentSheet.getRow(4),
			Seq("test four","IN102",dateTime(2013, 5, 30),dateTime(2013, 6, 27),"Summative", "", 35,35,0,7,0,35,7,0.2,28,0.8))

		check("Row 5",
			assignmentSheet.getRow(8),
			Seq("test five","IN102",dateTime(2013, 8, 22),dateTime(2013, 9, 20),"Summative", "", 100,100,0,2,0,100,2,0.02,98,0.98))

		/** Not sure how this was working previously with publish date as dateTime(2013, 7, 31). Looking at the logic of extension deadline, 20 day cal is done
			* based on assignment close date if there are no extensions OR if there is any extension other than approved OR if there is any submission that doesn't
			* have any extension. Test six has approved extension for just one student and not for other students which forces this to have deadline cal based on
			* close date rather than caluculation done on extension expiry date (2 days after assignment close date)
			*/
		check("Row 6",
			assignmentSheet.getRow(5),
			Seq("test six","IN102",dateTime(2013, 7, 1),dateTime(2013, 7, 29),"Summative", "", 73,73,1,23,0,73,68,0.9315068493150684,5,0.0684931506849315))

		check("Row 7",
			assignmentSheet.getRow(6),
			Seq("test seven","IN102", dateTime(2013, 7, 1), null, "Summative", "Dissertation", 100, 100, 0, 2, 50, 50, 50, 1, 0, 0))

		//assignmentEight has 100 submissions, 2 submitted late. Only 50 have feedback as late.
		// 1 of this 50 was submitted late so will be excluded
		check("Row 8",
			assignmentSheet.getRow(7),
			Seq("test eight","IN102", dateTime(2013, 7, 1), dateTime(2013, 7, 29), "Summative", "", 100, 100, 0, 2, 50, 50, 1, 0.02, 49, 0.98)
		)


		val moduleSheet = report.generateModuleSheet(department)
		report.populateModuleSheet(moduleSheet)

		//assignmentOne/assignmentTwo/assignmentThree belongs to -IN101 - count of all those
		check("Module row 1",
			moduleSheet.getRow(1),
			Seq("Module One","IN101",3,52,52,0,9,0,52,21,0.40384615384615385,31,0.5961538461538461))

		//assignmentFour/assignmentFive/assignmentSix/assignmentSeven/assignmentEight belongs to -IN102 - count of all those
		check("Module row 2",
			moduleSheet.getRow(2),
			Seq("Module Two","IN102",5,408,408,1,36,100,308,128,0.4155844155844156,180,0.5844155844155844))
	}

	def getTestFeedbackReport: FeedbackReport = {
		val report = new FeedbackReport(department, dateTime(2013, 3, 1), dateTime(2013, 9, 30))
		report.assignmentMembershipService = assignmentMembershipService
		report.auditEventQueryMethods = auditEventQueryMethods
		report.submissionService = submissionService
		report.feedbackService = feedbackService
		report
	}

}
