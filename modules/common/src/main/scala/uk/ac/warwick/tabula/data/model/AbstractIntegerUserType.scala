package uk.ac.warwick.tabula.data.model

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

import org.hibernate.`type`.StandardBasicTypes

import uk.ac.warwick.tabula.JavaImports._

/**
 * Handles a lot of the junk that isn't necessary if all you want to do is
 * convert between a class and a number.
 */
abstract class AbstractIntegerUserType[A <: Object: ClassManifest] extends AbstractBasicUserType[A, JInteger] {

	val basicType = StandardBasicTypes.INTEGER

	override def returnedClass = classOf[JInteger]
	override def sqlTypes = Array(Types.INTEGER)

}