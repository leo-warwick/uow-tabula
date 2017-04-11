package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.{Award, AccreditedPriorLearning, StudentCourseDetails}
import uk.ac.warwick.spring.Wire

trait AccreditedPriorLearningDaoComponent {
	val accreditedPriorLearningDao: AccreditedPriorLearningDao
}

trait AutowiringAccreditedPriorLearningDaoComponent extends AccreditedPriorLearningDaoComponent {
	val accreditedPriorLearningDao: AccreditedPriorLearningDao = Wire[AccreditedPriorLearningDao]
}

trait AccreditedPriorLearningDao {
	def saveOrUpdate(accreditedPriorLearning: AccreditedPriorLearning)
	def getByNotionalKey(studentCourseDetails: StudentCourseDetails, award: Award, sequenceNumber: Integer): Option[AccreditedPriorLearning]
}

@Repository
class AccreditedPriorLearningDaoImpl extends AccreditedPriorLearningDao with Daoisms {

	def saveOrUpdate(accreditedPriorLearning: AccreditedPriorLearning): Unit = session.saveOrUpdate(accreditedPriorLearning)

	def getByNotionalKey(studentCourseDetails: StudentCourseDetails, award: Award, sequenceNumber: Integer): Option[AccreditedPriorLearning] =
		session.newCriteria[AccreditedPriorLearning]
			.add(is("studentCourseDetails", studentCourseDetails))
			.add(is("award", award))
			.add(is("sequenceNumber", sequenceNumber))
			.uniqueResult

}
