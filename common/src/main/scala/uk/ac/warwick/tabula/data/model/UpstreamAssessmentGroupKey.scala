package uk.ac.warwick.tabula.data.model

import java.io.Serializable
import java.sql.{PreparedStatement, ResultSet, Types}

import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.usertype.UserType
import uk.ac.warwick.tabula.{AcademicYear, ToString}

import scala.jdk.CollectionConverters._

object UpstreamAssessmentGroupKey {
  def unapply(key: UpstreamAssessmentGroupKey): (String, String, String, String) = (key.moduleCode, key.academicYear.startYear.toString, key.sequence, key.occurrence)
}

class UpstreamAssessmentGroupKey (
  var moduleCode: String,
  var academicYear: AcademicYear,
  var sequence: String,
  var occurrence: String
) extends UserType with ToString {

  def this() = {
    this(null, null, null, null)
  }

  override def sqlTypes(): Array[Int] = Array(Types.ARRAY)

  override def returnedClass: Class[_] = classOf[UpstreamAssessmentGroupKey]

  final override def nullSafeGet(resultSet: ResultSet, names: Array[String], impl: SharedSessionContractImplementor, owner: Object): UpstreamAssessmentGroupKey = {
    Option(resultSet.getArray(names.head))
      .map(_.getArray())
      .map(a => a.asInstanceOf[Array[AnyRef]].toSeq)
      .flatMap(s => s match {
        case Seq(mc: String, ay: AcademicYear, s: String, o: String) => Some(new UpstreamAssessmentGroupKey(mc, ay, s, o))
        case _ => None
      })
      .orNull
  }

  final override def nullSafeSet(stmt: PreparedStatement, value: Any, index: Int, impl: SharedSessionContractImplementor): Unit = {
    value match {
      case key: UpstreamAssessmentGroupKey @unchecked =>
        val array = stmt.getConnection.createArrayOf(
          "VARCHAR",
          UpstreamAssessmentGroupKey.unapply(key).productIterator.toSeq.asJava.toArray
        )
        stmt.setArray(index, array)

      case _ => stmt.setNull(index, Types.ARRAY)
    }
  }

  override def isMutable = false

  override def equals(x: Object, y: Object): Boolean = x == y

  override def hashCode(x: Object): Int = Option(x).getOrElse("").hashCode

  override def deepCopy(x: Object): Object = x

  override def replace(original: Object, target: Object, owner: Object): Object = original

  override def disassemble(value: Object): Serializable = value.asInstanceOf[java.io.Serializable]

  override def assemble(cached: java.io.Serializable, owner: Object): AnyRef = cached.asInstanceOf[AnyRef]

  override def equals(that: Any): Boolean = that match {
    case other: UpstreamAssessmentGroupKey =>
      moduleCode == other.moduleCode &&
      academicYear == other.academicYear &&
      sequence == other.sequence &&
      occurrence == other.occurrence
    case _ => false
  }

  override def hashCode(): Int = toString().hashCode()

  override def toStringProps: Seq[(String, Any)] = Seq(
    "moduleCode" -> moduleCode,
    "academicYear" -> academicYear.toString,
    "sequence" -> sequence,
    "occurrence" -> occurrence
  )
}
