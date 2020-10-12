package uk.ac.warwick.tabula.data.model

import javax.persistence.{Entity, Id, NamedQueries, NamedQuery}
import org.apache.commons.lang3.builder.{EqualsBuilder, HashCodeBuilder}
import org.hibernate.annotations.Proxy
import org.joda.time.DateTime

import scala.util.Try

object Level {
  def toYearOfStudy(levelCode: String): Int = Try(levelCode.toInt).toOption.getOrElse(1)
}

@Entity(name = "StudyLevel") // Level is a reserved word in Oracle so the table is called StudyLevel
@Proxy
@NamedQueries(Array(
  new NamedQuery(name = "level.code", query = "select level from StudyLevel level where code = :code")))
class Level {

  def this(code: String = null, name: String = null) = {
    this()
    this.code = code
    this.name = name
  }

  @Id var code: String = _
  var shortName: String = _
  var name: String = _

  var lastUpdatedDate: DateTime = DateTime.now

  def toYearOfStudy: Int = Level.toYearOfStudy(code)

  override def toString: String = name

  override def equals(other: Any): Boolean = other match {
    case that: Level => new EqualsBuilder().append(code, that.code).build()
    case _ => false
  }

  override def hashCode(): Int = new HashCodeBuilder().append(code).build()
}
