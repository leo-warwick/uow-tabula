package uk.ac.warwick.tabula.data.model

import java.io._
import javax.persistence.CascadeType._
import javax.persistence._

import com.google.common.io.ByteSource
import com.google.common.net.MediaType
import org.apache.commons.io.IOUtils
import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.model.forms.{Extension, SavedFormValue}
import uk.ac.warwick.tabula.helpers.DetectMimeType._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.objectstore.ObjectStorageService

import scala.language.postfixOps
import scala.util.matching.Regex

@Entity @Access(AccessType.FIELD)
class FileAttachment extends GeneratedId {
	import FileAttachment._

	@transient var fileDao: FileDao = Wire[FileDao]
	@transient var objectStorageService: ObjectStorageService = Wire[ObjectStorageService]

	@Column(name="file_hash")
	var hash: String = _

	// optional link to a SubmissionValue
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "submission_id")
	var submissionValue: SavedFormValue = null

	// optional link to some Feedback
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "feedback_id")
	var feedback: Feedback = _

	// optional link to an Extension
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="extension_id")
	var extension:Extension =_

	// optional link to a Member Note
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="member_note_id")
	var memberNote: AbstractMemberNote =_

	// optional link to Meeting Record
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "meetingrecord_id")
	var meetingRecord: AbstractMeetingRecord = _

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinTable(name="MarkerFeedbackAttachment",
		joinColumns=Array( new JoinColumn(name="file_attachment_id") ),
		inverseJoinColumns=Array( new JoinColumn(name="marker_feedback_id")) )
	var markerFeedback:MarkerFeedback = _

	@OneToOne(fetch = FetchType.LAZY, cascade = Array(PERSIST), mappedBy = "attachment")
	var originalityReport: OriginalityReport = _

	@OneToOne(fetch = FetchType.LAZY, cascade = Array(PERSIST), mappedBy = "attachment")
	var feedbackForm: FeedbackTemplate = _

	/**
	 * WARNING this method isn't exhaustive. It only checks fields that are directly on this
	 * attachment table. It won't check mappings where the foreign key is on the other side,
	 * which is the case for things like member photos.
	 */
	def isAttached: JBoolean = Seq(feedback, submissionValue, extension, originalityReport).exists(_ != null)

	var temporary: JBoolean = true

	var dateUploaded: DateTime = DateTime.now
	var uploadedBy: String = _

	@Column(name = "name")
	private var _name: String = _
	def name: String = _name
	def getName: String = _name
	def setName(n: String) { name = n }
	def name_=(n: String) {
		_name = Option(n).map(sanitisedFilename).orNull
	}

	def this(n: String) {
		this()
		name = n
	}

	def length: Option[Long] = objectStorageService.metadata(id).map { _.contentLength }

	// checks the length field first. If that is not populated use uploadedData instead
	def actualDataLength: Long = length.orElse(Option(uploadedData).map { _.size() }).getOrElse(0)

	def fileExt: String = {
		if (name.lastIndexOf('.') > -1) {
			name.substring(name.lastIndexOf('.') + 1)
		} else {
			""
		}
	}

	/**
		* Returns null if no data to return
		*/
	def dataStream: InputStream = objectStorageService.fetch(id).orNull

	def duplicate(): FileAttachment = {
		val newFile = new FileAttachment(name)
		newFile.uploadedData = new ByteSource {
			override def openStream(): InputStream = dataStream
			override def size(): Long = actualDataLength
		}
		newFile.uploadedBy = uploadedBy
		fileDao.savePermanent(newFile)
		newFile
	}

	def originalityReportReceived: Boolean = {
		originalityReport != null && originalityReport.reportReceived
	}

	def urkundResponseReceived: Boolean = {
		originalityReport != null && originalityReport.reportUrl != null
	}

	def generateToken(): FileAttachmentToken = {
		val token = new FileAttachmentToken
		token.fileAttachmentId = this.id
		token.expires = new DateTime().plusMinutes(FileAttachmentToken.DefaultTokenValidityMinutes)
		token
	}

	def hasData: Boolean = id.hasText && objectStorageService.keyExists(id)

	@transient var uploadedData: ByteSource = null

	def isDataEqual(other: Any): Boolean = other match {
		case that: FileAttachment =>
			if (this.id != null && that.id != null && this.id == that.id) true
			else if (this.actualDataLength != that.actualDataLength) false
			else {
				val thisData = this.dataStream
				val thatData = that.dataStream

				if (thisData == null && thatData == null) true
				else try {
					thisData != null && thatData != null && IOUtils.contentEquals(thisData, thatData)
				} finally {
					IOUtils.closeQuietly(thisData)
					IOUtils.closeQuietly(thatData)
				}
			}
		case _ => false
	}

	@transient lazy val mimeType: String = objectStorageService.metadata(id) match {
		case Some(metadata) if metadata.contentType != MediaType.OCTET_STREAM.toString => metadata.contentType
		case _ => objectStorageService.fetch(id).map(detectMimeType).getOrElse(MediaType.OCTET_STREAM.toString)
	}
}

object FileAttachment {
	private val BadCharacters = new Regex("[^-!'., \\w]")
	private val Space = new Regex("(\\s|%20)+")
	private val DefaultFilename = "download"
	private val DefaultExtension = "unknowntype"

	def sanitisedFilename(filename: String): String = {
		val spaced = Space.replaceAllIn(filename, " ")
		val sanitised = BadCharacters.replaceAllIn(spaced, "")
		val leadingDot = sanitised.head == '.'

		val dotSplit = sanitised.split('.').map(_.trim).toList.filterNot(_.isEmpty)
		dotSplit match {
			case Nil => s"$DefaultFilename.$DefaultExtension"
			case extension :: Nil if leadingDot => s"$DefaultFilename.$extension"
			case fn :: Nil => s"$fn.$DefaultExtension"
			case filenameWithExtension => filenameWithExtension.mkString(".")
		}
	}
}