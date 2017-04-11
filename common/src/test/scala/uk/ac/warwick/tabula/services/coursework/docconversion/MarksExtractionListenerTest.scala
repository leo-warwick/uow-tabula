package uk.ac.warwick.tabula.services.coursework.docconversion

import java.io.ByteArrayInputStream

import org.apache.poi.hssf.eventusermodel.HSSFEventFactory
import org.apache.poi.hssf.eventusermodel.HSSFRequest
import org.apache.poi.poifs.filesystem.POIFSFileSystem
import uk.ac.warwick.tabula.helpers.Closeables.closeThis
import org.apache.poi.openxml4j.exceptions.InvalidFormatException
import uk.ac.warwick.tabula.{MockUserLookup, Mockito, TestBase}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.userlookup.User

// scalastyle:off magic.number
class MarksExtractionListenerTest extends TestBase with Logging with Mockito {

	val mockUserLookup = new MockUserLookup
	val s1  = new User("0123456") { setFoundUser(true); setWarwickId("0123456") }
	val s2  = new User("0123457") { setFoundUser(true); setWarwickId("0123457") }
	val s3  = new User("1170836") { setFoundUser(true); setWarwickId("1170836") }
	val s4  = new User("1186549") { setFoundUser(true); setWarwickId("1170836") }
	val s5  = new User("1120365") { setFoundUser(true); setWarwickId("1186549") }
	val s6  = new User("1147650") { setFoundUser(true); setWarwickId("1120365") }
	val s7  = new User("1115345") { setFoundUser(true); setWarwickId("1147650") }
	val s8  = new User("1985435") { setFoundUser(true); setWarwickId("1115345") }
	val s9  = new User("1875610") { setFoundUser(true); setWarwickId("1985435") }
	val s10 = new User("1651020") { setFoundUser(true); setWarwickId("1875610") }
	val s11 = new User("1598452") { setFoundUser(true); setWarwickId("1651020") }
	val s12 = new User("1651508") { setFoundUser(true); setWarwickId("1598452") }
	mockUserLookup.registerUserObjects(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12)

	@Test def readOLE2ExcelFile() {
		closeThis(new ByteArrayInputStream(resourceAsBytes("marks.xls"))) { fin =>
			closeThis(new POIFSFileSystem(fin).createDocumentInputStream("Workbook")) { din =>
				val req = new HSSFRequest
				val listener = new XslSheetHandler()
				req.addListenerForAllRecords(listener)
				val factory = new HSSFEventFactory
				factory.processEvents(req, din)
				listener.markItems.size should be (11) //header not skipped
			}
		}
	}

	// XLSX files are zips, but uploading any other zip gives a specific error
	// "Package should contain a content type part [M1.13]".
	@Test(expected=classOf[InvalidFormatException])
	def unexpectedZipFile() {
		val fin =  new ByteArrayInputStream(resourceAsBytes("feedback1.zip"))
		val marksExtractor = new MarksExtractor()
		marksExtractor.userLookup = mockUserLookup
		val marksList = marksExtractor.readXSSFExcelFile(fin)
		marksList.size should be (10)
	}

	@Test def readXSSFExcelFile() {
		val fin =  new ByteArrayInputStream(resourceAsBytes("marks.xlsx"))
		val marksExtractor = new MarksExtractor()
		marksExtractor.userLookup = mockUserLookup
		val marksList = marksExtractor.readXSSFExcelFile(fin)
		marksList.size should be (10)
	}

}