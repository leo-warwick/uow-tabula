package uk.ac.warwick.tabula.services.mitcircs

import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.mitcircs.{AssessmentSpecificRecommendation, MitCircsExamBoardRecommendation, MitigatingCircumstancesAffectedAssessment, MitigatingCircumstancesMessage, MitigatingCircumstancesNote, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.data.model.{Department, Module, StudentMember}
import uk.ac.warwick.tabula.data.{AutowiringMitCircsSubmissionDaoComponent, MitCircsSubmissionDaoComponent, MitigatingCircumstancesSubmissionFilter, ScalaRestriction}
import uk.ac.warwick.tabula.services.UserLookupService.UniversityId

trait MitCircsSubmissionService {
  def getById(id: String): Option[MitigatingCircumstancesSubmission]
  def getByKey(key: Long): Option[MitigatingCircumstancesSubmission]
  def saveOrUpdate(submission: MitigatingCircumstancesSubmission): MitigatingCircumstancesSubmission
  def submissionsForStudent(studentMember: StudentMember): Seq[MitigatingCircumstancesSubmission]
  def submissionsWithOutcomes(studentMember: StudentMember): Seq[MitigatingCircumstancesSubmission]
  def submissionsForDepartment(department: Department, studentRestrictions: Seq[ScalaRestriction], filter: MitigatingCircumstancesSubmissionFilter): Seq[MitigatingCircumstancesSubmission]
  def submissionsForStudents(students: Seq[StudentMember]): Map[UniversityId, Seq[MitigatingCircumstancesSubmission]]
  def getMessageById(id: String): Option[MitigatingCircumstancesMessage]
  def create(message: MitigatingCircumstancesMessage): MitigatingCircumstancesMessage
  def messagesForSubmission(submission: MitigatingCircumstancesSubmission): Seq[MitigatingCircumstancesMessage]
  def getNoteById(id: String): Option[MitigatingCircumstancesNote]
  def create(note: MitigatingCircumstancesNote): MitigatingCircumstancesNote
  def delete(note: MitigatingCircumstancesNote): MitigatingCircumstancesNote
  def notesForSubmission(submission: MitigatingCircumstancesSubmission): Seq[MitigatingCircumstancesNote]
  def affectedModulesForDepartment(department: Department): Seq[Module]
}

abstract class AbstractMitCircsSubmissionService extends MitCircsSubmissionService {
  self: MitCircsSubmissionDaoComponent =>

  override def getById(id: String): Option[MitigatingCircumstancesSubmission] = transactional(readOnly = true) {
    mitCircsSubmissionDao.getById(id)
  }

  override def getByKey(key: Long): Option[MitigatingCircumstancesSubmission] = transactional(readOnly = true) {
    mitCircsSubmissionDao.getByKey(key)
  }

  override def saveOrUpdate(submission: MitigatingCircumstancesSubmission): MitigatingCircumstancesSubmission = transactional() {
    mitCircsSubmissionDao.saveOrUpdate(submission)
  }

  override def submissionsForStudent(studentMember: StudentMember): Seq[MitigatingCircumstancesSubmission] = transactional(readOnly = true) {
    mitCircsSubmissionDao.submissionsForStudent(studentMember)
  }

  override def submissionsWithOutcomes(studentMember: StudentMember): Seq[MitigatingCircumstancesSubmission] = {
    mitCircsSubmissionDao.submissionsWithOutcomes(Seq(studentMember))
  }

  override def submissionsForDepartment(department: Department, studentRestrictions: Seq[ScalaRestriction], filter: MitigatingCircumstancesSubmissionFilter): Seq[MitigatingCircumstancesSubmission] = transactional(readOnly = true) {
    mitCircsSubmissionDao.submissionsForDepartment(department, studentRestrictions, filter)
  }

  override def submissionsForStudents(students: Seq[StudentMember]): Map[UniversityId, Seq[MitigatingCircumstancesSubmission]] = transactional(readOnly = true) {
    mitCircsSubmissionDao.submissionsWithOutcomes(students)
      // mitCircsSubmissionDao.submissionsWithOutcomes only returns submission with a grade so the get is fine here
      .map(s => s.student.universityId -> s)
      .groupBy(_._1)
      .view
      .mapValues(_.map(_._2))
      .toMap
  }

  override def getMessageById(id: String): Option[MitigatingCircumstancesMessage] = transactional(readOnly = true) {
    mitCircsSubmissionDao.getMessageById(id)
  }

  override def create(message: MitigatingCircumstancesMessage): MitigatingCircumstancesMessage = transactional() {
    mitCircsSubmissionDao.create(message)
  }

  override def messagesForSubmission(submission: MitigatingCircumstancesSubmission): Seq[MitigatingCircumstancesMessage] = transactional(readOnly = true) {
    mitCircsSubmissionDao.messagesForSubmission(submission)
  }

  override def getNoteById(id: String): Option[MitigatingCircumstancesNote] = transactional(readOnly = true) {
    mitCircsSubmissionDao.getNoteById(id)
  }

  override def create(note: MitigatingCircumstancesNote): MitigatingCircumstancesNote = transactional() {
    mitCircsSubmissionDao.create(note)
  }

  override def delete(note: MitigatingCircumstancesNote): MitigatingCircumstancesNote = transactional() {
    mitCircsSubmissionDao.delete(note)
  }

  override def notesForSubmission(submission: MitigatingCircumstancesSubmission): Seq[MitigatingCircumstancesNote] = transactional(readOnly = true) {
    mitCircsSubmissionDao.notesForSubmission(submission)
  }

  override def affectedModulesForDepartment(department: Department): Seq[Module] =  transactional(readOnly = true) {
    mitCircsSubmissionDao.affectedModulesForDepartment(department)
  }
}

case class MitigationGridInfo (
  code: String,
  globalRecommendations: Seq[MitCircsExamBoardRecommendation],
  assessmentSpecificRecommendations: Map[AssessmentSpecificRecommendation, Seq[MitigatingCircumstancesAffectedAssessment]],
  date: DateTime
)

@Service("mitCircsSubmissionService")
class AutowiredMitCircsSubmissionService
  extends AbstractMitCircsSubmissionService
    with AutowiringMitCircsSubmissionDaoComponent

trait MitCircsSubmissionServiceComponent {
  def mitCircsSubmissionService: MitCircsSubmissionService
}

trait AutowiringMitCircsSubmissionServiceComponent extends MitCircsSubmissionServiceComponent {
  var mitCircsSubmissionService: MitCircsSubmissionService = Wire[MitCircsSubmissionService]
}
