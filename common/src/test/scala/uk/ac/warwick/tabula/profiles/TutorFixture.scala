package uk.ac.warwick.tabula.profiles

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{ProfileService, RelationshipService}
import uk.ac.warwick.userlookup.User

trait TutorFixture extends Mockito {

  val tutorRelationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

  val department = new Department
  department.setStudentRelationshipSource(tutorRelationshipType, StudentRelationshipSource.Local)
  val actor = new User
  val recipient = new User
  recipient.setWarwickId("recipient")

  val student = new StudentMember
  student.universityId = "student"
  student.firstName = "Test"
  student.lastName = "Student"
  student.email = "student@warwick.ac.uk"
  student.inUseFlag = "Active"

  val studentCourseDetails = new StudentCourseDetails
  studentCourseDetails.student = student
  studentCourseDetails.department = department
  studentCourseDetails.sprCode = "0000001/1"
  studentCourseDetails.mostSignificant = true
  student.attachStudentCourseDetails(studentCourseDetails)
  student.mostSignificantCourse = studentCourseDetails

  val newTutor = new StaffMember
  newTutor.firstName = "Peter"
  newTutor.lastName = "O'Hanraha-Hanrahan"
  newTutor.universityId = "0000001"

  val oldTutor = new StaffMember
  oldTutor.universityId = "0000002"

  val mockProfileService: ProfileService = smartMock[ProfileService]
  mockProfileService.getStudentBySprCode("student") returns Some(student)
  mockProfileService.getMemberByUniversityId("0000001") returns Some(newTutor)
  mockProfileService.getMemberByUniversityId("0000002") returns Some(oldTutor)
  mockProfileService.getMemberByUniversityId("0000002", false, false) returns Some(oldTutor)

  val relationship = new MemberStudentRelationship
  relationship.studentMember = student
  relationship.agentMember = newTutor
  relationship.relationshipType = tutorRelationshipType

  val relationshipOld = new MemberStudentRelationship
  relationshipOld.studentMember = student
  relationshipOld.agentMember = oldTutor
  relationshipOld.relationshipType = tutorRelationshipType

  val mockRelationshipService: RelationshipService = smartMock[RelationshipService]
  mockRelationshipService.saveStudentRelationship(
    ArgumentMatchers.eq(tutorRelationshipType),
    ArgumentMatchers.eq(studentCourseDetails),
    ArgumentMatchers.eq(Left(newTutor)),
    ArgumentMatchers.eq(DateTime.now),
    any[Seq[StudentRelationship]]
  ) returns StudentRelationship(newTutor, tutorRelationshipType, studentCourseDetails, DateTime.now)
  mockRelationshipService.findCurrentRelationships(tutorRelationshipType, studentCourseDetails) returns Seq(relationshipOld)

}
