package uk.ac.warwick.tabula.services

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.joda.time.DateTime
import org.springframework.beans.BeanWrapperImpl
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.Reactor
import uk.ac.warwick.tabula.Reactor.EventSource
import uk.ac.warwick.tabula.commands.Describable
import uk.ac.warwick.tabula.events.{Event, EventDescription}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.system.exceptions.HandledException
import uk.ac.warwick.util.queue.conversion.ItemType
import uk.ac.warwick.util.queue.{Queue, QueueListener}

import scala.beans.BeanProperty

trait MaintenanceStatus {
  def enabled: Boolean

  def until: Option[DateTime]

  def message: Option[String]
}

trait MaintenanceModeService extends MaintenanceStatus {

  /** Enable maintenance mode. **/
  def enable: Unit

  /** Disable maintenance mode. **/
  def disable: Unit

  /** Returns whether maintenance mode is enabled. */
  def enabled: Boolean

  /**
    * An EventSource to which you can attach a listener to find
    * out when maintenance mode goes on and off.
    */
  val changingState: Reactor.EventSource[Boolean]

  /**
    * Returns an Exception object suitable for throwing when trying
    * to do an unsupported op while in maintenance mode. Only returns
    * it; you need to throw it yourself. Like a dog!
    */
  def exception(callee: Describable[_]): Exception

  var until: Option[DateTime]
  var message: Option[String]

  def update(message: MaintenanceModeMessage): MaintenanceModeService = {
    this.message = Option(message.message)
    this.until = message.until match {
      case -1 => None
      case millis => Some(new DateTime(millis))
    }

    if (message.enabled) this.enable
    else this.disable

    this
  }
}

@Service
class MaintenanceModeServiceImpl extends MaintenanceModeService with Logging {
  @Value("${environment.standby}") var _enabled: Boolean = _

  def enabled: Boolean = _enabled

  var until: Option[DateTime] = None
  var message: Option[String] = None

  // for other classes to listen to changes to maintenance mode.
  val changingState: EventSource[Boolean] = Reactor.EventSource[Boolean]()

  def exception(callee: Describable[_]): MaintenanceModeEnabledException = {
    val m = EventDescription.generateMessage(Event.fromDescribable(callee))
    logger.info("[Maintenance Reject] " + m)

    new MaintenanceModeEnabledException(until, message)
  }

  private def notEnabled = new IllegalStateException("Maintenance not enabled")

  def enable: Unit = {
    if (!_enabled) {
      _enabled = true
      changingState.emit(_enabled)
    }
  }

  def disable: Unit = {
    if (_enabled) {
      _enabled = false
      changingState.emit(_enabled)
    }
  }
}

trait MaintenanceModeServiceComponent {
  def maintenanceModeService: MaintenanceModeService
}

trait AutowiringMaintenanceModeServiceComponent extends MaintenanceModeServiceComponent {
  var maintenanceModeService: MaintenanceModeService = Wire[MaintenanceModeService]
}

trait SettingsSyncQueueComponent {
  def settingsSyncQueue: Queue
}

trait AutowiringSettingsSyncQueueComponent extends SettingsSyncQueueComponent {
  var settingsSyncQueue: Queue = Wire.named[Queue]("settingsSyncTopic")
}

/**
  * Exception thrown when a command tries to run during
  * maintenance mode, and it's not readonly. The view handler
  * should handle this exception specially, showing a nice message.
  *
  * Holds onto some info about the maintenance, since error views are
  * only provided with the thrown exception.
  */
class MaintenanceModeEnabledException(val until: Option[DateTime], val message: Option[String])
  extends RuntimeException
    with HandledException {

  def getMessageOrEmpty: String = message.getOrElse("")

}

@ItemType("MaintenanceMode")
@JsonAutoDetect
class MaintenanceModeMessage {
  // Warning: If you make this more complicated, you may break the Jackson auto-JSON stuff for the MaintenanceModeController

  def this(enabled: Boolean, until: Option[DateTime], message: Option[String]) = {
    this()

    val bean = new BeanWrapperImpl(this)

    bean.setPropertyValue("enabled", enabled)
    bean.setPropertyValue("until", until.map(_.getMillis).getOrElse(-1))
    bean.setPropertyValue("message", message.orNull)

  }

  @BeanProperty var enabled: Boolean = _
  @BeanProperty var until: Long = _
  @BeanProperty var message: String = _
}

class MaintenanceModeListener extends QueueListener with InitializingBean with Logging with AutowiringMaintenanceModeServiceComponent {

  var queue: Queue = Wire.named[Queue]("settingsSyncTopic")

  override def isListeningToQueue = true

  override def onReceive(item: Any): Unit = {
    item match {
      case copy: MaintenanceModeMessage => maintenanceModeService.update(copy)
      case _ =>
    }
  }

  override def afterPropertiesSet: Unit = {
    queue.addListener(classOf[MaintenanceModeMessage].getAnnotation(classOf[ItemType]).value, this)
  }

}
