package uk.ac.warwick.courses.data
import uk.ac.warwick.courses.data.model.Feedback
import org.springframework.stereotype.Repository
import uk.ac.warwick.courses.data.model.Assignment
import org.hibernate.criterion.{Restrictions => is}

trait FeedbackDao {
	def getFeedback(id:String): Option[Feedback]
	def getFeedbackByUniId(assignment:Assignment, uniId:String): Option[Feedback]
}

@Repository
class FeedbackDaoImpl extends FeedbackDao with Daoisms {
	
	private val clazz = classOf[Feedback].getName
	
	override def getFeedback(id:String) = option[Feedback](session.get(clazz, id))
	
	override def getFeedbackByUniId(assignment:Assignment, uniId:String): Option[Feedback] = option[Feedback](
			session.createCriteria(clazz)
				.add(is eq("universityId", uniId))
				.add(is eq("assignment", assignment))
				.uniqueResult() 
		)
}