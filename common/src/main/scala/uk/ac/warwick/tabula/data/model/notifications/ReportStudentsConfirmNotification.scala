package uk.ac.warwick.tabula.data.model.notifications

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointReport
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.userlookup.User

@Entity
@Proxy(`lazy` = false)
@DiscriminatorValue("ReportStudentsConfirmCommandNotification")
class ReportStudentsConfirmNotification extends Notification[MonitoringPointReport, Unit]
  with SingleRecipientNotification
  with AutowiringUserLookupComponent
  with MyWarwickNotification {

  @transient
  lazy val RecipientUsercode: String = Wire.optionProperty("${sits.notificationrecipient}").getOrElse(
    throw new IllegalStateException("sits.notificationrecipient property is missing")
  )

  @transient
  val templateLocation = "/WEB-INF/freemarker/emails/missed_monitoring_to_sits_email.ftl"

  override def verb: String = "view"

  override def title: String = "Missed monitoring points have been uploaded to SITS"


  override def content: FreemarkerModel = FreemarkerModel(templateLocation, Map(
    "numberOfStudentUpdated" -> entities.size,
    "academicYear" -> s"${entities.head.academicYear.startYear}/${entities.head.academicYear.endYear}",
    "monitoringPeriod" -> entities.head.monitoringPeriod,
    "agent" -> agent.getUserId,
    "agentDept" -> agent.getDepartment,
    "studentDepartments" -> entities.map(_.studentCourseDetails.department.fullName).distinct.mkString(", "),
    "created" -> created.toString(DateFormats.NotificationDateTimePattern)
  ))

  override def url: String = "https://warwick.ac.uk/tabula/manual/monitoring-points/upload-to-sits"

  override def urlTitle: String = "learn more about uploading missed monitoring points to SITS"

  override def recipient: User = userLookup.getUserByUserId(RecipientUsercode)

}
