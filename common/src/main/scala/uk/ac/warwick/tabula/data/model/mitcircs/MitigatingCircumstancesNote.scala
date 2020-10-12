package uk.ac.warwick.tabula.data.model.mitcircs

import java.io.Serializable

import freemarker.core.TemplateHTMLOutputModel
import javax.persistence.CascadeType.ALL
import javax.persistence._
import org.hibernate.annotations.{BatchSize, Proxy, Type}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml
import uk.ac.warwick.tabula.data.model.{FileAttachment, GeneratedId}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

@Entity
@Proxy
@Access(AccessType.FIELD)
class MitigatingCircumstancesNote extends GeneratedId
  with ToString
  with Serializable {

  def this(submission: MitigatingCircumstancesSubmission, creator: User) = {
    this()
    this._creator = creator
    this.lastModifiedBy = creator
    this.submission = submission
  }

  @ManyToOne
  @JoinColumn(name = "submission_id")
  var submission: MitigatingCircumstancesSubmission = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.EncryptedStringUserType")
  @Column(name = "text")
  private var encryptedText: CharSequence = _
  def text: String = Option(encryptedText).map(_.toString).orNull
  def text_=(text: String): Unit = encryptedText = text

  def formattedText: TemplateHTMLOutputModel = FormattedHtml(text)

  @Column(name = "creator", nullable = false)
  @Type(`type` = "uk.ac.warwick.tabula.data.model.SSOUserType")
  private var _creator: User = _
  def creator: User = _creator

  @Column(name = "createdDate", nullable = false)
  private val _createdDate: DateTime = DateTime.now()
  def createdDate: DateTime = _createdDate

  @Column(name = "lastModifiedDate", nullable = false)
  var lastModified: DateTime = DateTime.now()

  @Column(nullable = false)
  @Type(`type` = "uk.ac.warwick.tabula.data.model.SSOUserType")
  var lastModifiedBy: User = _

  @OneToMany(mappedBy = "mitigatingCircumstancesNote", fetch = FetchType.LAZY, cascade = Array(ALL))
  @BatchSize(size = 200)
  private val _attachments: JSet[FileAttachment] = JHashSet()
  def attachments: Seq[FileAttachment] = _attachments.asScala.toSeq.sortBy(_.dateUploaded)

  def addAttachment(attachment: FileAttachment): Unit = {
    if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
    attachment.temporary = false
    attachment.mitigatingCircumstancesNote = this
    _attachments.add(attachment)
  }

  def removeAttachment(attachment: FileAttachment): Boolean = {
    attachment.mitigatingCircumstancesNote = null
    _attachments.remove(attachment)
  }

  override def toStringProps: Seq[(String, Any)] = Seq(
    "id" -> id,
    "submission" -> submission.key,
    "creator" -> creator.getWarwickId
  )

}
