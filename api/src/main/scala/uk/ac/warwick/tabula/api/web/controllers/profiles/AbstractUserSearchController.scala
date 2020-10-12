package uk.ac.warwick.tabula.api.web.controllers.profiles

import java.util.Optional

import uk.ac.warwick.tabula.api.commands.profiles.UserSearchCommandState
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

abstract class AbstractUserSearchController extends ApiController with AutowiringProfileServiceComponent {

  protected def getCommand(academicYear: Optional[AcademicYear], user: CurrentUser): Appliable[Seq[String]]
  protected def resultKey: String

  final override def onPreRequest(): Unit = {
    session.enableFilter(Member.ActiveOnlyFilter)
    session.enableFilter(Member.FreshOnlyFilter)
  }

  final def doSearch(command: Appliable[Seq[String]] with UserSearchCommandState): Mav = {
    Mav(new JSONView(Map(
      "success" -> true,
      "status" -> "ok",
      "academicYear" -> command.academicYear,
      resultKey -> command.apply()
    )))
  }

}
