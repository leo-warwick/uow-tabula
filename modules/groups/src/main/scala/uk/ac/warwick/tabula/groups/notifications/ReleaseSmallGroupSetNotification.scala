package uk.ac.warwick.tabula.groups.notifications

import uk.ac.warwick.tabula.data.model.{SingleRecipientNotification, Notification}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.UserLookupService
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.web.views.{TextRenderer, FreemarkerRendering}
import freemarker.template.Configuration

class ReleaseSmallGroupSetNotification(private val group:SmallGroup, val agent:User, private val _recipient:User, private val isStudent:Boolean ) extends Notification[SmallGroup] with SingleRecipientNotification {

  this: TextRenderer=>

  val templateLocation  = "/WEB-INF/freemarker/notifications/release_small_group_student_notification.ftl"

  val verb: String = "Release"
  val _object: SmallGroup = group
  val target: Option[AnyRef] = None

  def title: String = group.groupSet.format.description + " allocation"

  def content: String = {
    renderTemplate(templateLocation, Map("user"->_recipient, "group"->group, "profileUrl"->url) )
  }
  def url: String = {
    if (isStudent){
      Routes.profile.view(_recipient)
    }else{
      Routes.tutor.mygroups(_recipient)
    }
  }

  def recipient = _recipient

}
