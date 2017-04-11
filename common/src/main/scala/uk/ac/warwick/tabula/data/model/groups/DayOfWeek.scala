package uk.ac.warwick.tabula.data.model.groups

import org.joda.time.{DateTime, DateTimeConstants}
import java.sql.Types

import org.hibernate.`type`.StandardBasicTypes
import uk.ac.warwick.tabula.data.model.AbstractBasicUserType
import uk.ac.warwick.tabula.JavaImports._

@SerialVersionUID(-8143257003489402756l) sealed abstract class DayOfWeek(val jodaDayOfWeek: Int) extends Serializable {
	def name: String = toString
	def shortName: String = name.substring(0, 3)

	// For Spring, the silly bum
	def getName: String = name
	def getAsInt: Int = jodaDayOfWeek
}

object DayOfWeek {
	case object Monday extends DayOfWeek(DateTimeConstants.MONDAY)
	case object Tuesday extends DayOfWeek(DateTimeConstants.TUESDAY)
	case object Wednesday extends DayOfWeek(DateTimeConstants.WEDNESDAY)
	case object Thursday extends DayOfWeek(DateTimeConstants.THURSDAY)
	case object Friday extends DayOfWeek(DateTimeConstants.FRIDAY)
	case object Saturday extends DayOfWeek(DateTimeConstants.SATURDAY)
	case object Sunday extends DayOfWeek(DateTimeConstants.SUNDAY)

	// lame manual collection. Keep in sync with the case objects above
	val members = Seq(Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday)
	private[groups] def find(i: Int) = members find { _.jodaDayOfWeek == i }

	def apply(i: Int): DayOfWeek = find(i) getOrElse { throw new IllegalArgumentException(s"Invalid value for day of week: $i") }
	def today: DayOfWeek = DayOfWeek(DateTime.now.dayOfWeek.get)
}

class DayOfWeekUserType extends AbstractBasicUserType[DayOfWeek, JInteger] {

	val basicType = StandardBasicTypes.INTEGER
	override def sqlTypes = Array(Types.INTEGER)

	val nullValue = null
	val nullObject = null

	override def convertToObject(int: JInteger) = DayOfWeek(int)
	override def convertToValue(day: DayOfWeek): JInteger = day.jodaDayOfWeek

}