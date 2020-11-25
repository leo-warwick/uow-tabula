package uk.ac.warwick.tabula.data

import org.hibernate.FlushMode
import org.hibernate.criterion.Restrictions._
import org.hibernate.criterion.{DetachedCriteria, Order, Projections, Property}
import org.hibernate.sql.JoinType
import org.joda.time.LocalDate
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmissionState.{ApprovedByChair, OutcomesRecorded}
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesMessage, MitigatingCircumstancesNote, MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmissionState}
import uk.ac.warwick.tabula.data.model.{Department, Module, StudentMember}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

trait MitCircsSubmissionDaoComponent {
  val mitCircsSubmissionDao: MitCircsSubmissionDao
}

trait AutowiringMitCircsSubmissionDaoComponent extends MitCircsSubmissionDaoComponent {
  val mitCircsSubmissionDao: MitCircsSubmissionDao = Wire[MitCircsSubmissionDao]
}

trait MitCircsSubmissionDao {
  def getById(id: String): Option[MitigatingCircumstancesSubmission]
  def getByKey(key: Long): Option[MitigatingCircumstancesSubmission]
  def saveOrUpdate(submission: MitigatingCircumstancesSubmission): MitigatingCircumstancesSubmission
  def submissionsForStudent(studentMember: StudentMember): Seq[MitigatingCircumstancesSubmission]
  def submissionsWithOutcomes(students: Seq[StudentMember]): Seq[MitigatingCircumstancesSubmission]
  def submissionsForDepartment(department: Department, studentRestrictions: Seq[ScalaRestriction], filter: MitigatingCircumstancesSubmissionFilter): Seq[MitigatingCircumstancesSubmission]
  def getMessageById(id: String): Option[MitigatingCircumstancesMessage]
  def create(message: MitigatingCircumstancesMessage): MitigatingCircumstancesMessage
  def messagesForSubmission(submission: MitigatingCircumstancesSubmission): Seq[MitigatingCircumstancesMessage]
  def getNoteById(id: String): Option[MitigatingCircumstancesNote]
  def create(note: MitigatingCircumstancesNote): MitigatingCircumstancesNote
  def delete(note: MitigatingCircumstancesNote): MitigatingCircumstancesNote
  def notesForSubmission(submission: MitigatingCircumstancesSubmission): Seq[MitigatingCircumstancesNote]
  def affectedModulesForDepartment(department: Department): Seq[Module]
}

