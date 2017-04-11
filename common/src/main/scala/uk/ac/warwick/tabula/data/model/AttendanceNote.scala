package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.joda.time.DateTime

import javax.validation.constraints.NotNull
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml
import java.text.BreakIterator
import org.hibernate.annotations.Type
import uk.ac.warwick.tabula.helpers.StringUtils._

@Entity
@DiscriminatorColumn(name = "discriminator", discriminatorType = DiscriminatorType.STRING)
abstract class AttendanceNote extends GeneratedId with FormattedHtml {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "student_id")
	var student: Member = _

	var updatedDate: DateTime = _

	@NotNull
	var updatedBy: String = _

	var note: String = _

	def escapedNote: String = formattedHtml(note)

	def truncatedNote: String = {
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
	}

	@OneToOne(cascade=Array(CascadeType.ALL), fetch = FetchType.LAZY)
	@JoinColumn(name = "attachment_id")
	var attachment: FileAttachment = _

	@NotNull
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AbsenceTypeUserType")
	@Column(name = "absence_type")
	var absenceType: AbsenceType = _

	def hasContent: Boolean = note.hasText || attachment != null || absenceType != null

}