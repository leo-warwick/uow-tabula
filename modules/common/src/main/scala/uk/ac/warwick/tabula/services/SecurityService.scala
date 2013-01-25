package uk.ac.warwick.tabula.services
import uk.ac.warwick.userlookup.GroupService
import org.springframework.beans.factory.annotation.{Autowired,Value}
import uk.ac.warwick.util.core.StringUtils._
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model._
import forms.Extension
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.PermissionDeniedException
import uk.ac.warwick.tabula.actions._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.SubmitPermissionDeniedException

/**
 * Checks permissions.
 */
@Service
class SecurityService extends Logging {
	var userLookup = Wire.auto[UserLookupService]
	var profileService = Wire.auto[ProfileService]
	
	@Value("${permissions.admin.group}") var adminGroup: String = _
	@Value("${permissions.masquerade.group}") var masqueradeGroup: String = _

	def groupService = userLookup.getGroupService

	type Response = Option[Boolean]
	type PermissionChecker = (CurrentUser, Action[_]) => Response

	// The possible response types for a permissions check.
	// Continue means continue to the next check, otherwise it will stop and use the returned value.
	// It is an error for all the checks to return Continue, so it makes sense to have one check
	// at the end which never returns Continue.
	val Allow: Response = Some(true)
	val Deny: Response = Some(false)
	val Continue: Response = None // delegate to the next handler

	def isSysadmin(usercode: String) = hasText(usercode) && groupService.isUserInGroup(usercode, adminGroup)
	// excludes sysadmins, though they can also masquerade
	def isMasquerader(usercode: String) = hasText(usercode) && groupService.isUserInGroup(usercode, masqueradeGroup)

	val checks: Seq[PermissionChecker] = List(checkSysadmin _, checkEnrolled _, checkGroup _)

	def checkSysadmin(user: CurrentUser, action: Action[_]): Response = if (user.god) Allow else Continue

	def checkEnrolled(user: CurrentUser, action: Action[_]): Response = action match {
		case Submit(assignment: Assignment) => if (assignment.canSubmit(user.apparentUser)) Allow else Deny
		case _ => Continue
	}

	def checkGroup(user: CurrentUser, action: Action[_]): Response = Some(action match {

		case Manage(department: Department) => department isOwnedBy user.idForPermissions

		case View(department: Department) => can(user, Manage(department))

		// Participate module = can submit feedback, publish feedback, add/create/delete assignments
		case Participate(module: Module) => module.ensuredParticipants.includes(user.apparentId) ||
			can(user, Manage(module.department))

		// Manage module = can modify its permissions.
		case Manage(module: Module) => can(user, Manage(module.department))

		// should only be called on extension requests i.e. extension.isManual == false
		case Manage(extensionRequest: Extension) =>
			extensionRequest.assignment.module.department.isExtensionManager(user.apparentId) || can(user, Participate(extensionRequest.assignment.module))
			
		case View(extensionRequest: Extension) =>
			extensionRequest.userId == user.apparentId || can(user, Manage(extensionRequest)) || can(user, Participate(extensionRequest.assignment.module))

		// View module = see what assignments are in a module
		case View(module: Module) => can(user, View(module.department))

		case View(assignment: Assignment) => can(user, View(assignment.module))

		case Submit(assignment: Assignment) => can(user, View(assignment.module))
		
		case View(submission: Submission) => submission.universityId == user.universityId ||
			can(user, View(submission.assignment))

		case View(feedback: Feedback) => feedback.universityId == user.universityId ||
			can(user, View(feedback.assignment))

		case Delete(feedback: Feedback) => can(user, Participate(feedback.assignment.module))

		case Delete(submission: Submission) => can(user, Participate(submission.assignment.module))

		case DownloadSubmissions(assignment: Assignment) => assignment.isMarker(user.apparentUser) ||
			can(user, Participate(assignment.module))

		case UploadMarkerFeedback(assignment: Assignment) => assignment.isMarker(user.apparentUser)

		case Masquerade() => user.sysadmin || user.masquerader
		
		case Sysadmin() => user.sysadmin
		
		case UniversityMember() => user.isMember
		
		case View(member: Member) => {
			def isSamePerson = user.apparentId == member.userId
			
			def inSameDepartment = {
				val myDepartments = profileService.getMemberByUserId(user.apparentId) map { _.affiliatedDepartments } getOrElse(Seq())
				val theirDepartments = member.touchedDepartments
				
				val sameDepartments = myDepartments intersect member.touchedDepartments
				
				!sameDepartments.isEmpty
			}
			
			isSamePerson || (user.isStaff && inSameDepartment)
		}
		
		case Search(clazz: Class[Member]) => user.isStaff

		case action: Action[_] => throw new IllegalArgumentException(action.toString)
		case _ => throw new IllegalArgumentException()

	})

	/**
	 * Returns whether the given user can do the given Action on the object
	 * specified by the Action.
	 */
	def can(user: CurrentUser, action: Action[_]): Boolean = {
		transactional(readOnly=true) {
			// loop through checks, seeing if any of them return Allow or Deny
			for (check <- checks) {
				val response = check(user, action)
				response map { canDo =>
					if (debugEnabled) logger.debug("can " + user + " do " + action + "? " + (if (canDo) "Yes" else "NO"))
					return canDo
				}
			}
			throw new IllegalStateException("No security rule handled request for " + user + " doing " + action)
		}
	}

	def check(user: CurrentUser, action: Action[_]) = if (!can(user, action)) {
		action match {
			case Submit(assignment) => throw new SubmitPermissionDeniedException(assignment)
			case action => throw new PermissionDeniedException(user, action)
		}
	}
}
