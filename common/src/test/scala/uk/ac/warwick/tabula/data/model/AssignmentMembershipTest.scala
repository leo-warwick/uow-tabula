package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.userlookup.User
import org.junit.Before
import uk.ac.warwick.userlookup.AnonymousUser

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports.{JHashMap, JList}

class AssignmentMembershipTest extends TestBase with Mockito {

	var userDatabase: Seq[User] = Seq(
		("0000000","aaaaa"),
		("0000001","aaaab"),
		("0000002","aaaac"),
		("0000003","aaaad"),
		("0000004","aaaae"),
		("0000005","aaaaf"),
		("0000006","aaaag"),
		("0000007","aaaah")
	) map { case(id,code) =>
		val user = new User(code)
		user.setWarwickId(id)
		user.setFullName("Roger " + code.head.toUpper + code.tail)
		user
	}

  var assignmentMembershipService: AssessmentMembershipService = _
	var userLookup: UserLookupService = _
	val nobody: UserGroup = UserGroup.ofUsercodes

	@Before def before() {
		userLookup = smartMock[UserLookupService]
		userLookup.getUserByUserId(any[String]) answers { id =>
			userDatabase find {_.getUserId == id} getOrElse new AnonymousUser()
		}
		userLookup.getUserByWarwickUniId(any[String]) answers { id =>
			userDatabase find {_.getWarwickId == id} getOrElse new AnonymousUser()
		}
		userLookup.getUsersByUserIds(any[JList[String]]) answers { ids =>
			val users = ids.asInstanceOf[JList[String]].asScala.map(id=>(id,userDatabase find {_.getUserId == id} getOrElse new AnonymousUser()))
			JHashMap(users:_*)
		}
		userLookup.getUsersByWarwickUniIds(any[Seq[String]]) answers { ids =>
			ids.asInstanceOf[Seq[String]].map(id => (id, userDatabase.find {_.getWarwickId == id}.getOrElse (new AnonymousUser()))).toMap
		}
		val profileService = smartMock[ProfileService]
		profileService.getAllMembersWithUniversityIds(any[Seq[String]]) returns Seq()
    assignmentMembershipService = {
      val s = new AssessmentMembershipServiceImpl
      s.userLookup = userLookup
			s.profileService = profileService
      s
    }
	}

	@Test def whenEmpty() {
		val membership = assignmentMembershipService.determineMembership(Nil, Option(nobody)).items
		membership.size should be (0)
	}

	@Test def emptyWithNone() {
		val membership = assignmentMembershipService.determineMembership(Nil, None).items
		membership.size should be (0)
	}

	@Test def plainSits() {
		val upstream = newAssessmentGroup(Seq("0000005","0000006"))
		val membership = assignmentMembershipService.determineMembership(Seq(upstream), Option(nobody)).items
		membership.size should be (2)
		membership.head.user.getFullName should be ("Roger Aaaaf")
		membership(1).user.getFullName should be ("Roger Aaaag")
	}

	@Test def plainSitsWithNone() {
		val upstream = newAssessmentGroup(Seq("0000005","0000006"))
		val membership = assignmentMembershipService.determineMembership(Seq(upstream), None).items
		membership.size should be (2)
		membership.head.user.getFullName should be ("Roger Aaaaf")
		membership(1).user.getFullName should be ("Roger Aaaag")
	}

	@Test def includeAndExclude() {
		val upstream = newAssessmentGroup(Seq("0000005","0000006"))
		val others = UserGroup.ofUsercodes
		others.userLookup = userLookup
		others.addUserId("aaaaa")
		others.excludeUserId("aaaaf")
		val membership = assignmentMembershipService.determineMembership(Seq(upstream), Option(others)).items

		membership.size should be (3)

		membership.head.user.getFullName should be ("Roger Aaaaa")
		membership.head.itemType should be (IncludeType)
		membership.head.itemTypeString should be ("include")
		membership.head.extraneous should be (false)

		membership(1).user.getFullName should be ("Roger Aaaaf")
		membership(1).itemType should be (ExcludeType)
		membership(1).itemTypeString should be ("exclude")
		membership(1).extraneous should be (false)

		membership(2).user.getFullName should be ("Roger Aaaag")
		membership(2).itemType should be (SitsType)
		membership(2).itemTypeString should be ("sits")
		membership(2).extraneous should be (false)

    // test the simpler methods that return a list of Users

    val users = assignmentMembershipService.determineMembershipUsers(Seq(upstream), Option(others))
    users.size should be (2)
    users.head.getFullName should be ("Roger Aaaaa")
    users(1).getFullName should be ("Roger Aaaag")
	}

	/**
	 * Test that the "extraneous" flag is set because "aaaaf" is already
	 * part of the SITS group, and excluded code "aaaah" is not in the
	 * group anyway so the exclusion does nothing.
	 */
	@Test def redundancy() {
		val upstream = newAssessmentGroup(Seq("0000005", "0000006"))
		val others = UserGroup.ofUsercodes
		others.addUserId("aaaaf")
		others.excludeUserId("aaaah")
		others.userLookup = userLookup
		val membership = assignmentMembershipService.determineMembership(Seq(upstream), Option(others)).items
		membership.size should be(3)

		membership.head.user.getFullName should be("Roger Aaaaf")
		membership.head.itemType should be(IncludeType)
		membership.head.itemTypeString should be("include")
		membership.head.extraneous should be(true)

		membership(1).user.getFullName should be("Roger Aaaah")
		membership(1).itemType should be(ExcludeType)
		membership(1).itemTypeString should be("exclude")
		membership(1).extraneous should be(true)

		membership(2).user.getFullName should be("Roger Aaaag")
		membership(2).itemType should be(SitsType)
		membership(2).itemTypeString should be("sits")
		membership(2).extraneous should be(false)
	}


	def newAssessmentGroup(uniIds:Seq[String]): UpstreamAssessmentGroup = {
		val upstream = new UpstreamAssessmentGroup
		upstream.members.addAll(uniIds.map(uniId => new UpstreamAssessmentGroupMember(upstream, uniId)).asJava)
		upstream
	}
}