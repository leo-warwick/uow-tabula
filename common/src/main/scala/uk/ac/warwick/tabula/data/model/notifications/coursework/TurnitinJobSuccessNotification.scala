package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, SingleRecipientNotification}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

@Entity
@Proxy
@DiscriminatorValue(value = "TurnitinJobSuccess")
class TurnitinJobSuccessNotification
  extends TurnitinReportNotification
    with SingleRecipientNotification {

  def title: String = "%s: The Turnitin check for \"%s\" has finished".format(assignment.module.code.toUpperCase, assignment.name)

  def content: FreemarkerModel = {
    val (successfulReports, failedReports) = items.asScala.map(_.entity).partition(_.reportReceived)
    FreemarkerModel("/WEB-INF/freemarker/emails/turnitinjobdone.ftl", Map(
      "assignment" -> assignment,
      "assignmentTitle" -> ("%s - %s".format(assignment.module.code.toUpperCase, assignment.name)),
      "failureCount" -> failedReports.size,
      "successCount" -> successfulReports.size,
      "totalCount" -> items.size(),
      "failedReports" -> failedReports,
      "path" -> url
    ))
  }

  def recipient: User = agent
}
