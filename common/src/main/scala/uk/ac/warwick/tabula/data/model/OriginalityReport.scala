package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.Type
import javax.persistence._
import org.joda.time.DateTime
import javax.persistence.Column
import uk.ac.warwick.tabula.JavaImports._

@Entity
class OriginalityReport extends GeneratedId with ToEntityReference {
	type Entity = OriginalityReport

	// Don't cascade as this is the wrong side of the association
	@OneToOne(optional = false, cascade=Array(), fetch = FetchType.LAZY)
	@JoinColumn(name="ATTACHMENT_ID")
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

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
	var similarity: Option[Int] = None

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

	// Urkund data

	var nextSubmitAttempt: DateTime = _

	var submitAttempts: JInteger = 0

	var submittedDate: DateTime = _

	var nextResponseAttempt: DateTime = _

	var responseAttempts: JInteger = 0

	var responseReceived: DateTime = _

	var reportUrl: String = _

	var significance: JFloat = _

	var matchCount: JInteger = _

	var sourceCount: JInteger = _

	@Lob
	var urkundResponse: String = _

	var urkundResponseCode: String = _


	override def toEntityReference: OriginalityReportEntityReference = new OriginalityReportEntityReference().put(this)
}