package uk.ac.warwick.courses.commands

import uk.ac.warwick.courses.TestBase
import org.springframework.mock.web.MockMultipartFile
import uk.ac.warwick.courses.helpers.ArrayList
import uk.ac.warwick.courses.Mockito
import uk.ac.warwick.courses.data.FileDao

class UploadedFileTest extends TestBase with Mockito{

	val multi1 = new MockMultipartFile("file", "feedback.doc", "text/plain", "aaaaaaaaaaaaaaaa".getBytes)
	val multiEmpty = new MockMultipartFile("file", null, "text/plain", null: Array[Byte])
	
	@Test // HFC-375
	def ignoreEmptyMultipartFiles {
		val uploadedFile = new UploadedFile
		uploadedFile.fileDao = smartMock[FileDao]
		uploadedFile.upload = ArrayList(multi1, multiEmpty)
		uploadedFile.onBind
		
		uploadedFile.attached.size should be (1)
		uploadedFile.attached.get(0).name should be ("feedback.doc")
	}
	
	@Test
	def hasUploads {
		val uploadedFile = new UploadedFile
		uploadedFile.upload = ArrayList()
		uploadedFile.hasUploads should be (false)
		uploadedFile.uploadOrEmpty should be (ArrayList())
		
		uploadedFile.upload = ArrayList(multiEmpty)
		uploadedFile.hasUploads should be (false)
		uploadedFile.uploadOrEmpty should be (ArrayList())
		
		uploadedFile.upload = ArrayList(multi1)
		uploadedFile.hasUploads should be (true)
		uploadedFile.uploadOrEmpty should be (ArrayList(multi1))
	}

}