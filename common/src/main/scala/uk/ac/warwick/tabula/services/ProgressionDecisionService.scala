package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.ProgressionDecision
import uk.ac.warwick.tabula.data.{AutowiringProgressionDecisionDaoComponent, AutowiringTransactionalComponent, ProgressionDecisionDaoComponent, TransactionalComponent}

trait ProgressionDecisionService {
  def saveOrUpdate(pd: ProgressionDecision): Unit
  def delete(pd: ProgressionDecision): Unit
  def getByAcademicYears(academicYears: Seq[AcademicYear]): Seq[ProgressionDecision]
  def getByUniversityIds(universityIds: Seq[String]): Seq[ProgressionDecision]
  def getAgreedByAcademicYears(academicYears: Seq[AcademicYear]): Seq[ProgressionDecision]
  def getAgreedByUniversityIds(universityIds: Seq[String]): Seq[ProgressionDecision]
}

abstract class DatabaseProgressionDecisionService extends ProgressionDecisionService {
  self: ProgressionDecisionDaoComponent
    with TransactionalComponent =>

  override def saveOrUpdate(pd: ProgressionDecision): Unit = transactional() {
    progressionDecisionDao.saveOrUpdate(pd)
  }

  override def delete(pd: ProgressionDecision): Unit = transactional() {
    progressionDecisionDao.delete(pd)
  }

  override def getByAcademicYears(academicYears: Seq[AcademicYear]): Seq[ProgressionDecision] = transactional(readOnly = true) {
    progressionDecisionDao.getByAcademicYears(academicYears)
  }

  override def getByUniversityIds(universityIds: Seq[String]): Seq[ProgressionDecision] = transactional(readOnly = true) {
    progressionDecisionDao.getByUniversityIds(universityIds)
  }

  override def getAgreedByAcademicYears(academicYears: Seq[AcademicYear]): Seq[ProgressionDecision] = transactional(readOnly = true) {
    progressionDecisionDao.getAgreedByAcademicYears(academicYears)
  }

  override def getAgreedByUniversityIds(universityIds: Seq[String]): Seq[ProgressionDecision] = transactional(readOnly = true) {
    progressionDecisionDao.getAgreedByUniversityIds(universityIds)
  }

}

@Service("progressionDecisionService")
class AutowiringProgressionDecisionService
  extends DatabaseProgressionDecisionService
    with AutowiringProgressionDecisionDaoComponent
    with AutowiringTransactionalComponent

trait ProgressionDecisionServiceComponent {
  def progressionDecisionService: ProgressionDecisionService
}

trait AutowiringProgressionDecisionServiceComponent extends ProgressionDecisionServiceComponent {
  var progressionDecisionService: ProgressionDecisionService = Wire[ProgressionDecisionService]
}

