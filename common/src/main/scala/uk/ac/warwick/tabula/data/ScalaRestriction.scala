package uk.ac.warwick.tabula.data

import org.hibernate.criterion.{Criterion, DetachedCriteria, Restrictions}
import org.hibernate.criterion.Restrictions._
import org.hibernate.sql.JoinType

import scala.collection.mutable
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

class ScalaRestriction(val underlying: Criterion) extends Aliasable {
  def apply[A](c: ScalaCriteria[A]): ScalaCriteria[A] = c.add(this)

  override final def equals(other: Any): Boolean = other match {
    case that: ScalaRestriction =>
      new EqualsBuilder()
        .append(underlying, that.underlying)
        .append(aliases, that.aliases)
        .build()
    case _ => false
  }

  override final def hashCode: Int =
    new HashCodeBuilder()
      .append(underlying)
      .append(aliases)
      .build()

  override final def toString: String =
    new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
      .append("underlying", underlying)
      .append("aliases", aliases)
      .build()
}

object ScalaRestriction {

  import Aliasable._

  def is(property: String, value: Any, aliases: (String, AliasAndJoinType)*): Option[ScalaRestriction] =
    Some(addAliases(new ScalaRestriction(HibernateHelpers.is(property, value)), aliases: _*))

  def isNot(property: String, value: Any, aliases: (String, AliasAndJoinType)*): Option[ScalaRestriction] =
    Some(addAliases(new ScalaRestriction(HibernateHelpers.isNot(property, value)), aliases: _*))

  def isIfTicked(property: String, value: Any, ticked: Boolean, aliases: (String, AliasAndJoinType)*): Option[ScalaRestriction] =
    if (!ticked) None
    else Some(addAliases(new ScalaRestriction(HibernateHelpers.is(property, value)), aliases: _*))

  def isPropertyIfTicked(property1: String, property2: String, ticked: Boolean, aliases: (String, AliasAndJoinType)*): Option[ScalaRestriction] =
    if (!ticked) None
    else Some(addAliases(new ScalaRestriction(org.hibernate.criterion.Restrictions.eqProperty(property1, property2)), aliases: _*))

  def isNotPropertyIfTicked(property1: String, property2: String, ticked: Boolean, aliases: (String, AliasAndJoinType)*): Option[ScalaRestriction] =
    if (!ticked) None
    else Some(addAliases(new ScalaRestriction(org.hibernate.criterion.Restrictions.neProperty(property1, property2)), aliases: _*))

  // if the collection is empty, don't return any restriction - else return the restriction that the property must be in the collection
  def inIfNotEmpty(property: String, collection: Iterable[Any], aliases: (String, AliasAndJoinType)*): Option[ScalaRestriction] =
    if (collection.isEmpty) None
    else Some(addAliases(new ScalaRestriction(HibernateHelpers.safeIn(property, collection.toSeq.distinct)), aliases: _*))

  // If *any* of the properties are empty, don't return any restriction.
  // This was written for the case of module registrations, where we should only check the
  // module registration year if there are modules to restrict by
  def inIfNotEmptyMultipleProperties(properties: Seq[String], collections: Seq[Iterable[Any]], aliases: (String, AliasAndJoinType)*): Option[ScalaRestriction] = {
    val criterion = Restrictions.conjunction()
    (properties, collections).zipped.foreach { (property, collection) =>
      if (collection.nonEmpty) {
        criterion.add(HibernateHelpers.safeIn(property, collection.toSeq.distinct))
      }
    }
    if (!collections.exists(coll => coll.isEmpty)) // if all the collections are non-empty
      Some(addAliases(new ScalaRestriction(criterion), aliases: _*))
    else
      None
  }

  def startsWithIfNotEmpty(property: String, collection: Iterable[String], aliases: (String, AliasAndJoinType)*): Option[ScalaRestriction] =
    if (collection.isEmpty) None
    else {
      val criterion = Restrictions.disjunction()
      collection.toSeq.distinct.foreach { prefix =>
        criterion.add(like(property, prefix + "%"))
      }

      Some(addAliases(new ScalaRestriction(criterion), aliases: _*))
    }

