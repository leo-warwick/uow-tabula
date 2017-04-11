package uk.ac.warwick.tabula.commands.attendance

import java.io.ByteArrayInputStream

import com.google.common.io.ByteSource
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.services.objectstore.ObjectStorageService
import uk.ac.warwick.tabula.services.{ProfileService, ProfileServiceComponent}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class CSVAttendanceExporterTest extends TestBase with Mockito {

	trait Fixture {
		val exporter = new CSVAttendanceExtractorInternal with ProfileServiceComponent {
			override val profileService: ProfileService = smartMock[ProfileService]
		}
		val errors = new BindException(exporter, "command")
	}

	@Test
	def noFile(): Unit = {
		new Fixture {
			private val file = new UploadedFile
			exporter.file = file
			private val result = exporter.extract(errors)
			result should be (Map())
			errors.hasErrors should be (true)
			errors.getAllErrors.get(0).getCode should be ("file.missing")
		}
	}

	trait FileFixture extends Fixture {
		private val file = new UploadedFile
		exporter.file = file
		val attachment = new FileAttachment
		attachment.id = "1234"
		attachment.objectStorageService = smartMock[ObjectStorageService]
		file.attached.add(attachment)
	}

	@Test
	def notEnoughFields(): Unit = {
		new FileFixture {
			attachment.objectStorageService.fetch(attachment.id) returns Option(new ByteArrayInputStream("test".getBytes))
			private val result = exporter.extract(errors)
			result should be (Map())
			errors.hasErrors should be (true)
			errors.getAllErrors.get(0).getCode should be ("attendanceMonitoringCheckpoint.upload.wrongFields")
		}
		new FileFixture {
			attachment.objectStorageService.fetch(attachment.id) returns Option(new ByteArrayInputStream("one,two,three".getBytes))
			private val result = exporter.extract(errors)
			result should be (Map())
			errors.hasErrors should be (true)
			errors.getAllErrors.get(0).getCode should be ("attendanceMonitoringCheckpoint.upload.wrongFields")
		}
	}

	@Test
	def noSuchState(): Unit = {
		new FileFixture {
			attachment.objectStorageService.fetch(attachment.id) returns Option(new ByteArrayInputStream("test,".getBytes))
			private val result = exporter.extract(errors)
			result should be (Map())
			errors.hasErrors should be (true)
			errors.getAllErrors.get(0).getCode should be ("attendanceMonitoringCheckpoint.upload.wrongState")
		}
		new FileFixture {
			attachment.objectStorageService.fetch(attachment.id) returns Option(new ByteArrayInputStream("test,noshow".getBytes))
			private val result = exporter.extract(errors)
			result should be (Map())
			errors.hasErrors should be (true)
			errors.getAllErrors.get(0).getCode should be ("attendanceMonitoringCheckpoint.upload.wrongState")
		}
	}

	@Test
	def noSuchStudent(): Unit = {
		new FileFixture {
			attachment.objectStorageService.fetch(attachment.id) returns Option(new ByteArrayInputStream("1234,attended".getBytes))
			exporter.profileService.getAllMembersWithUniversityIds(Seq("1234")) returns Seq()
			private val result = exporter.extract(errors)
			result should be (Map())
			errors.hasErrors should be (true)
			errors.getAllErrors.get(0).getCode should be ("attendanceMonitoringCheckpoint.upload.notStudent")
			errors.getAllErrors.get(0).getArguments.apply(0).asInstanceOf[String] should be ("1234,attended")
		}
		new FileFixture {
			attachment.objectStorageService.fetch(attachment.id) returns Option(new ByteArrayInputStream("1234,attended".getBytes))
			exporter.profileService.getAllMembersWithUniversityIds(Seq("1234")) returns Seq(Fixtures.staff("1234"))
			private val result = exporter.extract(errors)
			result should be (Map())
			errors.hasErrors should be (true)
			errors.getAllErrors.get(0).getCode should be ("attendanceMonitoringCheckpoint.upload.notStudent")
			errors.getAllErrors.get(0).getArguments.apply(0).asInstanceOf[String] should be ("1234,attended")
		}
	}

	@Test
	def allGood(): Unit = {
		new FileFixture {
			private val student1 = Fixtures.student("1234")
			private val student2 = Fixtures.student("2345")
			attachment.objectStorageService.fetch(attachment.id) returns Option(new ByteArrayInputStream("1234,attended\n2345,not-recorded".getBytes))
			exporter.profileService.getAllMembersWithUniversityIds(Seq("1234", "2345")) returns Seq(student1, student2)
			private val result = exporter.extract(errors)
			result should be (Map(student1 -> AttendanceState.Attended, student2 -> AttendanceState.NotRecorded))
			errors.hasErrors should be (false)
		}
	}

}
