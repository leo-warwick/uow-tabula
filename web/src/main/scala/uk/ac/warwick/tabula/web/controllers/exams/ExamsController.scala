package uk.ac.warwick.tabula.web.controllers.exams

import uk.ac.warwick.tabula.data.model.{Member, RuntimeMember}
import uk.ac.warwick.tabula.web.controllers.{CurrentMemberComponent, BaseController}

abstract class ExamsController extends BaseController with ExamsBreadcrumbs with CurrentMemberComponent {
	final def optionalCurrentMember: Option[Member] = user.profile
	final def currentMember: Member = optionalCurrentMember getOrElse new RuntimeMember(user)
}