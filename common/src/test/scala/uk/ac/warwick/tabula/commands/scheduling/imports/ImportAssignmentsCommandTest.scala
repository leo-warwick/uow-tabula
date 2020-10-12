package uk.ac.warwick.tabula.commands.scheduling.imports

import org.hibernate.Session
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.scheduling.AssignmentImporter
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, FeedbackService, ModuleAndDepartmentService}
import uk.ac.warwick.tabula.{AcademicYear, CustomHamcrestMatchers, Mockito}
import uk.ac.warwick.userlookup.User

import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters._

@RunWith(classOf[JUnitRunner])
class ImportAssignmentsCommandTest extends AnyFlatSpec with Matchers with Mockito {

  trait Fixture {
    val mockSession: Session = smartMock[Session]
    val importer: AssignmentImporter = smartMock[AssignmentImporter]
    val membershipService: AssessmentMembershipService = smartMock[AssessmentMembershipService]
    val moduleService: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]
    val command = new ImportAssignmentsCommand with ImportAssignmentsAllMembers {
      def session: Session = mockSession

      override def assignmentImporter: AssignmentImporter = importer
      override def yearsToImport: Seq[AcademicYear] = Seq(AcademicYear(2013))
    }
    command.assessmentMembershipService = membershipService
    command.moduleAndDepartmentService = moduleService

    moduleService.getModuleByCode(any[String]) returns None // Not necessary for this to work

    membershipService.getUpstreamAssessmentGroup(any[UpstreamAssessmentGroup], anyBoolean) answers { args: Array[AnyRef] =>
      val uag = args(0).asInstanceOf[UpstreamAssessmentGroup]
      uag.id = "seenGroupId"
      Some(uag)
    }

    membershipService.replaceMembers(any[UpstreamAssessmentGroup], any[Seq[UpstreamAssessmentRegistration]], any[UpstreamAssessmentGroupMemberAssessmentType]) answers { args: Array[AnyRef] =>
      val uag = args(0).asInstanceOf[UpstreamAssessmentGroup]
      uag.id = "seenGroupId"
      val registrations = args(1).asInstanceOf[Seq[Any]].map(_.asInstanceOf[UpstreamAssessmentRegistration])
      uag.replaceMembers(registrations.map(r => (r.universityId, r.resitSequence.maybeText)), args(2).asInstanceOf[UpstreamAssessmentGroupMemberAssessmentType])
      uag
    }

    val registrations: Seq[UpstreamAssessmentRegistration]

    importer.allMembers(any[UpstreamAssessmentGroupMemberAssessmentType], any[Seq[AcademicYear]])(any[UpstreamAssessmentRegistration => Unit]) answers { args: Array[AnyRef] =>
      args match {
        case Array(UpstreamAssessmentGroupMemberAssessmentType.OriginalAssessment, _, fn: (UpstreamAssessmentRegistration => Unit) @unchecked) => registrations.foreach(fn)
        case Array(UpstreamAssessmentGroupMemberAssessmentType.Reassessment, _, _) =>
      }
    }

    membershipService.getAssessmentComponents("HI33M-30", inUseOnly = false) returns Seq(
      new AssessmentComponent {
        assessmentGroup = "A"
        sequence = "A01"
      },
      new AssessmentComponent {
        assessmentGroup = "A"
        sequence = "A02"
      }
    )
    membershipService.getAssessmentComponents("HI100-30", inUseOnly = false) returns Seq(
      new AssessmentComponent {
        assessmentGroup = "A"
        sequence = "A01"
      }
    )
    membershipService.getAssessmentComponents("HI101-30", inUseOnly = false) returns Seq(
      new AssessmentComponent {
        assessmentGroup = "A"
        sequence = "A01"
      }
    )

