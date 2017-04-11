package uk.ac.warwick.tabula.web.controllers.reports

import uk.ac.warwick.tabula.data.model.{Member, RuntimeMember}
import uk.ac.warwick.tabula.web.controllers.{CurrentMemberComponent, BaseController}

/**
 * Base class for controllers in Reports.
 */
abstract class ReportsController extends BaseController with ReportsBreadcrumbs with CurrentMemberComponent {

	final def optionalCurrentMember: Option[Member] = user.profile
	final def currentMember: Member = optionalCurrentMember getOrElse new RuntimeMember(user)

}
