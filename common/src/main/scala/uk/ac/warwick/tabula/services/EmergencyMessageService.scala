package uk.ac.warwick.tabula.services

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.springframework.beans.BeanWrapperImpl
import org.springframework.beans.factory.InitializingBean
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

trait EmergencyMessageStatus {
  def enabled: Boolean

  def message: Option[String]
}

trait EmergencyMessageService extends EmergencyMessageStatus {

  /** Enable emergency message. **/
  def enable: Unit

  /** Disable emergency message. **/
  def disable: Unit

  /** Returns whether emergency message is enabled. */
  def enabled: Boolean

  /**
    * An EventSource to which you can attach a listener to find
    * out when emergency message goes on and off.
    */
  val changingState: Reactor.EventSource[Boolean]

  /**
    * Returns an Exception object suitable for throwing when trying
    * to do an unsupported op while in emergency message. Only returns
    * it; you need to throw it yourself. Like a dog!
    */
  def exception(callee: Describable[_]): Exception

  var message: Option[String]

  def update(message: EmergencyMessage): EmergencyMessageService = {
    this.message = Option(message.message)
    if (message.enabled) this.enable
    else this.disable
    this
  }
}

@Service
class EmergencyMessageServiceImpl extends EmergencyMessageService with Logging {
  var _enabled: Boolean = false

  def enabled: Boolean = _enabled

  var message: Option[String] = None

  // for other classes to listen to changes to emergency messahe.
  val changingState: EventSource[Boolean] = Reactor.EventSource[Boolean]()

  def exception(callee: Describable[_]): EmergencyMessageServiceEnabledException = {
    val m = EventDescription.generateMessage(Event.fromDescribable(callee))
    logger.info("[Emergency Message Reject] " + m)

    new EmergencyMessageServiceEnabledException(message)
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

trait EmergencyMessageServiceComponent {
  def emergencyMessageService: EmergencyMessageService
}

trait AutowiringEmergencyMessageServiceComponent extends EmergencyMessageServiceComponent {
  val emergencyMessageService: EmergencyMessageService = Wire[EmergencyMessageService]
}

/**
  * Exception thrown when a command tries to run during
  * emergency message, and it's not readonly. The view handler
  * should handle this exception specially, showing a nice message.
  *
  * Holds onto some info about the maintenance, since error views are
  * only provided with the thrown exception.
  */
class EmergencyMessageServiceEnabledException(val message: Option[String])
  extends RuntimeException
    with HandledException {

  def getMessageOrEmpty: String = message.getOrElse("")

}

class CannotPerformWriteOperationException(callee: Describable[_])
  extends RuntimeException with HandledException

@ItemType("EmergencyMessage")
@JsonAutoDetect
class EmergencyMessage {
  // Warning: If you make this more complicated, you may break the Jackson auto-JSON stuff for the EmergencyMessageController

  def this(enabled: Boolean, message: Option[String]) = {
    this()

    val bean = new BeanWrapperImpl(this)

    bean.setPropertyValue("enabled", enabled)
    bean.setPropertyValue("message", message.orNull)

  }

  @BeanProperty var enabled: Boolean = _
  @BeanProperty var message: String = _
}

class EmergencyMessageListener extends QueueListener with InitializingBean with Logging with AutowiringEmergencyMessageServiceComponent {

  var queue: Queue = Wire.named[Queue]("settingsSyncTopic")

  override def isListeningToQueue = true

  override def onReceive(item: Any): Unit = {
    item match {
      case copy: EmergencyMessage => emergencyMessageService.update(copy)
      case _ =>
    }
  }

  override def afterPropertiesSet: Unit = {
    queue.addListener(classOf[EmergencyMessage].getAnnotation(classOf[ItemType]).value, this)
  }

}
