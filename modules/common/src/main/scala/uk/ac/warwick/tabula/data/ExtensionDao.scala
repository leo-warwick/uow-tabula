package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.forms.{ExtensionState, Extension}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Assignment
import org.hibernate.criterion.Restrictions._
import org.hibernate.criterion.Projections._

trait ExtensionDaoComponent {
	val extensionDao: ExtensionDao
}

trait AutowiringExtensionDaoComponent extends ExtensionDaoComponent {
	val extensionDao = Wire[ExtensionDao]
}

trait ExtensionDao {
	def getExtensionById(id: String): Option[Extension]
	def saveOrUpdate(extension: Extension)
	def delete(extension: Extension)
	def countExtensions(assignment: Assignment): Int
	def countUnapprovedExtensions(assignment: Assignment): Int
	def getUnapprovedExtensions(assignment: Assignment): Seq[Extension]
}

@Repository
class ExtensionDaoImpl extends ExtensionDao with Daoisms {
	def getExtensionById(id: String) = session.getById[Extension](id)

	def saveOrUpdate(extension: Extension) = session.saveOrUpdate(extension)
	def delete(extension: Extension) = session.delete(extension)

	def countExtensions(assignment: Assignment): Int = {
		session.newCriteria[Extension]
			.add(is("assignment", assignment))
			.project[Number](rowCount())
			.uniqueResult.get.intValue()
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
			.project[Number](rowCount())
			.uniqueResult.get.intValue()
	}

	def getUnapprovedExtensions(assignment: Assignment): Seq[Extension] = {
		unapprovedExtensionsCriteria(assignment).seq
	}
}