    val hi900_30: UpstreamAssessmentGroup = {
      val g = new UpstreamAssessmentGroup
      g.id = "hi900_30"
      g.moduleCode = "HI900-30"
      g.occurrence = "A"
      g.assessmentGroup = "A"
      g.academicYear = AcademicYear.parse("13/14")
      g
    }
  }

  behavior of "doGroupMembers"

  it should "process all collections" in {
    new Fixture {
      membershipService.getUpstreamAssessmentGroupsNotIn(isEq(Seq("seenGroupId")), any[Seq[AcademicYear]]) returns Nil

      val registrations = Seq(
        UpstreamAssessmentRegistration("13/14", "0100001/1", "1", "A", "A01", "HI33M-30", "A", "", "", "", "", null, null, None, None, None),
        UpstreamAssessmentRegistration("13/14", "0100001/1", "1", "A", "A01", "HI100-30", "A", "", "", "", "", null, null, None, None, None),
        UpstreamAssessmentRegistration("13/14", "0100002/1", "2", "A", "A01", "HI101-30", "A", "", "", "", "", null, null, None, None, None)
      )
      command.doGroupMembers()
      verify(membershipService, times(5)).replaceMembers(any[UpstreamAssessmentGroup], any[Seq[UpstreamAssessmentRegistration]], any[UpstreamAssessmentGroupMemberAssessmentType])
    }
  }

  /**
    * TAB-1265
    */
  it should "process empty groups" in {
    new Fixture {
      val registrations = Seq(
        UpstreamAssessmentRegistration("13/14", "0100001/1", "1", "A", "A01", "HI33M-30", "A", "", "", "", "", null, null, None, None, None),
        UpstreamAssessmentRegistration("13/14", "0100002/1", "2", "A", "A01", "HI33M-30", "A", "", "", "", "", null, null, None, None, None),
        UpstreamAssessmentRegistration("13/14", "0100003/1", "3", "A", "A01", "HI100-30", "A", "", "", "", "", null, null, None, None, None),
        UpstreamAssessmentRegistration("13/14", "0100002/1", "2", "A", "A01", "HI100-30", "A", "", "", "", "", null, null, None, None, None)
      )

      membershipService.getUpstreamAssessmentGroupsNotIn(isEq(Seq("seenGroupId")), any[Seq[AcademicYear]]) returns Seq("hi900_30")

      command.doGroupMembers()

      verify(membershipService, times(2)).replaceMembers(anArgThat(hasModuleCode("HI33M-30")), isEq(
        Seq(
          registrations.head,
          registrations(1)
        )
      ), isEq(UpstreamAssessmentGroupMemberAssessmentType.OriginalAssessment))

      verify(membershipService, times(1)).replaceMembers(anArgThat(hasModuleCode("HI100-30")), isEq(
        Seq(
          registrations(2),
          registrations(3)
        )
      ), isEq(UpstreamAssessmentGroupMemberAssessmentType.OriginalAssessment))

      // The bug is that we don't update any group we don't have moduleregistrations for.
      verify(membershipService, times(0)).replaceMembers(anArgThat(hasModuleCode("HI900-30")), isEq(Nil), isEq(UpstreamAssessmentGroupMemberAssessmentType.OriginalAssessment))

    }
  }

  /**
    * TAB-3389
    */
  it should "set seat number to null where there is ambiguity" in {
    new Fixture {
      val registrations = Seq(
        UpstreamAssessmentRegistration("13/14", "0100001/1", "1", "A", "A01", "HI33M-30", "A", "", "", "", "", null, null, None, None, None),
        UpstreamAssessmentRegistration("13/14", "0100001/1", "1", "A", "A01", "HI33M-30", "A", "", "", "", "", null, null, None, None, None),

        // We only set currentResitAttempt here so it passes the hasChanged check and calls .save() (for testing at the end)
        UpstreamAssessmentRegistration("13/14", "0100002/1", "2", "A", "A01", "HI33M-30", "A", "", "", "", "", "1", null, None, None, None),
        UpstreamAssessmentRegistration("13/14", "0100002/1", "3", "A", "A01", "HI33M-30", "A", "", "", "", "", "1", null, None, None, None)
      )

      membershipService.getUpstreamAssessmentGroupsNotIn(isEq(Seq("seenGroupId")), any[Seq[AcademicYear]]) returns Seq("hi900_30")

      val members: ArrayBuffer[UpstreamAssessmentGroupMember] = collection.mutable.ArrayBuffer[UpstreamAssessmentGroupMember]()
      membershipService.save(any[UpstreamAssessmentGroupMember]) answers { arg: Any =>
        val member = arg.asInstanceOf[UpstreamAssessmentGroupMember]
        members.append(member)
        ()
      }

      command.doGroupMembers()

      // Duplicates now allowed and handled inside replaceMembers
      verify(membershipService, times(2)).replaceMembers(anArgThat(hasModuleCode("HI33M-30")), isEq(
        Seq(
          registrations.head,
          registrations(1),
          registrations(2),
          registrations(3)
        )
      ), isEq(UpstreamAssessmentGroupMemberAssessmentType.OriginalAssessment))

      // 0100001/1 set to 1 (duplicate seat number)
      members.find(_.universityId == "0100001").get.position should be(Option(1))

      // 0100002/1 not passed in (stays null).
      // Only called once as it only matches the _exact_ group (where sequence is A01)
      members.find(_.universityId == "0100002").get.position should be(None)
    }
  }

  /**
    * TAB-4557
    */
  it should "set marks and grades to null where there is ambiguity" in {
    new Fixture {
      val registrations = Seq(
        UpstreamAssessmentRegistration("13/14", "0100001/1", "1", "A", "A01", "HI33M-30", "A", "34", "21", "34", "21", null, null, None, None, None),
        UpstreamAssessmentRegistration("13/14", "0100001/1", "1", "A", "A01", "HI33M-30", "A", "34", "F", "34", "F", null, null, None, None, None),
        UpstreamAssessmentRegistration("13/14", "0100002/1", "2", "A", "A01", "HI33M-30", "A", "67", "21", "72", "1", null, null, None, None, None),
        UpstreamAssessmentRegistration("13/14", "0100002/1", "3", "A", "A01", "HI33M-30", "A", "67", "21", "72", "1", null, null, None, None, None)
      )

      membershipService.getUpstreamAssessmentGroupsNotIn(isEq(Seq("seenGroupId")), any[Seq[AcademicYear]]) returns Seq("hi900_30")

      val members: ArrayBuffer[UpstreamAssessmentGroupMember] = collection.mutable.ArrayBuffer[UpstreamAssessmentGroupMember]()
      membershipService.save(any[UpstreamAssessmentGroupMember]) answers { arg: Any =>
        val member = arg.asInstanceOf[UpstreamAssessmentGroupMember]
        members.append(member)
        ()
      }

      command.doGroupMembers()

      // Duplicates now allowed and handled inside replaceMembers
      verify(membershipService, times(2)).replaceMembers(anArgThat(hasModuleCode("HI33M-30")), isEq(
        Seq(
          registrations.head,
          registrations(1),
          registrations(2),
          registrations(3)
        )
      ), isEq(UpstreamAssessmentGroupMemberAssessmentType.OriginalAssessment))

      val member1: UpstreamAssessmentGroupMember = members.find(_.universityId == "0100001").get
      member1.actualMark should be(Some(BigDecimal(34)))
      member1.actualGrade should be(None)
      member1.agreedMark should be(Some(BigDecimal(34)))
      member1.agreedGrade should be(None)
      val member2: UpstreamAssessmentGroupMember = members.find(_.universityId == "0100002").get
      member2.actualMark should be(Some(BigDecimal(67)))
      member2.actualGrade should be(Some("21"))
      member2.agreedMark should be(Some(BigDecimal(72)))
      member2.agreedGrade should be(Some("1"))
    }
  }

  /** Matches on an UpstreamAssessmentGroup's module code. */
  def hasModuleCode(code: String): _root_.uk.ac.warwick.tabula.CustomHamcrestMatchers.HasPropertyMatcher[Nothing] = CustomHamcrestMatchers.hasProperty(Symbol("moduleCode"), code)

  behavior of "removeBlankFeedbackForDeregisteredStudents"

  it should "remove blank feedback for deregistered students" in new Fixture {
    val registrations: Seq[UpstreamAssessmentRegistration] = Nil

    val user = new User
    user.setUserId("custrd")

    val feedback = new Feedback
    val markerFeedback = new MarkerFeedback
    markerFeedback.feedback = feedback
    feedback.markerFeedback.add(markerFeedback)
    feedback.usercode = user.getUserId

    val assignment = new Assignment
    assignment.addFeedback(feedback)

    assignment.feedbackService = smartMock[FeedbackService]
    assignment.feedbackService.loadFeedbackForAssignment(assignment) answers { _: Any => assignment.feedbacks.asScala.toSeq }

    command.modifiedAssignments = Set(assignment)

    // User not a member of the assignment, empty feedback - remove
    membershipService.determineMembershipUsers(assignment) returns Nil
    command.removeBlankFeedbackForDeregisteredStudents() should contain(feedback)

    // User not a member of the assignment, modified feedback - don't remove
    markerFeedback.updatedOn = DateTime.now
    command.removeBlankFeedbackForDeregisteredStudents() should be(empty)

    // User is a member of the assignment, empty feedback - don't remove
    membershipService.determineMembershipUsers(assignment) returns Seq(user)
    markerFeedback.updatedOn = null
    command.removeBlankFeedbackForDeregisteredStudents() should be(empty)
  }

}
