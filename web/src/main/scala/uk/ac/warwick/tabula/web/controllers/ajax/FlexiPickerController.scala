package uk.ac.warwick.tabula.web.controllers.ajax

import java.io.{UnsupportedEncodingException, Writer}
import java.net.URLEncoder

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.UniversityId
import uk.ac.warwick.tabula.commands.{Appliable, CommandInternal, ComposableCommand, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, AutowiringUserLookupComponent, ProfileServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.controllers.ajax.FlexiPickerController.{FlexiPickerState, FlexiPickerResult, FlexiPickerCommand}
import uk.ac.warwick.userlookup.webgroups.{GroupNotFoundException, GroupServiceException}
import uk.ac.warwick.userlookup.{Group, User}

import scala.collection.JavaConversions._

@Controller
class FlexiPickerController extends BaseController {
	var json: ObjectMapper = Wire[ObjectMapper]

	@ModelAttribute("command")
	def createCommand() = FlexiPickerCommand()

	@RequestMapping(value = Array("/ajax/flexipicker/query.json"))
	def queryJson(@ModelAttribute("command") form: Appliable[FlexiPickerResult] with FlexiPickerState, out: Writer): Unit = {
		var results: FlexiPickerResult = null

		if (form.hasQuery && form.query.trim.length > 2) {
			results = form.apply()
		}

		json.writeValue(out, Map("data" -> Map("results" -> results)))
	}

}



object FlexiPickerController {
	type FlexiPickerResult = List[Map[String, String]]

	val NewStarterUserTypeString = "New Starter"

	object FlexiPickerCommand {
		def apply() =
			new FlexiPickerCommand()
				with ComposableCommand[FlexiPickerResult]
				with AutowiringUserLookupComponent
				with AutowiringProfileServiceComponent
				with FlexiPickerPermissions
				with ReadOnly with Unaudited
	}

	class FlexiPickerCommand extends CommandInternal[FlexiPickerResult] with FlexiPickerState with Logging {
		self: UserLookupComponent with ProfileServiceComponent =>

		def applyInternal(): List[Map[String, String]] = {
			var results = List[Map[String, String]]()

			if(includeEmail)  results ++= parseEmail       // add results of email search to our list
			if(includeUsers)  results ++= searchUsers      // add results of user search to our list
			if(includeGroups) results ++= searchGroups     // add results of group search to our list

			results
		}

		private val EmailPattern = "^\\s*(?:([^<@]+?)\\s+)<?([^\\s]+?@[^\\s]+\\.[^\\s]+?)>?\\s*$".r

		private def parseEmail: FlexiPickerResult = {
			var list: FlexiPickerResult = List[Map[String, String]]()
			if (hasQuery) {
				val matched = EmailPattern.findAllIn(query.trim).matchData.toList

				if (matched.length>0) {
					val firstMatch = matched.head
					val builder : Map[String, String] = Map("type" -> "email", "address" -> firstMatch.group(2))

					if (firstMatch.group(1) != null) {
						builder.put("name", firstMatch.group(1))
						builder.put("value", firstMatch.group(1) + " <" + firstMatch.group(2) + ">")
					}
					else {
						builder.put("value", firstMatch.group(2))
					}
					list = list :+ builder
				}
			}
			list
		}

		private def searchUsers : FlexiPickerResult = {
			var users = List[User]()

			if (hasQuery) {
				val terms: Array[String] = query.trim.replace(",", " ").replaceAll("\\s+", " ").split(" ")
				users = doSearchUsers(terms)
			}

			users.filter(isValidUser).map(createItemFor)
		}

		/**
		 * Search users using the given terms.
		 *
		 * Algorithm cribbed from Files.Warwick's UserDAOImpl.
		 *
		 */
		private def doSearchUsers(terms: Array[String]): List[User] = {
			var users: List[User] = List[User]()
			if (exact) {
				if (terms.length == 1) {
					val user =
						if (UniversityId.isValid(terms(0)))
							userLookup.getUserByWarwickUniId(terms(0))
						else
							userLookup.getUserByUserId(terms(0))
					if (user.isFoundUser) {
						users = users :+ user
					}
				}
			}
			else if (terms.length == 1) {
				val user =
					if (UniversityId.isValid(terms(0)))
						userLookup.getUserByWarwickUniId(terms(0))
					else
						userLookup.getUserByUserId(terms(0))
				if (user.isFoundUser) {
					users = users :+ user
				}	else {
					users  = users ++  userLookup.findUsersWithFilter(item("sn" , terms(0)))
					if (users.size < EnoughResults) {
						users ++= userLookup.findUsersWithFilter(item("givenName", terms(0)))
					}
					if (users.size < EnoughResults) {
						users ++= userLookup.findUsersWithFilter(item("cn", terms(0)))
					}
				}
			}
			else if (terms.length >= 2) {
				users ++= userLookup.findUsersWithFilter(item("givenName", terms(0)) ++ item("sn", terms(1)))
				if (users.size < EnoughResults) {
					users ++= userLookup.findUsersWithFilter(item("sn", terms(0)) ++ item("givenName", terms(1)))
				}
			}

			users.sortBy(_.getFullName)
		}

		private def isValidUser(user: User) =
			user.isFoundUser && {
				val hasUniversityIdIfNecessary = !universityId || user.getWarwickId.hasText
				val isNotNewStarter = user.getUserType != NewStarterUserTypeString
				val isTabulaMemberIfNecessary = hasUniversityIdIfNecessary && (!tabulaMembersOnly || {
						if (universityId) profileService.getMemberByUniversityId(user.getWarwickId).isDefined
						else profileService.getMemberByUser(user, disableFilter = true).isDefined
				})

				hasUniversityIdIfNecessary && isTabulaMemberIfNecessary && isNotNewStarter
			}

		private def searchGroups: FlexiPickerResult = {
			var list: FlexiPickerResult = List[Map[String, String]]()
			if (hasQuery) {
				try {
					list = doGroupSearch()
				}
				catch {
					case e: GroupServiceException =>
						logger.error("Failed to look up group names", e)
					case e: UnsupportedEncodingException =>
						throw new IllegalStateException("UTF-8 Does Not Exist??")
					case e: GroupNotFoundException =>
						logger.debug("Group didn't exist")
				}
			}
			list
		}

		private def doGroupSearch(): FlexiPickerResult =  {
			var outList: FlexiPickerResult = List[Map[String, String]]()
			if (exact) {
				if (!query.trim.contains(" ")) {
					val group: Group = userLookup.getGroupService.getGroupByName(query.trim)
					outList = List(createItemFor(group))
				}
			}
			else {
				val groupQuery = URLEncoder.encode(query.trim, "UTF-8")
				val groups: List[Group] = userLookup.getGroupService.getGroupsForQuery(groupQuery).toList
				outList = groups.map(createItemFor)
			}

			outList
		}



		private def createItemFor(user: User): Map[String, String] = {
			Map("type" -> "user", "name" -> user.getFullName, "isStaff" -> user.isStaff.toString, "department" -> user.getDepartment, "value" -> getValue(user))
		}

		/** Return appropriate value for a user, either warwick ID or usercode. */
		private def getValue(user: User): String = {
			if (universityId) user.getWarwickId else user.getUserId
		}

		private def createItemFor(group: Group): Map[String, String] = {
			Map("type" -> "group", "title" -> group.getTitle, "groupType" -> group.getType, "value" -> group.getName)
		}

		private def item(name: String, value: String): Map[String, String] = value match {
			case s: String if s.hasText => Map(name -> (value + "*"))
			case _ => Map.empty
		}
	}

	trait FlexiPickerState {
		val EnoughResults = 10
		var includeUsers = true
		var includeGroups = false
		var includeEmail = false
		var query: String = ""
		var exact = false
		var universityId = false    // when returning users, use university ID as value
		var tabulaMembersOnly = false // filter out anyone who isn't in the Tabula members db

		def hasQuery: Boolean = query.hasText
	}

	trait FlexiPickerPermissions extends RequiresPermissionsChecking {
		override def permissionsCheck(p: PermissionsChecking) {
			p.PermissionCheck(Permissions.UserPicker)
		}
	}
}