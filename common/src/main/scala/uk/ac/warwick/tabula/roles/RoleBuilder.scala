package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.ScopelessPermission
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import org.apache.commons.lang3.builder.{ToStringStyle, ToStringBuilder, HashCodeBuilder, EqualsBuilder}

object RoleBuilder {
	def build(definition: RoleDefinition, scope: Option[PermissionsTarget], name: String) =
		new GeneratedRole(definition, scope, name)

	class GeneratedRole(definition: RoleDefinition, scope: Option[PermissionsTarget], val name: String) extends Role(definition, scope) {
		override def getName: String = name

		override final def equals(o: Any): Boolean = o match {
			case other: GeneratedRole =>
				new EqualsBuilder()
					.append(definition, other.definition)
					.append(scope, other.scope)
					.append(name, other.name)
					.build()
			case _ => false
		}

		override final def hashCode: Int =
			new HashCodeBuilder()
				.append(definition)
				.append(scope)
				.append(name)
				.build()

		override final def toString: String =
			new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
				.append("definition", definition)
				.append("scope", scope)
				.append("name", name)
				.build()
	}
}