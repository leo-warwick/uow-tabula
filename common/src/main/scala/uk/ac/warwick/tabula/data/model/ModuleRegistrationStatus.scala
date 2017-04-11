package uk.ac.warwick.tabula.data.model

import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

sealed abstract class ModuleRegistrationStatus(val dbValue: String, val description: String)

// these are the module registration statuses available in SITS

object ModuleRegistrationStatus {
	case object AwaitingRegistration extends ModuleRegistrationStatus("GEN", "Ready for student to make choices")
	case object AwaitingConfirmation extends ModuleRegistrationStatus("ENT", "Student has made choices; awaiting confirmation")
	case object Confirmed extends ModuleRegistrationStatus("CON", "Confirmed")
	case object PreviouslyConfirmed extends ModuleRegistrationStatus("PCO", "Previously confirmed")
	case object Queried extends ModuleRegistrationStatus("QUE", "Queried")
	case object QueryAnswered extends ModuleRegistrationStatus("ANS", "Query answered")
	case object Rejected extends ModuleRegistrationStatus("REJ", "Rejected")
	case object ModuleRejected extends ModuleRegistrationStatus("MRJ", "Module rejected")
	case object NotAvailable extends ModuleRegistrationStatus("NOTAPP", "Not available")
	case object ChangeRequested extends ModuleRegistrationStatus("CRQ", "Change requested")
	case object ChangeRejected extends ModuleRegistrationStatus("CRJ", "Change rejected")
	case object PostConfirmationQueried extends ModuleRegistrationStatus("PCQ", "Post-confirmation query")
	case object PostConfirmationAnswered extends ModuleRegistrationStatus("PCA", "Post-confirmation answer")

	def fromCode(code: String): ModuleRegistrationStatus = code match {
	  	case AwaitingRegistration.dbValue => AwaitingRegistration
	  	case AwaitingConfirmation.dbValue => AwaitingConfirmation
	  	case Confirmed.dbValue => Confirmed
	  	case PreviouslyConfirmed.dbValue => PreviouslyConfirmed
	  	case Queried.dbValue => Queried
	  	case QueryAnswered.dbValue => QueryAnswered
	  	case Rejected.dbValue => Rejected
	  	case ModuleRejected.dbValue => ModuleRejected
	  	case NotAvailable.dbValue => NotAvailable
	  	case ChangeRequested.dbValue => ChangeRequested
	  	case ChangeRejected.dbValue => ChangeRejected
	  	case PostConfirmationQueried.dbValue => PostConfirmationQueried
	  	case PostConfirmationAnswered.dbValue => PostConfirmationAnswered
	  	case null => null
	  	case _ => throw new IllegalArgumentException()
	}
}

class ModuleRegistrationStatusUserType extends AbstractBasicUserType[ModuleRegistrationStatus, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String): ModuleRegistrationStatus = ModuleRegistrationStatus.fromCode(string)

	override def convertToValue(selectionStatus: ModuleRegistrationStatus): String = selectionStatus.dbValue

}