package uk.ac.warwick.tabula.web.controllers.ajax

import java.io.Writer

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Controller
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.controllers.ajax.UserPickerController.UserPickerCommand
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

@Controller
class UserPickerController extends BaseController {
	var json: ObjectMapper = Wire.auto[ObjectMapper]

	@RequestMapping(value = Array("/ajax/userpicker/form"))
	def form: Mav = Mav("ajax/userpicker/form").noLayout()

	@RequestMapping(value = Array("/ajax/userpicker/query.json"))
	def queryJson(form: UserPickerCommand, out: Writer): Unit = {
		def toJson(user: User) = Map(
			"value" -> user.getUserId,
			"label" -> user.getFullName,
			"type" -> user.getUserType,
			"dept" -> user.getShortDepartment)
		json.writeValue(out, (form.apply() map toJson));
	}

	@RequestMapping(value = Array("/ajax/userpicker/query"))
	def query(form: UserPickerCommand, out: Writer): Mav = {
		val foundUsers = form.apply()
		val (staff, students) = foundUsers.partition { _.isStaff }
		Mav("ajax/userpicker/results",
			"staff" -> staff,
			"students" -> students).noLayout()
	}

}

object UserPickerController {
	class UserPickerCommand extends Command[Seq[User]] with ReadOnly with Unaudited {
		PermissionCheck(Permissions.UserPicker)

		var userLookup: UserLookupService = Wire.auto[UserLookupService]

		var firstName: String = ""
		var lastName: String = ""

		def applyInternal(): Seq[User] = {
			var users = userLookup.findUsersWithFilter(filter).asScala.toSeq
			if (users.size < 10) users ++= (userLookup.findUsersWithFilter(filterBackwards).asScala.toSeq filter { !users.contains(_) })

			users
		}

		/**
		 * If one word is given, it's used as surname.
		 * If more words are given, the first two are used
		 * 	as firstname and surname.
		 */
		def setQuery(q: String) {
			firstName = ""
			lastName = ""
			q.split("\\s").toList match {
				case Nil =>
				case surname :: Nil => lastName = surname
				case first :: second :: _ => firstName = first; lastName = second
			}
		}
		def query: String = if (firstName == "") lastName else firstName + " " + lastName
		def query_=(q: String): Unit = setQuery(q)

		def filter: Map[String, String] = {
			item("givenName", firstName) ++ item("sn", lastName)
		}

		// filter with surname as firstname and viceversa, in case we get no results
		def filterBackwards: Map[String, String] = {
			item("givenName", lastName) ++ item("sn", firstName)
		}

		private def item(name: String, value: String): Map[String, String] = value match {
			case s: String if s.hasText => Map(name -> (value + "*"))
			case _ => Map.empty
		}
	}
}
