package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Tap._
import uk.ac.warwick.userlookup.{AnonymousUser, User}

import scala.jdk.CollectionConverters._

class AssessmentMembershipServiceTest extends TestBase with Mockito {

  @Test def testDetermineMembership() {
    val userLookup = new MockUserLookup
    userLookup.registerUsers("aaaaa", "bbbbb", "ccccc", "ddddd", "eeeee", "fffff")

    val user1 = userLookup.getUserByUserId("aaaaa")
    user1.setLastName("Aaaaa")
    val user2 = userLookup.getUserByUserId("bbbbb")
    user2.setLastName("Bbbbb")
    val user3 = userLookup.getUserByUserId("ccccc")
    user3.setLastName("Ccccc")
    val user4 = userLookup.getUserByUserId("ddddd")
    user4.setLastName("Ddddd")
    val user5 = userLookup.getUserByUserId("eeeee")
    user5.setLastName("Eeeee")
    val user6 = userLookup.getUserByUserId("fffff")
    user6.setLastName("Fffff")

    val assignmentMembershipService = new AssessmentMembershipServiceImpl
    assignmentMembershipService.userLookup = userLookup
    assignmentMembershipService.profileService = smartMock[ProfileService]
    assignmentMembershipService.profileService.getAllMembersWithUniversityIds(any[Seq[String]]) returns Seq()

    val uag = new UpstreamAssessmentGroup
    uag.assessmentGroup = "A"
    uag.moduleCode = "AM101"
    uag.members.add(new UpstreamAssessmentGroupMember(uag, user3.getWarwickId))
    uag.members.add(new UpstreamAssessmentGroupMember(uag, user1.getWarwickId))
    uag.members.add(new UpstreamAssessmentGroupMember(uag, user2.getWarwickId))
    val usr4 = new AnonymousUser()
    uag.members.add(new UpstreamAssessmentGroupMember(uag, usr4.getWarwickId))
    val activeMembers = uag.members.asScala.filter(_.universityId != usr4.getWarwickId)

    val other = UserGroup.ofUsercodes
    other.userLookup = userLookup

    other.add(user5)
    other.add(user4)
    other.add(user6)

    val upstream = Seq[UpstreamAssessmentGroup](uag)
    val uInfo = UpstreamAssessmentGroupInfo(uag, uag.members.asScala.toSeq.filter(_.universityId != usr4.getWarwickId))

    val others = Some(other)

    val info = assignmentMembershipService.determineMembership(Seq(uInfo), others, resitOnly = false)
    info.items.size should be(6)
    info.items.head.userId should be(Some("aaaaa"))
    info.items(1).userId should be(Some("bbbbb"))
    info.items(2).userId should be(Some("ccccc"))
  }

  @Test def studentMember() {
    val service = new AssessmentMembershipServiceImpl

    val user = new User("cuscav").tap(_.setWarwickId("0672089"))

    service.userLookup = new MockUserLookup().tap(_.registerUserObjects(user))
    service.profileService = smartMock[ProfileService]

    val allMembers: Seq[Member] = Seq(
      Fixtures.student("0123456"),
      Fixtures.student("0123457"),
      Fixtures.staff(universityId = "0672089", userId = "cuscav")
    )
    service.profileService.getAllMembersWithUniversityIds(Seq("0123456", "0123457", "0672089")) returns allMembers

    val excludedGroup = smartMock[UnspecifiedTypeUserGroup]
    excludedGroup.excludesUser(user) returns true

    service.isStudentCurrentMember(user, Nil, Some(excludedGroup), resitOnly = false) should be(false)
    verify(excludedGroup, times(0)).includesUser(user) // we quit early

    val includedGroup = smartMock[UnspecifiedTypeUserGroup]
    includedGroup.excludesUser(user) returns false
    includedGroup.includesUser(user) returns true

    service.isStudentCurrentMember(user, Nil, Some(includedGroup), resitOnly = false) should be(true)

    val notInGroup = smartMock[UnspecifiedTypeUserGroup]
    notInGroup.excludesUser(user) returns false
    notInGroup.includesUser(user) returns false
    notInGroup.users returns Set.empty
    notInGroup.excludes returns Set.empty

    service.isStudentCurrentMember(user, Nil, Some(notInGroup), resitOnly = false) should be(false)

    val module = Fixtures.module("in101")

    val upstream1 = Fixtures.assessmentGroup(Fixtures.upstreamAssignment(module, 101))
    //member 0123458 as PWD
    val upstreamWithActiveMembers1 = UpstreamAssessmentGroupInfo(upstream1, upstream1.members.asScala.toSeq.filter(m => m.universityId != "0123458"))

    val upstream2 = Fixtures.assessmentGroup(Fixtures.upstreamAssignment(module, 101))
    // Include the user in upstream2
    upstream2.members.add(new UpstreamAssessmentGroupMember(upstream2, "0672089"))
    val upstreamWithActiveMembers2 = UpstreamAssessmentGroupInfo(upstream2, upstream2.members.asScala.toSeq.filter(m => m.universityId != "0123458"))

    val upstream3 = Fixtures.assessmentGroup(Fixtures.upstreamAssignment(module, 101))
    val upstreamWithActiveMembers3 = UpstreamAssessmentGroupInfo(upstream3, upstream3.members.asScala.toSeq.filter(m => m.universityId != "0123458"))

    val upstreams = Seq(upstreamWithActiveMembers1, upstreamWithActiveMembers2, upstreamWithActiveMembers3)
    service.isStudentCurrentMember(user, upstreams, None, resitOnly = false) should be(true)

    // Doesn't affect results from the usergroup itself
    service.isStudentCurrentMember(user, upstreams, Some(excludedGroup), resitOnly = false) should be(false)
    service.isStudentCurrentMember(user, upstreams, Some(includedGroup), resitOnly = false) should be(true)
    service.isStudentCurrentMember(user, upstreams, Some(notInGroup), resitOnly = false) should be(true)
  }

}
