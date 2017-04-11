package uk.ac.warwick.tabula.services.jobs
import com.fasterxml.jackson.databind.ObjectMapper
import uk.ac.warwick.tabula.{CurrentUser, JsonObjectMapperFactory, MockUserLookup, Mockito, TestBase}
import uk.ac.warwick.tabula.system.{CurrentUserInterceptor, UserNavigation, UserNavigationGenerator}
import uk.ac.warwick.tabula.services.permissions.RoleService
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, ProfileService}
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.userlookup.User

class JobInstanceTest extends TestBase with Mockito {

	val jsonMapper: ObjectMapper = new JsonObjectMapperFactory().createInstance
	val userLookup = new MockUserLookup

	val currentUserFinder = new CurrentUserInterceptor
	val roleService: RoleService = mock[RoleService]
	currentUserFinder.userLookup = userLookup
	currentUserFinder.roleService = roleService
	currentUserFinder.profileService = smartMock[ProfileService]

	currentUserFinder.departmentService = smartMock[ModuleAndDepartmentService]
	currentUserFinder.departmentService.departmentsWithPermission(any[CurrentUser], any[Permission]) returns Set()

	currentUserFinder.userNavigationGenerator = new UserNavigationGenerator {
		override def apply(user: User, forceUpdate: Boolean): UserNavigation = UserNavigation("", "")
	}

	@Test def onLoad() {
		val instance = new JobInstanceImpl
		instance.jsonMapper = jsonMapper
		instance.userLookup = userLookup
		instance.currentUserFinder = currentUserFinder

		userLookup.registerUsers("cuscav", "cusebr")

		instance.realUser = "cuscav"
		instance.apparentUser = "cusebr"

		instance.data = """{"steve":"yes"}"""

		instance.user should be (null)
		instance.json should be ('empty)
		instance.updatedDate should not be null
		
		val oldUpdatedDate = instance.updatedDate
		instance.postLoad

		instance.updatedDate.isAfter(oldUpdatedDate) should be (true)
		instance.user.apparentId should be ("cusebr")
		instance.user.realId should be ("cuscav")
		instance.json("steve") should be ("yes")
	}

	@Test def strings() {
		val instance = new JobInstanceImpl
		instance.jsonMapper = jsonMapper

		instance.setString("steve", "yes")
		instance.setStrings("bob", Seq("no", "maybe"))

		instance.getString("steve") should be ("yes")
		instance.getStrings("bob") should be (Seq("no", "maybe"))

		instance.data should be ("""{"steve":"yes","bob":["no","maybe"]}""")
	}

}