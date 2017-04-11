package uk.ac.warwick.tabula.helpers.profiles

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

import scala.collection.JavaConversions._

trait SearchJSONHelpers {

	val formMav: Mav

	def toJson(profiles: Seq[Member]): Seq[Map[String, String]] = {
		def memberToJson(member: Member) = Map[String, String](
			"name" -> {member.fullName match {
				case None => "[Unknown user]"
				case Some(name) => name
			}},
			"id" -> member.universityId,
			"userId" -> member.userId,
			"description" -> member.description)

		profiles.map(memberToJson(_))
	}

	def submitJson(cmd: Appliable[Seq[Member]], errors: Errors): Mav = {
		if (errors.hasErrors) {
			formMav
		} else {
			val profilesJson: JList[Map[String, Object]] = toJson(cmd.apply())

			Mav(new JSONView(profilesJson))
		}
	}

	def submit(cmd: Appliable[Seq[Member]], errors: Errors, path: String): Mav = {
		if (errors.hasErrors) {
			formMav
		} else {
			Mav(path,
				"results" -> cmd.apply())
		}
	}


}