  def atLeastOneIsTrue(property1: String, property2: String, ticked: Boolean, aliases: (String, AliasAndJoinType)*): Option[ScalaRestriction] =
    if (!ticked) None
    else {
      val criterion = Restrictions.disjunction()
      criterion.add(HibernateHelpers.is(property1, true))
      criterion.add(HibernateHelpers.is(property2, true))

      Some(addAliases(new ScalaRestriction(criterion), aliases: _*))
    }

  def neitherIsTrue(property1: String, property2: String, ticked: Boolean, aliases: (String, AliasAndJoinType)*): Option[ScalaRestriction] =
    if (!ticked) None
    else {
      val criterion = Restrictions.conjunction()
      criterion.add(HibernateHelpers.is(property1, false))
      criterion.add(HibernateHelpers.is(property2, false))

      Some(addAliases(new ScalaRestriction(criterion), aliases: _*))
    }

  def gt(property: String, value: Any, aliases: (String, AliasAndJoinType)*): Option[ScalaRestriction] =
    Some(addAliases(new ScalaRestriction(Restrictions.gt(property, value)), aliases: _*))

  def lt(property: String, value: Any, aliases: (String, AliasAndJoinType)*): Option[ScalaRestriction] =
    Some(addAliases(new ScalaRestriction(Restrictions.lt(property, value)), aliases: _*))

  def notEmpty(property: String, aliases: (String, AliasAndJoinType)*): Option[ScalaRestriction] =
    Some(addAliases(new ScalaRestriction(Restrictions.isNotEmpty(property)), aliases: _*))

  def custom(criterion: Criterion, aliases: (String, AliasAndJoinType)*): Option[ScalaRestriction] =
    Some(addAliases(new ScalaRestriction(criterion), aliases: _*))

  def anyOf(restrictions: ScalaRestriction*): Option[ScalaRestriction] = restrictions.toList match {
    case Nil => None
    case custom :: Nil => Some(custom)
    case list =>
      val aliases = list.flatMap(_.aliases).distinct
      val criterion = list.map(_.underlying)

      Some(addAliases(new ScalaRestriction(Restrictions.disjunction(criterion: _*)), aliases: _*))
  }

  def allOf(restrictions: ScalaRestriction*): Option[ScalaRestriction] = restrictions.toList match {
    case Nil => None
    case custom :: Nil => Some(custom)
    case list =>
      val aliases = list.flatMap(_.aliases).distinct
      val criterion = list.map(_.underlying)

      Some(addAliases(new ScalaRestriction(Restrictions.conjunction(criterion: _*)), aliases: _*))
  }

  def applyToDetached(restrictions: Seq[ScalaRestriction], c: DetachedCriteria): DetachedCriteria = {
    val aliases: mutable.Map[String, String] = mutable.Map()
    def createAlias(property: String, alias: String, joinType: JoinType = JoinType.INNER_JOIN, withClause: Option[Criterion] = None): Unit = {
      aliases.put(property, alias) match {
        case None => c.createAlias(property, alias, joinType, withClause.orNull)
        case Some(existing) if existing == alias => // duplicate
        case Some(other) => throw new IllegalArgumentException("Tried to alias %s to %s, but it is already aliased to %s!".format(property, alias, other))
      }
    }

    restrictions.foreach { restriction =>
      restriction.aliases.foreach { case (property, aliasAndJoinType) => createAlias(property, aliasAndJoinType.alias, aliasAndJoinType.joinType, aliasAndJoinType.withClause) }
      c.add(restriction.underlying)
    }

    c
  }

}

trait Aliasable {
  final val aliases: mutable.Map[String, AliasAndJoinType] = mutable.Map()

  def alias(property: String, aliasAndJoinType: AliasAndJoinType): Aliasable = {
    aliases.put(property, aliasAndJoinType) match {
      case Some(other) if other.alias != aliasAndJoinType.alias =>
        // non-duplicate
        throw new IllegalArgumentException("Tried to alias %s to %s, but it is already aliased to %s!".format(property, aliasAndJoinType.alias, other))
      case _ =>
    }
    this
  }
}

object Aliasable {
  def addAliases[A <: Aliasable](aliasable: A, aliases: (String, AliasAndJoinType)*): A = {
    aliases.foreach { case (property, alias) => aliasable.alias(property, alias) }
    aliasable
  }
}

case class AliasAndJoinType(alias: String, joinType: JoinType = JoinType.INNER_JOIN, withClause: Option[Criterion] = None)