package uk.ac.warwick.tabula.data.model

import com.google.common.io.ByteSource
import com.google.common.net.MediaType
import javax.persistence.CascadeType._
import javax.persistence._
import org.hibernate.annotations.Proxy
import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.model.forms.{Extension, SavedFormValue}
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesMessage, MitigatingCircumstancesNote, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.helpers.DetectMimeType._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.objectstore.{ObjectStorageService, RichByteSource}
import uk.ac.warwick.tabula.services.turnitintca.TcaSubmissionStatus.{Complete, Created, Processing}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters._
import scala.language.postfixOps
import scala.util.matching.Regex

@Entity
@Proxy
@Access(AccessType.FIELD)
class FileAttachment extends GeneratedId {

  import FileAttachment._

  @transient var fileDao: FileDao = Wire[FileDao]
  @transient var objectStorageService: ObjectStorageService = Wire[ObjectStorageService]

  @Column(name = "file_hash")
  var hash: String = _

  // optional link to a SubmissionValue
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "submission_id")
  var submissionValue: SavedFormValue = _

  // optional link to some Feedback
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "feedback_id")
  var feedback: Feedback = _

  // optional link to an Extension
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "extension_id")
  var extension: Extension = _

  // optional link to a Member Note
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_note_id")
  var memberNote: AbstractMemberNote = _

  // optional link to Meeting Record
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "meetingrecord_id")
  var meetingRecord: AbstractMeetingRecord = _

  // optional link to MarkerFeedback via MarkerFeedbackAttachment
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinTable(name = "MarkerFeedbackAttachment",
    joinColumns = Array(new JoinColumn(name = "file_attachment_id")),
    inverseJoinColumns = Array(new JoinColumn(name = "marker_feedback_id")))
  var markerFeedback: MarkerFeedback = _


  // optional link to MitigatingCircumstancesSubmission via MitCircsSubmissionAttachment
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinTable(name = "mitcircssubmissionattachment",
    joinColumns = Array(new JoinColumn(name = "file_attachment_id")),
    inverseJoinColumns = Array(new JoinColumn(name = "submission_id")))
  var mitigatingCircumstancesSubmission: MitigatingCircumstancesSubmission = _

  // optional link to MitigatingCircumstancesMessage via MitCircsMessageAttachment
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinTable(name = "mitcircsmessageattachment",
    joinColumns = Array(new JoinColumn(name = "file_attachment_id")),
    inverseJoinColumns = Array(new JoinColumn(name = "message_id")))
  var mitigatingCircumstancesMessage: MitigatingCircumstancesMessage = _

  // optional link to MitigatingCircumstancesNote via MitCircsMessageAttachment
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinTable(name = "mitcircsnoteattachment",
    joinColumns = Array(new JoinColumn(name = "file_attachment_id")),
    inverseJoinColumns = Array(new JoinColumn(name = "note_id")))
  var mitigatingCircumstancesNote: MitigatingCircumstancesNote = _

  /*
   * Both of these are really One-to-One relationships (and are @OneToOne on the other side)
   * but Hibernate can't lazy-load a One-to-One because it doesn't know whether to set the
   * property to a proxy or null, so we pretend it's OneToMany to avoid eagerly loading
   * originality reports and feedback templates every time we fetch a file attachment from
   * the database.
   */

  @OneToMany(fetch = FetchType.LAZY, cascade = Array(PERSIST), mappedBy = "attachment")
  private val _originalityReport: JList[OriginalityReport] = JArrayList()

  def originalityReport: OriginalityReport = _originalityReport.asScala.headOption.orNull

  def originalityReport_=(originalityReport: OriginalityReport): Unit = {
    _originalityReport.clear()
    _originalityReport.add(originalityReport)
  }

  @OneToMany(fetch = FetchType.LAZY, cascade = Array(PERSIST), mappedBy = "attachment")
  private val _feedbackForm: JList[FeedbackTemplate] = JArrayList()

  def feedbackForm: FeedbackTemplate = _feedbackForm.asScala.headOption.orNull

  def feedbackForm_=(feedbackForm: FeedbackTemplate): Unit = {
    _feedbackForm.clear()
    _feedbackForm.add(feedbackForm)
  }

  /**
    * WARNING this method isn't exhaustive. It only checks fields that are directly on this
    * attachment table. It won't check mappings where the foreign key is on the other side,
    * which is the case for things like member photos.
    */
  def isAttached: JBoolean = Seq(feedback, submissionValue, extension, originalityReport, mitigatingCircumstancesSubmission, mitigatingCircumstancesMessage, mitigatingCircumstancesNote).exists(_ != null)

  var temporary: JBoolean = true

  var dateUploaded: DateTime = DateTime.now
  var uploadedBy: String = _

  @Column(name = "name")
  private var _name: String = _

  def name: String = _name

  def getName: String = _name

  def setName(n: String): Unit = {
    name = n
  }

  def name_=(n: String): Unit = {
    _name = Option(n).map(sanitisedFilename).orNull
  }

  def this(n: String) = {
    this()
    name = n
  }

  def length: Option[Long] = Option(asByteSource).filterNot(_.isEmpty).map(_.size)

  // checks the length field first. If that is not populated use uploadedData instead
  def actualDataLength: Long = length.orElse(Option(uploadedData).map(_.size())).getOrElse(0)

  def fileExt: String = {
    if (name.lastIndexOf('.') > -1) {
      name.substring(name.lastIndexOf('.') + 1)
    } else {
      ""
    }
  }

  @transient
  lazy val asByteSource: RichByteSource = Await.result(objectStorageService.fetch(id), Duration.Inf)

  def duplicate(): FileAttachment = {
    val newFile = new FileAttachment(name)
    newFile.uploadedData = asByteSource
    newFile.uploadedBy = uploadedBy
    fileDao.savePermanent(newFile)
    newFile
  }

  // we have a result back from the Turnitin for this file - either a similarity score or an error relating to the file contents
  def turnitinResultReceived: Boolean = {
    originalityReport != null && (originalityReport.reportReceived || originalityReport.hasTcaError)
  }

  def turnitinCheckInProgress: Boolean = {
    Option(originalityReport).exists(or => Seq(Created, Processing, Complete).contains(or.tcaSubmissionStatus))
  }

  def generateToken(): FileAttachmentToken = {
    val token = new FileAttachmentToken
    token.fileAttachmentId = this.id
    token.expires = new DateTime().plusMinutes(FileAttachmentToken.DefaultTokenValidityMinutes)
    token
  }

  def hasData: Boolean = id.hasText && Await.result(objectStorageService.keyExists(id), Duration.Inf)

  @transient var uploadedData: ByteSource = _
  @transient var overwrite: Boolean = false

  def isDataEqual(other: Any): Boolean = other match {
    case that: FileAttachment =>
      if (this.id != null && that.id != null && this.id == that.id) true
      else if (this.actualDataLength != that.actualDataLength) false
      else {
        val thisData = this.asByteSource
        val thatData = that.asByteSource

        if (thisData == null && thatData == null) true
        else if (thisData != null && thatData != null && thisData.isEmpty && thatData.isEmpty) true
        else thisData != null && thatData != null && thisData.contentEquals(thatData)
      }
    case _ => false
  }

  @transient lazy val mimeType: String = asByteSource.metadata match {
    case Some(metadata) if metadata.contentType != MediaType.OCTET_STREAM.toString => metadata.contentType
    case _ => Option(asByteSource).filterNot(_.isEmpty).map { source => detectMimeType(source.openStream()).toString }.getOrElse(MediaType.OCTET_STREAM.toString)
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
