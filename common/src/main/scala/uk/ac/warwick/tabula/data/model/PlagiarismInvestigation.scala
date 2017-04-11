package uk.ac.warwick.tabula.data.model

import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

sealed abstract class PlagiarismInvestigation(val dbValue: String)

object PlagiarismInvestigation {

	case object NotInvestigated extends PlagiarismInvestigation("NotInvestigated")
	case object SuspectPlagiarised  extends PlagiarismInvestigation("SuspectPlagiarised")
	case object InvestigationCompleted extends PlagiarismInvestigation("InvestigationCompleted")

	val Default = NotInvestigated

	// lame manual collection. Keep in sync with the case objects above
	val members = Seq(NotInvestigated, SuspectPlagiarised, InvestigationCompleted)

	def fromDatabase(dbValue: String): PlagiarismInvestigation ={
		if (dbValue == null) null
		else members.find{_.dbValue == dbValue} match {
			case Some(caseObject) => caseObject
			case None => throw new IllegalArgumentException()
		}
	}

	def apply(value:String): PlagiarismInvestigation = fromDatabase(value)
}


class PlagiarismInvestigationUserType extends AbstractBasicUserType[PlagiarismInvestigation, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String): PlagiarismInvestigation = PlagiarismInvestigation.fromDatabase(string)
	override def convertToValue(method: PlagiarismInvestigation): String = method.dbValue
}