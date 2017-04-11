package uk.ac.warwick.tabula.data.model

import java.sql.Types
import javax.persistence._
import javax.validation.constraints.NotNull

import org.hibernate.annotations.Type
import org.joda.time.DateTime

@Entity @Access(AccessType.FIELD)
class Mark extends GeneratedId {

	@OneToOne(fetch = FetchType.LAZY, optional = false, cascade=Array())
	@JoinColumn(name = "feedback_id", nullable = false)
	@ForeignKey(name = "none")
	var feedback: Feedback = _

	@NotNull
	var uploaderId: String = _

	@NotNull
	var uploadedDate: DateTime = _

	@Column(name="marktype")
	@Type(`type` = "uk.ac.warwick.tabula.data.model.MarkTypeUserType")
	var markType: MarkType = _

	@NotNull
	var mark: Int = _

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
	var grade: Option[String] = None

	@NotNull
	var reason: String = _

	var comments: String = _

}

sealed abstract class MarkType(val code: String, val description: String) {
	// for Spring
	def getCode: String = code
	def getDescription: String = description

	override def toString: String = description
}

object MarkType {
	case object Adjustment extends MarkType("adjustment", "Adjustment")
	case object PrivateAdjustment extends MarkType("private", "Private Adjustment")

	// manual collection - keep in sync with the case objects above
	val members = Seq(Adjustment, PrivateAdjustment)

	def fromCode(code: String): MarkType =
		if (code == null) null
		else members.find{_.code == code} match {
			case Some(caseObject) => caseObject
			case None => throw new IllegalArgumentException()
		}

	def fromDescription(description: String): MarkType =
		if (description == null) null
		else members.find{_.description == description} match {
			case Some(caseObject) => caseObject
			case None => throw new IllegalArgumentException()
		}
}

class MarkTypeUserType extends AbstractStringUserType[MarkType] {

	override def sqlTypes = Array(Types.VARCHAR)
	override def convertToObject(string: String): MarkType = MarkType.fromCode(string)
	override def convertToValue(format: MarkType): String = format.code
}