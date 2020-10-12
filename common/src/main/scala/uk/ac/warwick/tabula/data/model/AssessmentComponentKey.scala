package uk.ac.warwick.tabula.data.model

import java.io.Serializable
import java.sql.{PreparedStatement, ResultSet, Types}

import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.usertype.UserType
import uk.ac.warwick.tabula.ToString

import scala.jdk.CollectionConverters._

object AssessmentComponentKey {
  def apply(ac: AssessmentComponent): AssessmentComponentKey = new AssessmentComponentKey(ac.moduleCode, ac.assessmentGroup, ac.sequence)
  def apply(uag: UpstreamAssessmentGroup): AssessmentComponentKey = new AssessmentComponentKey(uag.moduleCode, uag.assessmentGroup, uag.sequence)
  def unapply(key: AssessmentComponentKey): Option[Seq[String]] = Some(Seq(key.moduleCode, key.assessmentGroup, key.sequence))
}

class AssessmentComponentKey(
  val moduleCode: String,
  val assessmentGroup: String,
  val sequence: String
) extends UserType with ToString {

  def this() = {
    this(null, null, null)
  }

  override def sqlTypes(): Array[Int] = Array(Types.ARRAY)

  override def returnedClass: Class[_] = classOf[AssessmentComponentKey]

  final override def nullSafeGet(resultSet: ResultSet, names: Array[String], impl: SharedSessionContractImplementor, owner: Object): AssessmentComponentKey = {
    Option(resultSet.getArray(names.head))
      .map(_.getArray())
      .map(a => a.asInstanceOf[Array[String]].toSeq)
      .flatMap(s => s match {
        case Seq(mc, ag, s) => Some(new AssessmentComponentKey(mc, ag, s))
        case _ => None
      })
      .orNull
  }

  final override def nullSafeSet(stmt: PreparedStatement, value: Any, index: Int, impl: SharedSessionContractImplementor): Unit = {
    value match {
      case key: AssessmentComponentKey @unchecked =>
        val array = stmt.getConnection.createArrayOf(
          "VARCHAR",
          AssessmentComponentKey.unapply(key).getOrElse(Seq()).asJava.toArray
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
    case other: AssessmentComponentKey => moduleCode == other.moduleCode && assessmentGroup == other.assessmentGroup && sequence == other.sequence
    case _ => false
  }

  override def hashCode(): Int = toString().hashCode()

  override def toStringProps: Seq[(String, Any)] = Seq(
    "moduleCode" -> moduleCode,
    "assessmentGroup" -> assessmentGroup,
    "sequence" -> sequence
  )
}
