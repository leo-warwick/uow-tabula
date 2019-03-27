package uk.ac.warwick.tabula.mitcircs.web

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.web.Routes.context
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.web.RoutesUtils

/**
  * Generates URLs to various locations, to reduce the number of places where URLs
  * are hardcoded and repeated.
  *
  * For methods called "apply", you can leave out the "apply" and treat the object like a function.
  */
object Routes {

  import RoutesUtils._

  private val context = "/mitcircs"

  def home: String = context + "/"


  object Student {
    def home: String = context + "/profile"
    def home(student: StudentMember): String = home + "/%s" format encoded(student.universityId)
  }

}
