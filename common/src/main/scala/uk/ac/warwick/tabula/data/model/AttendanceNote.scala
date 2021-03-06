package uk.ac.warwick.tabula.data.model

import java.text.BreakIterator

import freemarker.core.TemplateHTMLOutputModel
import javax.persistence._
import javax.validation.constraints.NotNull
import org.hibernate.annotations.{Proxy, Type}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml
import uk.ac.warwick.tabula.helpers.StringUtils._

@Entity
@Proxy
@DiscriminatorColumn(name = "discriminator", discriminatorType = DiscriminatorType.STRING)
abstract class AttendanceNote extends GeneratedId {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id")
  var student: Member = _

  var updatedDate: DateTime = _

  @NotNull
  var updatedBy: String = _

  var note: String = _

  def escapedNote: TemplateHTMLOutputModel = FormattedHtml(note)

  def truncatedNote: String = {
    scala.xml.Utility.escape(
      Option(note).fold("")(note => {
        val breakIterator: BreakIterator = BreakIterator.getWordInstance
        breakIterator.setText(note)
        val length = Math.min(note.length, breakIterator.following(Math.min(50, note.length)))
        if (length < 0 || length == note.length) {
          note
        } else {
          note.substring(0, length) + 0x2026.toChar // 0x2026 being unicode horizontal ellipsis (TAB-1891)
        }
      })
    )
  }

  @OneToOne(cascade = Array(CascadeType.ALL), fetch = FetchType.LAZY)
  @JoinColumn(name = "attachment_id")
  var attachment: FileAttachment = _

  @NotNull
  @Type(`type` = "uk.ac.warwick.tabula.data.model.AbsenceTypeUserType")
  @Column(name = "absence_type")
  var absenceType: AbsenceType = _

  def hasContent: Boolean = note.hasText || attachment != null || absenceType != null

}