@Repository
class MitCircsSubmissionDaoImpl extends MitCircsSubmissionDao
  with Daoisms with TaskBenchmarking with AutowiringUserLookupComponent {

  override def getById(id: String): Option[MitigatingCircumstancesSubmission] = getById[MitigatingCircumstancesSubmission](id)

  override def getByKey(key: Long): Option[MitigatingCircumstancesSubmission] =
    session.newQuery[MitigatingCircumstancesSubmission]("from MitigatingCircumstancesSubmission where key = :key").setLong("key", key).uniqueResult

  override def saveOrUpdate(submission: MitigatingCircumstancesSubmission): MitigatingCircumstancesSubmission = {
    // fetch a new key if required
    if (submission.key == null) {
      // set the flush mode to commit to avoid TransientObjectExceptions when fetching the next key
      session.setHibernateFlushMode(FlushMode.COMMIT)
      submission.key = session.createNativeQuery("select nextval('mit_circ_sequence')").getSingleResult.asInstanceOf[java.math.BigInteger].longValue
      // set the flush mode back
      session.setHibernateFlushMode(FlushMode.AUTO)
    }
    session.saveOrUpdate(submission)
    submission
  }

  override def submissionsForStudent(studentMember: StudentMember): Seq[MitigatingCircumstancesSubmission] =
    session.newCriteria[MitigatingCircumstancesSubmission]
      .add(is("student", studentMember))
      .addOrder(Order.desc("_lastModified"))
      .seq

  override def submissionsWithOutcomes(students: Seq[StudentMember]): Seq[MitigatingCircumstancesSubmission] =
    if (students.isEmpty) Seq.empty
    else session.newCriteria[MitigatingCircumstancesSubmission]
      .add(safeIn("student", students))
      .add(or(
        is("_state", ApprovedByChair),
        and(isNotNull("acuteOutcome"), is("_state", OutcomesRecorded))
      ))
      .add(isNotNull("outcomeGrading"))
      .seq

  override def submissionsForDepartment(department: Department, studentRestrictions: Seq[ScalaRestriction], filter: MitigatingCircumstancesSubmissionFilter): Seq[MitigatingCircumstancesSubmission] = {
    val c =
      session.newCriteria[MitigatingCircumstancesSubmission]
        .add(is("department", department))

    if (studentRestrictions.nonEmpty) {
      val students =
        DetachedCriteria.forClass(classOf[StudentMember])
          .setProjection(Projections.property("universityId"))

      ScalaRestriction.applyToDetached(studentRestrictions, students)

      c.add(Property.forName("student.universityId").in(students))
    }

    if (filter.affectedAssessmentModules.nonEmpty) {
      c.createAlias("affectedAssessments", "affectedAssessment")
        .add(safeIn("affectedAssessment._module", filter.affectedAssessmentModules.toSeq))
    }

    filter.includesStartDate.foreach { d =>
      c.add(
        or(
          isNull("endDate"),     // Is ongoing, or
          not(lt("endDate", d))  // Doesn't end before this date
        )
      )
    }

    filter.includesEndDate.foreach { d =>
      // Doesn't start after this date, doesn't end before this date
      c.add(
        and(
          not(gt("startDate", d)),
          isNotNull("endDate"),
          not(lt("endDate", d))
        )
      )
    }

    filter.approvedStartDate.foreach { d =>
      // The approved date must not be before this date
      c.add(
        and(
          isNotNull("_approvedOn"),
          not(lt("_approvedOn", d.toDateTimeAtStartOfDay))
        )
      )
    }

    filter.approvedEndDate.foreach { d =>
      // The approved date must not be after this date
      c.add(
        and(
          isNotNull("_approvedOn"),
          not(gt("_approvedOn", d.plusDays(1).toDateTimeAtStartOfDay.minusMillis(1)))
        )
      )
    }

    if (filter.state.nonEmpty) {
      c.add(safeIn("_state", filter.state.toSeq))
    }

    // MCOs can never see drafts
    c.add(isNot("_state", MitigatingCircumstancesSubmissionState.Draft))

    filter.isCoronavirus.foreach { b =>
      c.add(is("covid19Submission", b))
    }

    if(filter.isUnread) {
      c.createAlias("_messages",  "m", joinType = JoinType.LEFT_OUTER_JOIN) // join messages to avoid lazy loading for each submission individually
      c.distinct.seq.sortBy(_.lastModified).reverse.filter(_.isUnreadByOfficer)
    } else {
      c.distinct.seq.sortBy(_.lastModified).reverse
    }
  }

  override def getMessageById(id: String): Option[MitigatingCircumstancesMessage] = getById[MitigatingCircumstancesMessage](id)

  override def create(message: MitigatingCircumstancesMessage): MitigatingCircumstancesMessage = {
    session.saveOrUpdate(message)
    message
  }

  override def messagesForSubmission(submission: MitigatingCircumstancesSubmission): Seq[MitigatingCircumstancesMessage] =
    session.newCriteria[MitigatingCircumstancesMessage]
      .add(is("submission", submission))
      .addOrder(Order.asc("createdDate"))
      .seq

  override def getNoteById(id: String): Option[MitigatingCircumstancesNote] = getById[MitigatingCircumstancesNote](id)

  override def create(note: MitigatingCircumstancesNote): MitigatingCircumstancesNote = {
    session.saveOrUpdate(note)
    note
  }

  override def delete(note: MitigatingCircumstancesNote): MitigatingCircumstancesNote = {
    session.delete(note)
    note
  }

  override def notesForSubmission(submission: MitigatingCircumstancesSubmission): Seq[MitigatingCircumstancesNote] =
    session.newCriteria[MitigatingCircumstancesNote]
      .add(is("submission", submission))
      .addOrder(Order.asc("_createdDate"))
      .seq

  override def affectedModulesForDepartment(department: Department): Seq[Module] =
    session.newCriteria[Module]
      .createAlias("_affectedAssessments", "affectedAssessment")
      .createAlias("affectedAssessment.mitigatingCircumstancesSubmission", "mitigatingCircumstancesSubmission")
      .add(is("mitigatingCircumstancesSubmission.department", department))
      .distinct
      .seq

}

case class MitigatingCircumstancesSubmissionFilter(
  affectedAssessmentModules: Set[Module] = Set.empty,
  includesStartDate: Option[LocalDate] = None,
  includesEndDate: Option[LocalDate] = None,
  approvedStartDate: Option[LocalDate] = None,
  approvedEndDate: Option[LocalDate] = None,
  state: Set[MitigatingCircumstancesSubmissionState] = Set.empty,
  isUnread: Boolean = false,
  isCoronavirus: Option[Boolean] = None
)
