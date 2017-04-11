package uk.ac.warwick.tabula.web.controllers.coursework

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.data.model.{Member, RuntimeMember}

abstract class OldCourseworkController extends BaseController with CourseworkBreadcrumbs with CurrentMemberComponent {
	final def optionalCurrentMember: Option[Member] = user.profile
	final def currentMember: Member = optionalCurrentMember.getOrElse(new RuntimeMember(user))
}

trait CurrentMemberComponent {
	def optionalCurrentMember: Option[Member]
	def currentMember: Member

	final var urlPrefix: String = Wire.property("${cm1.prefix}")
}