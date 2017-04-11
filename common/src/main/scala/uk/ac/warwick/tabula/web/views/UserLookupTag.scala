package uk.ac.warwick.tabula.web.views

import freemarker.template.TemplateDirectiveModel
import freemarker.template.TemplateDirectiveBody
import freemarker.template.TemplateModel
import freemarker.core.Environment
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.UserLookupService
import freemarker.template.utility.DeepUnwrap
import uk.ac.warwick.tabula.JavaImports._
import freemarker.template.TemplateException
import collection.JavaConversions._

import scala.collection.JavaConverters._

/**
 * Accepts either id="abc" or ids=["abc","def"] as attributes.
 * Consequently sets a local variable -
 * 	either returned_user = a User
 *  or returned_users = a map of id -> User
 *
 * When users is set, missing_ids will also be a sequence of user IDs
 * that couldn't be found.
 *
 * Set the boolean parameter lookupByUniversityId to true (default false)
 * to search by university ID rather than usercode.
 */
class UserLookupTag extends TemplateDirectiveModel {

	@Autowired var userLookup: UserLookupService = _

	override def execute(env: Environment,
		params: JMap[_, _],
		loopVars: Array[TemplateModel],
		body: TemplateDirectiveBody): Unit = {

		val wrapper = env.getObjectWrapper()

		val user = unwrap[String](params.get("id"))
		val users = unwrap[JList[String]](params.get("ids"))
		val lookupByUniversityId = unwrap[JBoolean](params.get("lookupByUniversityId")).exists { _.booleanValue() }

		if (body == null) {
			throw new TemplateException("UserLookupTag: must have a body", env);
		}

		if (user.nonEmpty) {
			val userId = user.get

			val returnedUser = (if (lookupByUniversityId) userLookup.getUserByWarwickUniId(userId) else userLookup.getUserByUserId(userId))
			env.getCurrentNamespace().put("returned_user", wrapper.wrap(returnedUser))
		} else if (users.nonEmpty) {
			val userIds = users.get

			val returnedUsers =
				if (lookupByUniversityId) userLookup.getUsersByWarwickUniIds(userIds)
				else userLookup.getUsersByUserIds(userIds).asScala

			val missingUserIds =
				returnedUsers.values
					.filterNot(_.isFoundUser())
					.map { user => if (lookupByUniversityId) user.getWarwickId else user.getUserId }

			env.getCurrentNamespace().put("returned_users", wrapper.wrap(returnedUsers))
			env.getCurrentNamespace().put("missing_ids", wrapper.wrap(missingUserIds))
		} else {
			throw new TemplateException("UserLookupTag: Either user or users must be specified", env)
		}

		body.render(env.getOut())

	}

	def unwrap[A](obj: Any): Option[A] = Option(obj).map { obj =>
		DeepUnwrap.unwrap(obj.asInstanceOf[TemplateModel]).asInstanceOf[A]
	}

}