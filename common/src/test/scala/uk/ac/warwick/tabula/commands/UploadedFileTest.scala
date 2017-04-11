package uk.ac.warwick.tabula.commands

import org.springframework.mock.web.MockMultipartFile
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.services.MaintenanceModeService
import uk.ac.warwick.tabula.{Mockito, TestBase}


class UploadedFileTest extends TestBase with Mockito{

	val multi1 = new MockMultipartFile("file", "feedback.doc", "text/plain", "aaaaaaaaaaaaaaaa".getBytes)
	val multiEmpty = new MockMultipartFile("file", null, "text/plain", null: Array[Byte])
	val multiUnderscorePrefix = new MockMultipartFile("file", "thumbs.db", "text/plain", "aaaaaaaaaaaa".getBytes)
	val multiSystemFile = new MockMultipartFile("file", "thumbs.db", "text/plain", "aaaaaaaaaaaa".getBytes)
	val multiAppleDouble = new MockMultipartFile("file", "._thing.doc", "text/plain", "aaaaaaaaaaaa".getBytes)

	@Test // HFC-375
	def ignoreEmptyMultipartFiles() {
		val uploadedFile = new UploadedFile
		uploadedFile.maintenanceMode = smartMock[MaintenanceModeService]
		uploadedFile.fileDao = smartMock[FileDao]
		uploadedFile.upload = JArrayList(multi1, multiEmpty)
		uploadedFile.onBind(new BindException(uploadedFile, "file"))

		uploadedFile.attached.size should be (1)
		uploadedFile.attached.get(0).name should be ("feedback.doc")
	}

	@Test
	def uploads() {
		val uploadedFile = new UploadedFile
		uploadedFile.upload = JArrayList()
		uploadedFile.hasUploads should be {false}
		uploadedFile.uploadOrEmpty should be (JArrayList())

		uploadedFile.upload = JArrayList(multiEmpty)
		uploadedFile.hasUploads should be {false}
		uploadedFile.uploadOrEmpty should be (JArrayList())

		uploadedFile.upload = JArrayList(multi1)
		uploadedFile.hasUploads should be {true}
		uploadedFile.uploadOrEmpty should be (JArrayList(multi1))
	}


	@Test // TAB-48
	def ignoreSystemFiles() {
		val uploadedFile = new UploadedFile
		uploadedFile.maintenanceMode = smartMock[MaintenanceModeService]
		uploadedFile.disallowedFilenames = List("thumbs.db")
		uploadedFile.fileDao = smartMock[FileDao]
		uploadedFile.upload = JArrayList(multi1, multiSystemFile)
		uploadedFile.onBind(new BindException(uploadedFile, "file"))

		uploadedFile.attached.size should be (1)
		uploadedFile.attached.get(0).name should be ("feedback.doc")
	}


	@Test // TAB-48
	def ignoreAppleDouble() {
		val uploadedFile = new UploadedFile
		uploadedFile.maintenanceMode = smartMock[MaintenanceModeService]
		uploadedFile.disallowedPrefixes = List("._")
		uploadedFile.fileDao = smartMock[FileDao]
		uploadedFile.upload = JArrayList(multi1, multiAppleDouble)
		uploadedFile.onBind(new BindException(uploadedFile, "file"))

		uploadedFile.attached.size should be (1)
		uploadedFile.attached.get(0).name should be ("feedback.doc")
	}

	@Test
	def customDisallowed() {
		val uploadedFile = new UploadedFile
		uploadedFile.maintenanceMode = smartMock[MaintenanceModeService]
		uploadedFile.fileDao = smartMock[FileDao]
		uploadedFile.disallowedPrefixes = List()
		uploadedFile.disallowedFilenames = List("feedback.doc")
		uploadedFile.upload = JArrayList(multiSystemFile, multiAppleDouble, multi1)
		uploadedFile.onBind(new BindException(uploadedFile, "file"))

		withClue(uploadedFile.attached) {
		uploadedFile.attached.size should be (2)
		}
		uploadedFile.attached.get(0).name should be (multiSystemFile.getOriginalFilename)
		uploadedFile.attached.get(1).name should not be multiAppleDouble.getOriginalFilename
	}


}