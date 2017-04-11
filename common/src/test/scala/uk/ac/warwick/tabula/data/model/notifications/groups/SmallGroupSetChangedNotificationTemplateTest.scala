package uk.ac.warwick.tabula.data.model.notifications.groups

import org.joda.time.LocalTime
import org.mockito.Matchers._
import uk.ac.warwick.tabula.JavaImports.JHashMap
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, SmallGroupEvent}
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, ScalaFreemarkerConfiguration}
import uk.ac.warwick.tabula.{FreemarkerTestHelpers, SmallGroupEventBuilder, SmallGroupFixture, TestBase}

class SmallGroupSetChangedNotificationTemplateTest extends TestBase with FreemarkerTestHelpers with FreemarkerRendering {

    private trait NotificationFixture extends SmallGroupFixture {

      val weekRangeFormatter = new StubFreemarkerMethodModel
      val urlModel = new StubFreemarkerDirectiveModel
      val timeBuilder= new StubFreemarkerMethodModel

      // make group1 have 2 events (the default fixture only has 1) so we can verify iteration over
      // the group's events
      val event: SmallGroupEvent = new SmallGroupEventBuilder()
        .withDay(DayOfWeek.Monday)
        .withStartTime(new LocalTime(12,0,0,0))
        .withLocation("TEST").build
      event.group = group1
      group1.addEvent(event)


      implicit val config: ScalaFreemarkerConfiguration = newFreemarkerConfiguration(JHashMap(
        "url" -> urlModel,
        "weekRangesFormatter" -> weekRangeFormatter,
        "timeBuilder"->timeBuilder))
    }

  @Test
  def includesTheNameOfTheGroup{
    new NotificationFixture {
      val output: String =
        renderToString(SmallGroupSetChangedNotification.templateLocation,
          Map("groups" -> groupSet1.groups, "groupSet" -> groupSet1, "profileUrl" -> "profileUrl"))
      output should include(group1.name)
    }}

  @Test
  def includesTheCountOfStudents{new NotificationFixture {
    val output: String =
      renderToString(SmallGroupSetChangedNotification.templateLocation,
				Map("groups" -> groupSet1.groups, "groupSet" -> groupSet1, "profileUrl" -> "profileUrl"))
    output should include("2 students")
  }}

  @Test
  def callsWeekRangeFormatterForEachEvent() {
    new NotificationFixture {
      val output: String =
        renderToString(SmallGroupSetChangedNotification.templateLocation,
					Map("groups" -> groupSet1.groups, "groupSet" -> groupSet1, "profileUrl" -> "profileUrl"))
      verify(weekRangeFormatter.mock, times(2)).exec(anyList())
    }}

  @Test
  def formatsTimeNicelyForEachEvent(){
    new NotificationFixture {
      val output: String =
        renderToString(SmallGroupSetChangedNotification.templateLocation,
					Map("groups" -> groupSet1.groups, "groupSet" -> groupSet1, "profileUrl" -> "profileUrl"))
      verify(timeBuilder.mock, times(2)).exec(anyList())
    }}

  }
