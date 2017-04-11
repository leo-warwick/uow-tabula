package uk.ac.warwick.tabula.data.model

import javax.persistence.CascadeType._
import javax.persistence._
import javax.persistence.FetchType._
import javax.validation.constraints.NotNull

import org.hibernate.annotations.{BatchSize, Type}
import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.userlookup.User

@Entity
@DiscriminatorColumn(name = "discriminator", discriminatorType = DiscriminatorType.STRING)
@Table(name = "membernote")
abstract class AbstractMemberNote extends GeneratedId with CanBeDeleted with PermissionsTarget with FormattedHtml {

	@transient
	var userLookup: UserLookupService = Wire.auto[UserLookupService]

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="memberid")
	def member: Member

	var note: String =_

	def escapedNote: String = formattedHtml(note)

	var title: String =_

	@OneToMany(mappedBy="memberNote", fetch=LAZY, cascade=Array(ALL))
	@BatchSize(size=200)
	var attachments: JList[FileAttachment] = JArrayList()

	var creatorId: String =_

	@Type(`type`="org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	var creationDate: DateTime = DateTime.now

	@Type(`type`="org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	var lastUpdatedDate: DateTime = creationDate

	def addAttachment(attachment:FileAttachment) {
		if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
		attachment.temporary = false
		attachment.memberNote = this
		attachments.add(attachment)
	}

	def permissionsParents = Stream(member)

	override def toString: String = "MemberNote(" + id + ")"

	def creator: User = userLookup.getUserByWarwickUniId(creatorId)

}

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("note")
class MemberNote extends AbstractMemberNote {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="memberid")
	var member: Member = _

}

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("circumstances")
class ExtenuatingCircumstances extends AbstractMemberNote {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="memberid")
	var member: Member = _

	@NotNull
	@Column(name = "start_date")
	var startDate: LocalDate = _

	@NotNull
	@Column(name = "end_date")
	var endDate: LocalDate = _

}