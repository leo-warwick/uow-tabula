package uk.ac.warwick.tabula.commands.groups.admin

import org.joda.time.LocalTime
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek.Tuesday
import uk.ac.warwick.tabula.data.model.groups.EventDeliveryMethod.Hybrid
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.{Fixtures, TestBase}

import scala.jdk.CollectionConverters._

class ModifySmallGroupEventTest extends TestBase  {

  private trait ValidationFixture {
    val validator: ModifySmallGroupEventValidation with ModifySmallGroupEventCommandState = new ModifySmallGroupEventValidation with ModifySmallGroupEventCommandState {
      override val module: Module = Fixtures.module("in101", "module")
      override val set: SmallGroupSet = Fixtures.smallGroupSet("set")
      override val group: SmallGroup = Fixtures.smallGroup("group")
      override val existingEvent: Option[SmallGroupEvent] = None
      override val isImport: Boolean = false

      // pre-populate with valid values
      day = Tuesday
      startTime = LocalTime.parse("13:00:00")
      endTime = LocalTime.parse("14:00:00")
      weekRanges = Seq(WeekRange(1,1))
      deliveryMethod = Hybrid
    }
  }

  @Test def validationPasses(): Unit = {
    new ValidationFixture {
      var errors = new BindException(validator, "command")
      validator.validate(errors)
      errors.hasErrors should be(false)
    }
  }

  @Test def validateUrlTitleTooLong(): Unit = {
    new ValidationFixture {
      var errors = new BindException(validator, "command")
      validator.relatedUrlTitle = (1 to 256).map { _ => "x"}.mkString("")
      validator.validate(errors)
      errors.hasErrors should be (true)
      errors.getErrorCount should be(1)
      errors.getFieldErrors.asScala.map(_.getField).contains("relatedUrlTitle") should be(true)
      errors.getFieldError.getCode should be("smallGroupEvent.urlTitle.tooLong")
    }
  }
}
