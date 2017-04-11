package uk.ac.warwick.tabula.commands.coursework.assignments

import java.io.{File, FileOutputStream, InputStream}

import com.google.common.io.ByteSource
import org.apache.commons.compress.archivers.zip.ZipFile
import org.springframework.util.FileCopyUtils
import org.springframework.validation.{BindingResult, Errors}
import org.springframework.web.multipart.MultipartFile
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.UniversityId
import uk.ac.warwick.tabula.commands.{Command, Description, UploadedFile}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment, Module}
import uk.ac.warwick.tabula.data.{Daoisms, FileDao}
import uk.ac.warwick.tabula.helpers._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.core.spring.FileUtils

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.matching.Regex

class FeedbackItem {
	var uniNumber: String = _
	var student: Option[User] = _
	var file: UploadedFile = new UploadedFile

	var submissionExists = false
	var isPublished = false
	// true when at least one non-ignored file is uploaded
	var isModified = true
	var duplicateFileNames: Set[String] = Set()
	var ignoredFileNames: Set[String] = Set()

	def listAttachments(): mutable.Buffer[AttachmentItem] = file.attached.asScala.map(f => {
		val duplicate = duplicateFileNames.contains(f.name)
		val ignore = ignoredFileNames.contains(f.name)
		new AttachmentItem(f.name, duplicate, ignore)
	})

	def this(uniNumber:String, student: User) = {
		this()
		this.uniNumber = uniNumber
		this.student = Option(student).filter(_.isFoundUser)
	}

	class AttachmentItem(val name: String, val duplicate: Boolean, val ignore: Boolean)
}

// Purely for storing in command to display on the model.
case class ProblemFile(var path: String, var file: FileAttachment) {
	def this() = this(null, null)
}

// Purely to generate an audit log event
class ExtractFeedbackZip(cmd: UploadFeedbackCommand[_]) extends Command[Unit] {
	def applyInternal() {}
	def describe(d: Description): Unit = d.assignment(cmd.assignment).properties(
		"archive" -> cmd.archive.getOriginalFilename)
}

/**
 * Command which adds feedback for an assignment.
 * It either takes a single uniNumber and file, or it takes a
 * zip of files with uni numbers embedded in their path.
 */
