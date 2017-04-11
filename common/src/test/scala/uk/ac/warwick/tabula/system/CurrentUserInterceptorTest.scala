package uk.ac.warwick.tabula.system
import javax.servlet.http

import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}
import uk.ac.warwick.sso.client.SSOClientFilter
import uk.ac.warwick.tabula.{CurrentUser, MockUserLookup, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.roles.{Masquerader, Sysadmin}
import uk.ac.warwick.tabula.services.permissions.RoleService
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, ProfileService}
import uk.ac.warwick.userlookup.{AnonymousUser, User}

class CurrentUserInterceptorTest extends TestBase with Mockito {

	val interceptor = new CurrentUserInterceptor

	val roleService: RoleService = mock[RoleService]
	val profileService: ProfileService = mock[ProfileService]
	val departmentService: ModuleAndDepartmentService = mock[ModuleAndDepartmentService]
	val userLookup = new MockUserLookup

	interceptor.roleService = roleService
	interceptor.userLookup = userLookup
	interceptor.profileService = profileService
	interceptor.departmentService = departmentService

	departmentService.departmentsWithPermission(any[CurrentUser], any[Permission]) returns Set()

	interceptor.userNavigationGenerator = new UserNavigationGenerator {
		override def apply(user: User, forceUpdate: Boolean): UserNavigation = UserNavigation("", "")
	}
	
