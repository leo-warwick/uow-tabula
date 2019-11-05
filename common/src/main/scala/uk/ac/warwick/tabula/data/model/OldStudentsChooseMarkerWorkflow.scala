package uk.ac.warwick.tabula.data.model

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.MarkingMethod.StudentsChooseMarker
import uk.ac.warwick.tabula.services.{SubmissionService, UserLookupService}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

@Entity
@Proxy
@DiscriminatorValue(value = "StudentsChooseMarker")
class OldStudentsChooseMarkerWorkflow extends MarkingWorkflow with NoSecondMarker {

  def this(dept: Department) = {
    this()
    this.department = dept
  }

  @transient var submissionService: SubmissionService = Wire[SubmissionService]
  @transient var userLookupService: UserLookupService = Wire[UserLookupService]

  def markingMethod = StudentsChooseMarker

  override def studentsChooseMarker = true

  def getStudentsFirstMarker(assessment: Assessment, usercode: String): Option[String] = assessment match {
    case exam: Exam => None
    case assignment: Assignment => assignment.markerSelectField.flatMap { field =>
      val submission = submissionService.getSubmissionByUsercode(assignment, usercode)
      submission.flatMap(_.getValue(field).map(_.value))
    }
  }

  def getMarkersStudents(assessment: Assessment, user: User): Set[User] = assessment match {
    case assignment: Assignment => getSubmissions(assignment, user).map(_.usercode).toSet.map(userLookupService.getUserByUserId)
    case _ => Set.empty
  }


  def getSubmissions(assignment: Assignment, user: User): Seq[Submission] = assignment.markerSelectField.map { markerField =>
    val releasedSubmission = assignment.submissions.asScala.toSeq.filter(s => assignment.isReleasedForMarking(s.usercode))
    releasedSubmission.filter(submission => {
      submission.getValue(markerField) match {
        case Some(subValue) => user.getUserId == subValue.value
        case None => false
      }
    })
  }.getOrElse(Nil)


}