abstract class UploadFeedbackCommand[A](val module: Module, val assignment: Assignment, val marker: User)
	extends Command[A] with Daoisms with Logging with BindListener {

	// Permissions checks delegated to implementing classes FOR THE MOMENT

	val uniNumberPattern = new Regex("""(\d{7,})""")
	// TAB-114 - vast majority of module codes match this pattern
	val moduleCodePattern = new Regex("""([a-z][a-z][0-9][0-z][0-z])""")
	@NoBind var disallowedFilenames: List[String] = commaSeparated(Wire[String]("${uploads.disallowedFilenames}"))
	@NoBind var disallowedPrefixes: List[String] = commaSeparated(Wire[String]("${uploads.disallowedPrefixes}"))

	var zipService: ZipService = Wire[ZipService]
	var userLookup: UserLookupService = Wire[UserLookupService]
	var fileDao: FileDao = Wire[FileDao]
	var assignmentService: AssessmentService = Wire[AssessmentService]
	var stateService: StateService = Wire[StateService]

	var file: UploadedFile = new UploadedFile

	/* for multiple upload */
	// use lazy list with factory as spring doesn't know how to dynamically create items
	var items: JList[FeedbackItem] = LazyLists.create[FeedbackItem]()
	var unrecognisedFiles: JList[ProblemFile] = LazyLists.create[ProblemFile]()
	var moduleMismatchFiles: JList[ProblemFile] = LazyLists.create[ProblemFile]()
	var invalidFiles: JList[ProblemFile] = LazyLists.create[ProblemFile]()
	var archive: MultipartFile = _
	var batch: Boolean = false
	var fromArchive: Boolean = false
	var confirmed: Boolean = false
	/* ---- */

	private def filenameOf(path: String) = new java.io.File(path).getName

	def preExtractValidation(errors: Errors) {
		if (batch) {
			if (archive != null && !archive.isEmpty) {
				logger.debug("file name is " + archive.getOriginalFilename)
				if (!"zip".equals(FileUtils.getLowerCaseExtension(archive.getOriginalFilename))) {
					errors.rejectValue("archive", "archive.notazip")
				}
			} else if (items != null && items.isEmpty && file.isMissing) {
				errors.rejectValue("file.upload", "file.missing")
			}
		}
	}

	def postExtractValidation(errors: Errors) {
		if (!invalidFiles.isEmpty) errors.rejectValue("invalidFiles", "invalidFiles")
		items.asScala.zipWithIndex.foreach { case (item, i) =>
			errors.pushNestedPath("items[" + i + "]")
			validateUploadedFile(item, errors)
			errors.popNestedPath()
		}
	}

	private def validateUploadedFile(item: FeedbackItem, errors: Errors) {
		val file = item.file

		if (file.isMissing) errors.rejectValue("file", "file.missing")
		for((f, i) <- file.attached.asScala.zipWithIndex){
			if (f.actualDataLength == 0) {
				errors.rejectValue("file.attached[" + i + "]", "file.empty")
			}
			// TAB-489 - Check to see that this isn't a blank copy of the feedback template
			else if (assignment.hasFeedbackTemplate && assignment.feedbackTemplate.attachment.actualDataLength == f.actualDataLength) {
				if (f.hash == assignment.feedbackTemplate.attachment.hash)
					errors.rejectValue("file.attached[" + i + "]", "file.duplicate.template")
			}
			if ("url".equals(FileUtils.getLowerCaseExtension(f.getName))) {
				errors.rejectValue("file.attached[" + i + "]", "file.url")
			}
		}

		if (!UniversityId.isValid(item.uniNumber)) {
			errors.rejectValue("uniNumber", "uniNumber.invalid")
		} else {
			item.student match {
				case Some(FoundUser(u)) =>
				case _ => errors.rejectValue("uniNumber", "uniNumber.userNotFound", Array(item.uniNumber), "")
			}

			validateExisting(item, errors)
		}

	}

	def validateExisting(item: FeedbackItem, errors: Errors)

	private def processFiles(bits: Seq[(String, FileAttachment)]) {

		def store(itemMap: collection.mutable.Map[String, FeedbackItem], number: String, name: String, file: FileAttachment) = {
			val student = userLookup.getUserByWarwickUniId(number)
			itemMap
				.getOrElseUpdate(number, new FeedbackItem(number, student))
				.file.attached.add(file)
		}

		// go through individual files, extracting the uni number and grouping
		// them into feedback items.
		val itemMap = new collection.mutable.HashMap[String, FeedbackItem]()
		unrecognisedFiles.clear()

		for ((filename, file) <- bits) {
			// match uni numbers found in file path
			val allNumbers = uniNumberPattern.findAllIn(filename).matchData.map { _.subgroups(0) }.toList

			// ignore any numbers longer than 7 characters.
			val numbers = allNumbers.filter { _.length == 7 }

			if (numbers.isEmpty) {
				// no numbers at all.
				unrecognisedFiles.add(new ProblemFile(filename, file))
			} else if (numbers.distinct.size > 1) {
				// multiple different numbers, ambiguous, reject this.
				invalidFiles.add(new ProblemFile(filename, file))
			} else {
				// one 7 digit number, this one might be okay.
				store(itemMap, numbers.head, filenameOf(filename), file)
			}

			// match potential module codes found in file path
			val allModuleCodes = moduleCodePattern.findAllIn(filename).matchData.map { _.subgroups(0)}.toList

			if(allModuleCodes.nonEmpty){
				// the potential module code doesn't match this assignment's module code
				if (!allModuleCodes.distinct.head.equals(assignment.module.code)){
					moduleMismatchFiles.add(new ProblemFile(filename, file))
				}
			}

		}
		items = itemMap.values.toList.asJava
	}

	override def onBind(result: BindingResult): Unit = transactional() {
		file.onBind(result)

		// ZIP has been uploaded. unpack it
		if (archive != null && !archive.isEmpty) {
			val tempFile = File.createTempFile("feedback", ".zip")
			FileCopyUtils.copy(archive.getInputStream, new FileOutputStream(tempFile))

			val zip = new ZipFile(tempFile)

			try {
				val bits = for (
					entry <- zip.getEntries().asScala.toSeq
					if !entry.isDirectory
					if !(disallowedFilenames contains entry.getName)
					if !disallowedPrefixes.exists(filenameOf(entry.getName).startsWith)
				) yield {
					val f = new FileAttachment

					// Funny char from Windows? We can't work out what it is so
					// just turn it into an underscore.
					val name = entry.getName.replace("\uFFFD", "_")
					f.name = filenameOf(name)

					f.uploadedData = new ByteSource {
						override def openStream(): InputStream = zip.getInputStream(entry)

						override def size(): Long = entry.getSize
					}

					f.uploadedBy = marker.getUserId
					fileDao.saveTemporary(f)
					(name, f)
				}

				processFiles(bits)

				// remember we got these items from a Zip, so we can tailor the text in the HTML.
				fromArchive = true

				// this do-nothing command is to generate an audit event to record the unzipping
				new ExtractFeedbackZip(this).apply()
			} finally {
				zip.close()
				if (!tempFile.delete()) tempFile.deleteOnExit()
			}
		} else {
			if (batch && !file.attached.isEmpty) {
				val bits = file.attached.asScala.map { (attachment) => attachment.name -> attachment }
				processFiles(bits)
			}

			if (items != null) {
				for (item <- items.asScala) {
					if (item.file != null) item.file.onBind(result)
					if (item.student == null) {
						item.student = Option(userLookup.getUserByWarwickUniId(item.uniNumber)).filterNot(_.getUserType == "Applicant")
					}
				}
			}
		}

	}

	private def commaSeparated(csv: String) =
		if (csv == null) Nil
		else csv.split(",").toList

}
