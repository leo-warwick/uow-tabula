package uk.ac.warwick.tabula.api.web.helpers

import java.io.StringWriter
import java.util.concurrent.ConcurrentHashMap

import freemarker.template.Template
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ScalaConcurrentMapHelpers
import uk.ac.warwick.tabula.api.web.helpers.MemberToJsonConverter._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.web.views.ScalaFreemarkerConfigurationComponent

import scala.collection.JavaConverters._

/**
	* We use Freemarker to ensure that we use the same logic as in ScalaBeansWrapper, i.e. that we don't expose
	* any properties that the user doesn't have permissions to see.
	*/
trait MemberToJsonConverter {
	self: ScalaFreemarkerConfigurationComponent =>

	def jsonMemberObject(member: Member): Map[String, Any] = {
		def eval(el: String): Option[String] = {
			val template = templateCache.getOrElseUpdate(el, new Template(s"memberJsonApi://$el", el, freemarkerConfiguration))

			val writer = new StringWriter
			template.process(member, writer)
			writer.toString.maybeText
		}

		def str(name: String, propertyName: String): Option[(String, String)] =
			eval(s"$${($propertyName)!}").map { name -> _ }

		def date(name: String, propertyName: String): Option[(String, String)] =
			eval(s"$${($propertyName.toString())!}").map { name -> _ }

		def boolean(name: String, propertyName: String): Option[(String, Boolean)] =
			eval(s"$${($propertyName?string('true', 'false'))!}") match {
				case Some("true") => Some(name -> true)
				case Some("false") => Some(name -> false)
				case _ => None
			}

		def canViewProperty(propertyName: String): Boolean =
			eval(s"$${$propertyName???string('true', 'false')}").contains("true")

		def departmentToJson(department: Department): Map[String, Any] =
			Map("code" -> department.code, "name" -> department.name)

		val memberProperties = Seq(
			str("universityId", "universityId"),
			str("userId", "userId"),
			str("firstName", "firstName"),
			str("lastName", "lastName"),
			str("email", "email"),
			str("userType", "userType.description"),
			str("fullName", "fullName"),
			str("officialName", "officialName"),
			str("homeEmail", "homeEmail"),
			str("fullFirstName", "fullFirstName"),
			str("title", "title"),
			str("gender", "gender.description"),
			str("inUseFlag", "inUseFlag"),
			str("jobTitle", "jobTitle"),
			str("phoneNumber", "phoneNumber"),
			str("nationality", "nationality"),
			str("mobileNumber", "mobileNumber"),
			str("groupName", "groupName"),
			if (canViewProperty("affiliatedDepartments"))
				Some("affiliatedDepartments" -> member.affiliatedDepartments.map(departmentToJson))
			else None,
			if (canViewProperty("touchedDepartments"))
				Some("touchedDepartments" -> member.touchedDepartments.map(departmentToJson))
			else None,
			if (canViewProperty("homeDepartment"))
				Some("homeDepartment", departmentToJson(member.homeDepartment))
			else None,
			date("inactivationDate", "inactivationDate"),
			date("dateOfBirth", "dateOfBirth")
		).flatten.toMap

		def addressToJson(address: Address): Map[String, Any] =
			Seq(
				address.line1.maybeText.map { "line1" -> _ },
				address.line2.maybeText.map { "line2" -> _ },
				address.line3.maybeText.map { "line3" -> _ },
				address.line4.maybeText.map { "line4" -> _ },
				address.line5.maybeText.map { "line5" -> _ },
				address.postcode.maybeText.map { "postcode" -> _ },
				address.telephone.maybeText.map { "telephone" -> _ }
			).flatten.toMap

		def nextOfKinToJson(nextOfKin: NextOfKin): Map[String, Any] =
			Seq(
				nextOfKin.firstName.maybeText.map { "firstName" -> _ },
				nextOfKin.lastName.maybeText.map { "lastName" -> _ },
				nextOfKin.relationship.maybeText.map { "relationship" -> _ }
			).flatten.toMap

		def disabilityToJson(disability: Disability): Map[String, Any] = Map(
			"code" -> disability.code,
			"sitsDefinition" -> disability.sitsDefinition,
			"definition" -> disability.definition
		)

		val studentProperties = member match {
			case student: StudentMember => Seq(
				if (canViewProperty("homeAddress"))
					Some("homeAddress" -> addressToJson(student.homeAddress))
				else None,
				if (canViewProperty("termtimeAddress"))
					Some("termtimeAddress" -> addressToJson(student.termtimeAddress))
				else None,
				if (canViewProperty("nextOfKins"))
					Some("nextOfKins" -> student.nextOfKins.asScala.map(nextOfKinToJson))
				else None,
				if (canViewProperty("disability"))
					student.disability.filter(_.reportable).map { "disability" -> disabilityToJson(_) }
				else None,
				boolean("tier4VisaRequirement", "tier4VisaRequirement")
			).flatten.toMap
			case _ => Map()
		}

		val staffProperties = member match {
			case staff @ (_: StaffMember | _: EmeritusMember) => Map() // no additional properties
			case _ => Map()
		}

		memberProperties ++ studentProperties ++ staffProperties
	}

}

object MemberToJsonConverter {
	val templateCache: ConcurrentHashMap[String, Template] with ScalaConcurrentMapHelpers[String, Template] = JConcurrentMap[String, Template]()
}
