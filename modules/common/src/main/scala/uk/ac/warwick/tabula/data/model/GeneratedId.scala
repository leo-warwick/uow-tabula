package uk.ac.warwick.tabula.data.model
import javax.persistence.GeneratedValue
import org.hibernate.annotations.GenericGenerator
import javax.persistence.Id
import scala.beans.BeanProperty
import org.hibernate.proxy.HibernateProxyHelper

/**
 * Provides an autogenerated Hibernate property called "id".
 * Hibernate will generate a UUID when the entity is first saved.
 *
 */
trait GeneratedId {

	@BeanProperty
	@Id
	@GeneratedValue(generator = "system-uuid")
	@GenericGenerator(name = "system-uuid", strategy = "org.hibernate.id.UUIDGenerator")
	var id: String = null

	override final def hashCode = id match {
		case null => super.hashCode
		case str => getClass.hashCode + (41 * str.hashCode)
	}


	override final def equals(other: Any)= other match {
		case that: GeneratedId if sameDomainClass(that) =>
			if (id == null && that.id == null) this.eq(that) // Reference equality
			else id == that.id
		case _ => false
	}


	def sameDomainClass(other:GeneratedId):Boolean = {
		HibernateProxyHelper.getClassWithoutInitializingProxy(this) == HibernateProxyHelper.getClassWithoutInitializingProxy(other)
	}

	override def toString = getClass.getSimpleName + "[" + (if (id != null) id else "(transient " + hashCode + ")") + "]"

}
