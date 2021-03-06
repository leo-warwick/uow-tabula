package uk.ac.warwick.tabula.data.model

import javax.persistence.{Column, _}
import org.hibernate.annotations.{Proxy, Type}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.services.turnitintca.{TcaErrorCode, TcaSubmissionStatus}

@Entity
@Proxy
class OriginalityReport extends GeneratedId with ToEntityReference {
  type Entity = OriginalityReport

  // Don't cascade as this is the wrong side of the association
  @OneToOne(optional = false, cascade = Array(), fetch = FetchType.LAZY)
  @JoinColumn(name = "ATTACHMENT_ID")
  var attachment: FileAttachment = _

  var createdDate: DateTime = DateTime.now

  @Column(name = "TURNITIN_ID")
  var turnitinId: String = _

  var lastSubmittedToTurnitin: DateTime = _

  var submitToTurnitinRetries: JInteger = 0

  var fileRequested: DateTime = _

  var lastReportRequest: DateTime = _

  var reportRequestRetries: JInteger = 0

  @Column(name = "REPORT_RECEIVED")
  var reportReceived: Boolean = _

  var lastTurnitinError: String = _

  def similarity: Option[Int] = {
    overlap match {
      case Some(o) if o > 74 => Some(4)
      case Some(o) if o > 49 => Some(3)
      case Some(o) if o > 24 => Some(2)
      case Some(o) if o > 0 => Some(1)
      case Some(o) if o == 0 => Some(0)
      case _ => None
    }
  }

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var overlap: Option[Int] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  @Column(name = "STUDENT_OVERLAP")
  var studentOverlap: Option[Int] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  @Column(name = "WEB_OVERLAP")
  var webOverlap: Option[Int] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  @Column(name = "PUBLICATION_OVERLAP")
  var publicationOverlap: Option[Int] = None

  // TCA fields

  var tcaSubmissionRequested: Boolean = _

  var tcaSubmission: String = _

  @Type(`type` = "uk.ac.warwick.tabula.services.turnitintca.TcaSubmissionStatusUserType")
  var tcaSubmissionStatus: TcaSubmissionStatus = _

  @Type(`type` = "uk.ac.warwick.tabula.services.turnitintca.TcaErrorCodeUserType")
  var errorCode: TcaErrorCode = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var pageCount: Option[Int] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var wordCount: Option[Int] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var characterCount: Option[Int] = None

  var similarityRequestedOn: DateTime = _

  var similarityLastGenerated: DateTime = _

  def isTcaReport: Boolean = Option(tcaSubmission).isDefined

  def tcaSubmissionCreated: Boolean = tcaSubmissionStatus == TcaSubmissionStatus.Created

  def tcaSubmissionProcessing: Boolean = tcaSubmissionStatus == TcaSubmissionStatus.Processing

  def tcaUploadComplete: Boolean = tcaSubmissionStatus == TcaSubmissionStatus.Complete

  def tcaSimilarityCheckComplete: Boolean = Option(similarityLastGenerated).isDefined

  def hasTcaError: Boolean = tcaSubmissionStatus == TcaSubmissionStatus.Error

}
