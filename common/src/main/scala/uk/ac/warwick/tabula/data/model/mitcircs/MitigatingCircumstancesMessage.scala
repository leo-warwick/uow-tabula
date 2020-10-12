package uk.ac.warwick.tabula.data.model.mitcircs

import java.io.Serializable

import freemarker.core.TemplateHTMLOutputModel
import javax.persistence.CascadeType._
import javax.persistence._
import org.hibernate.annotations.{BatchSize, Proxy, Type}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

@Entity
@Proxy
@Access(AccessType.FIELD)
class MitigatingCircumstancesMessage extends GeneratedId
  with ToString
  with Serializable
  with ToEntityReference {
    type Entity = MitigatingCircumstancesMessage

  def this(submission: MitigatingCircumstancesSubmission, sender: User) = {
    this()
    this.sender = sender
    this.submission = submission
  }

  @ManyToOne
  @JoinColumn(name = "submission_id")
  var submission: MitigatingCircumstancesSubmission = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.EncryptedStringUserType")
  @Column(name = "message")
  private var encryptedMessage: CharSequence = _
  def message: String = Option(encryptedMessage).map(_.toString).orNull
  def message_=(message: String): Unit = encryptedMessage = message

  def formattedMessage: TemplateHTMLOutputModel = FormattedHtml(message.toString)

  @Column(nullable = false)
  @Type(`type` = "uk.ac.warwick.tabula.data.model.SSOUserType")
  final var sender: User = _

  def studentSent: Boolean = sender.getUserId == submission.student.userId

  @Column(nullable = false)
  var createdDate: DateTime = DateTime.now()

  @OneToMany(mappedBy = "mitigatingCircumstancesMessage", fetch = FetchType.LAZY, cascade = Array(ALL))
  @BatchSize(size = 200)
  private val _attachments: JSet[FileAttachment] = JHashSet()
  def attachments: Seq[FileAttachment] = _attachments.asScala.toSeq.sortBy(_.dateUploaded)

  def addAttachment(attachment: FileAttachment): Unit = {
    if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
    attachment.temporary = false
    attachment.mitigatingCircumstancesMessage = this
    _attachments.add(attachment)
  }

  def removeAttachment(attachment: FileAttachment): Boolean = {
    attachment.mitigatingCircumstancesMessage = null
    _attachments.remove(attachment)
  }

  @Column(name = "replyByDate")
  private var _replyByDate: DateTime = _
  def replyByDate: Option[DateTime] = Option(_replyByDate)
  def replyByDate_=(dt: DateTime): Unit = _replyByDate = dt

  def isUnreadByStudent: Boolean = !studentSent && submission.lastViewedByStudent.forall(_.isBefore(createdDate))

  override def toStringProps: Seq[(String, Any)] = Seq(
    "id" -> id,
    "submission" -> submission.key,
    "sender" -> sender.getWarwickId
  )
}




