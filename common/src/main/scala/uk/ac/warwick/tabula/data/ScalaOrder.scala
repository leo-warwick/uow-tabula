package uk.ac.warwick.tabula.data

import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.EqualsBuilder
import org.hibernate.criterion.Order
import org.apache.commons.lang3.builder.ToStringStyle

class ScalaOrder(val underlying: Order) extends Aliasable {
	def apply[A](c: ScalaCriteria[A]): ScalaCriteria[A] = c.addOrder(this)

	override final def equals(other: Any): Boolean = other match {
		case that: ScalaOrder =>
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

object ScalaOrder {
	import Aliasable._

	def apply(underlying: Order, aliases: (String, AliasAndJoinType)*): ScalaOrder =
		addAliases(new ScalaOrder(underlying), aliases: _*)

	def asc(property: String, aliases: (String, AliasAndJoinType)*): ScalaOrder =
		addAliases(new ScalaOrder(Order.asc(property)), aliases: _*)

	def desc(property: String, aliases: (String, AliasAndJoinType)*): ScalaOrder =
		addAliases(new ScalaOrder(Order.desc(property)), aliases: _*)
}