	@Test def foundUser() {
		val user = new User("cuscav")
		user.setFoundUser(true)

		val req = new MockHttpServletRequest
		req.setAttribute(SSOClientFilter.USER_KEY, user)

		profileService.getMemberByUser(user, disableFilter = true, eagerLoad = true) returns None
		
		val resp = new MockHttpServletResponse
		
		interceptor.preHandle(req, resp, null) should be {true}
		
		val currentUser = req.getAttribute(CurrentUser.keyName).asInstanceOf[CurrentUser]
		currentUser should not be null
		
		currentUser.realUser should be (user)
		currentUser.apparentUser should be (user)
		currentUser.god should be {false}
		currentUser.masquerader should be {false}
		currentUser.sysadmin should be {false}
		currentUser.profile should be ('empty)
	}

	@Test def foundUserWithProfile() {
		val user = new User("cuscav")
		user.setFoundUser(true)

		val req = new MockHttpServletRequest
		req.setAttribute(SSOClientFilter.USER_KEY, user)

		val member = mock[Member]
		profileService.getMemberByUser(user, disableFilter = true, eagerLoad = true) returns Some(member)
		
		val resp = new MockHttpServletResponse
		
		interceptor.preHandle(req, resp, null) should be {true}
		
		val currentUser = req.getAttribute(CurrentUser.keyName).asInstanceOf[CurrentUser]
		currentUser should not be null
		
		currentUser.realUser should be (user)
		currentUser.apparentUser should be (user)
		currentUser.god should be {false}
		currentUser.masquerader should be {false}
		currentUser.sysadmin should be {false}
		currentUser.profile should be (Some(member))
	}

	@Test def notFoundUser() {
		val user = new User("cuscav")
		user.setFoundUser(false)

		val req = new MockHttpServletRequest
		req.setAttribute(SSOClientFilter.USER_KEY, user)

		profileService.getMemberByUser(user, disableFilter = true, eagerLoad = true) returns None
		
		val resp = new MockHttpServletResponse
		
		interceptor.preHandle(req, resp, null) should be {true}
		
		val currentUser = req.getAttribute(CurrentUser.keyName).asInstanceOf[CurrentUser]
		currentUser should not be null
		
		currentUser.realUser.isInstanceOf[AnonymousUser] should be {true}
		currentUser.apparentUser.isInstanceOf[AnonymousUser] should be {true}
		currentUser.god should be {false}
		currentUser.masquerader should be {false}
		currentUser.sysadmin should be {false}
		currentUser.profile should be ('empty)
	}

	@Test def masquerading {
		val user = new User("cuscav")
		user.setFoundUser(true)

		val masque = new User("cusebr")
		masque.setFoundUser(true)

		roleService.hasRole(isA[CurrentUser], isEq(Masquerader())) returns true
		userLookup.users += ("cusebr" -> masque)
		
		profileService.getMemberByUser(masque, disableFilter = true, eagerLoad = true) returns None
		
		val req = new MockHttpServletRequest
		req.setAttribute(SSOClientFilter.USER_KEY, user)
		req.setCookies(new http.Cookie(CurrentUser.masqueradeCookie, "cusebr"))

		val resp = new MockHttpServletResponse

		interceptor.preHandle(req, resp, null) should be {true}
		
		val currentUser = req.getAttribute(CurrentUser.keyName).asInstanceOf[CurrentUser]
		currentUser should not be null
		
		currentUser.realUser should be (user)
		currentUser.apparentUser should be (masque)
		currentUser.god should be {false}
		currentUser.masquerader should be {true}
		currentUser.sysadmin should be {false}
		currentUser.profile should be ('empty)
	}

	@Test def masqueradeAttemptButNotMasquerader {
		val user = new User("cuscav")
		user.setFoundUser(true)

		val masque = new User("cusebr")
		masque.setFoundUser(true)

		roleService.hasRole(isA[CurrentUser], isEq(Masquerader())) returns false
		userLookup.users += ("cusebr" -> masque)
		
		profileService.getMemberByUser(user, disableFilter = true, eagerLoad = true) returns None
		
		val req = new MockHttpServletRequest
		req.setAttribute(SSOClientFilter.USER_KEY, user)
		req.setCookies(new http.Cookie(CurrentUser.masqueradeCookie, "cusebr"))

		val resp = new MockHttpServletResponse

		interceptor.preHandle(req, resp, null) should be {true}
		
		val currentUser = req.getAttribute(CurrentUser.keyName).asInstanceOf[CurrentUser]
		currentUser should not be null
		
		currentUser.realUser should be (user)
		currentUser.apparentUser should be (user)
		currentUser.god should be {false}
		currentUser.masquerader should be {false}
		currentUser.sysadmin should be {false}
		currentUser.profile should be ('empty)
	}

	@Test def sysadmin {
		val user = new User("cuscav")
		user.setFoundUser(true)

		roleService.hasRole(isA[CurrentUser], isEq(Sysadmin())) returns true
		
		val req = new MockHttpServletRequest
		req.setAttribute(SSOClientFilter.USER_KEY, user)
		
		profileService.getMemberByUser(user, disableFilter = true, eagerLoad = true) returns None
		
		val resp = new MockHttpServletResponse
		
		interceptor.preHandle(req, resp, null) should be {true}
		
		val currentUser = req.getAttribute(CurrentUser.keyName).asInstanceOf[CurrentUser]
		currentUser should not be null
		
		currentUser.realUser should be (user)
		currentUser.apparentUser should be (user)
		currentUser.god should be {false}
		currentUser.masquerader should be {true}
		currentUser.sysadmin should be {true}
		currentUser.profile should be ('empty)
	}

	@Test def god {
		val user = new User("cuscav")
		user.setFoundUser(true)

		roleService.hasRole(isA[CurrentUser], isEq(Sysadmin())) returns true
		
		val req = new MockHttpServletRequest
		req.setAttribute(SSOClientFilter.USER_KEY, user)
		req.setCookies(new http.Cookie(CurrentUser.godModeCookie, "true"))
		
		profileService.getMemberByUser(user, disableFilter = true, eagerLoad = true) returns None
		
		val resp = new MockHttpServletResponse
		
		interceptor.preHandle(req, resp, null) should be {true}
		
		val currentUser = req.getAttribute(CurrentUser.keyName).asInstanceOf[CurrentUser]
		currentUser should not be null
		
		currentUser.realUser should be (user)
		currentUser.apparentUser should be (user)
		currentUser.god should be {true}
		currentUser.masquerader should be {true}
		currentUser.sysadmin should be {true}
		currentUser.profile should be ('empty)
	}

	@Test def godNotSysadmin {
		val user = new User("cuscav")
		user.setFoundUser(true)

		roleService.hasRole(isA[CurrentUser], isEq(Sysadmin())) returns false
		
		val req = new MockHttpServletRequest
		req.setAttribute(SSOClientFilter.USER_KEY, user)
		req.setCookies(new http.Cookie(CurrentUser.godModeCookie, "true"))
		
		profileService.getMemberByUser(user, disableFilter = true, eagerLoad = true) returns None
		
		val resp = new MockHttpServletResponse
		
		interceptor.preHandle(req, resp, null) should be {true}
		
		val currentUser = req.getAttribute(CurrentUser.keyName).asInstanceOf[CurrentUser]
		currentUser should not be null
		
		currentUser.realUser should be (user)
		currentUser.apparentUser should be (user)
		currentUser.god should be {false}
		currentUser.masquerader should be {false}
		currentUser.sysadmin should be {false}
		currentUser.profile should be ('empty)
	}

}
