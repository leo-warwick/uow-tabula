package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.forms.{Extension, ExtensionState}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Assignment
import org.hibernate.criterion.Restrictions._
import org.hibernate.criterion.Projections._
import uk.ac.warwick.userlookup.User

trait ExtensionDaoComponent {
	val extensionDao: ExtensionDao
}

trait AutowiringExtensionDaoComponent extends ExtensionDaoComponent {
	val extensionDao: ExtensionDao = Wire[ExtensionDao]
}

trait ExtensionDao {
	def getExtensionById(id: String): Option[Extension]
	def saveOrUpdate(extension: Extension)
	def countExtensions(assignment: Assignment): Int
	def countUnapprovedExtensions(assignment: Assignment): Int
	def getUnapprovedExtensions(assignment: Assignment): Seq[Extension]
	def getPreviousExtensions(user: User): Seq[Extension]
	def filterExtensions(restrictions: Seq[ScalaRestriction], orders: Seq[ScalaOrder], maxResults: Int, startResult: Int): Seq[Extension]
	def countFilteredExtensions(restrictions: Seq[ScalaRestriction]): Int
}

@Repository
class ExtensionDaoImpl extends ExtensionDao with Daoisms {
	def getExtensionById(id: String): Option[Extension] = getById[Extension](id)

	def saveOrUpdate(extension: Extension): Unit = session.saveOrUpdate(extension)

	def countExtensions(assignment: Assignment): Int = {
		session.newCriteria[Extension]
			.add(is("assignment", assignment))
			.count.intValue
	}

	private def unapprovedExtensionsCriteria(assignment: Assignment) = session.newCriteria[Extension]
		.add(is("assignment", assignment))
		.add(
			and(
				isNotNull("requestedOn"),
				is("_state", ExtensionState.Unreviewed)
			)
		)

	def countUnapprovedExtensions(assignment: Assignment): Int = {
		unapprovedExtensionsCriteria(assignment)
			.count.intValue
	}

	def getUnapprovedExtensions(assignment: Assignment): Seq[Extension] = {
		unapprovedExtensionsCriteria(assignment).seq
	}

	def getPreviousExtensions(user: User): Seq[Extension] = {
		session.newCriteria[Extension]
			.add(is("usercode", user.getUserId))
			.seq
	}

	def filterExtensions(restrictions: Seq[ScalaRestriction], orders: Seq[ScalaOrder], maxResults: Int, startResult: Int): Seq[Extension] = {
		val c = session.newCriteria[Extension]
		restrictions.foreach { _.apply(c) }
		orders.foreach { c.addOrder }
		c.setMaxResults(maxResults).setFirstResult(startResult).seq
	}

	def countFilteredExtensions(restrictions: Seq[ScalaRestriction]): Int = {
		val c = session.newCriteria[Extension]
		restrictions.foreach { _.apply(c) }
		c.count.intValue()
	}
}
