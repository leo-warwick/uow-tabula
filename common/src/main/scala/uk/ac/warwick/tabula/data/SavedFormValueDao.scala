package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.data.model.forms.{SavedFormValue, FormField}
import uk.ac.warwick.tabula.data.model.{MarkerFeedback, Feedback}
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.Daoisms._
import org.hibernate.criterion.Restrictions.{eq => is}
import uk.ac.warwick.spring.Wire

trait SavedFormValueDao {
	def get(field: FormField, feedback: Feedback): Option[SavedFormValue]
	def get(field: FormField, feedback: MarkerFeedback): Option[SavedFormValue]
}


abstract class AbstractSavedFormValueDao extends SavedFormValueDao {
	self: ExtendedSessionComponent =>

	override def get(field: FormField, feedback: Feedback): Option[SavedFormValue] =
		session.newCriteria[SavedFormValue]
			.add(is("name", field.name))
			.add(is("feedback", feedback))
			.uniqueResult

	override def get(field: FormField, markerFeedback: MarkerFeedback): Option[SavedFormValue] =
		session.newCriteria[SavedFormValue]
			.add(is("name", field.name))
			.add(is("markerFeedback", markerFeedback))
			.uniqueResult

}

@Repository
class SavedFormValueDaoImpl extends AbstractSavedFormValueDao with Daoisms

trait SavedFormValueDaoComponent {
	def savedFormValueDao: SavedFormValueDao
}

trait AutowiringSavedFormValueDaoComponent extends SavedFormValueDaoComponent {
	var savedFormValueDao: SavedFormValueDao = Wire[SavedFormValueDao]
}