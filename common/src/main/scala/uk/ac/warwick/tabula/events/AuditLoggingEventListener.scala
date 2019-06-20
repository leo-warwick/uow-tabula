package uk.ac.warwick.tabula.events

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.util.logging.AuditLogger
import uk.ac.warwick.util.logging.AuditLogger.{Field, RequestInformation}
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.collection.JavaConverters._

/**
  * An event listener that sends events to CLogS using WarwickUtils' AuditLogger
  */
class AuditLoggingEventListener extends EventListener {

  val logger: AuditLogger = AuditLogger.getAuditLogger("tabula")

  override def beforeCommand(event: Event): Unit = {}

  override def onException(event: Event, exception: Throwable): Unit = {}

  // Currently we only want afters
  override def afterCommand(event: Event, returnValue: Any, beforeEvent: Event): Unit = {
    var info = RequestInformation.forEventType(event.name)
    event.realUserId.maybeText.foreach { realUserId => info = info.withUsername(realUserId) }
    event.userAgent.maybeText.foreach { userAgent => info = info.withUserAgent(userAgent) }
    event.ipAddress.maybeText.foreach { ipAddress => info = info.withIpAddress(ipAddress) }

    // We need to convert all Scala collections into Java collections
    def handle(in: Any): AnyRef = (in match {
      case Some(x: Object) => handle(x)
      case Some(null) => null
      case None => null
      case jcol: java.util.Collection[_] => jcol.asScala.map(handle).asJavaCollection
      case jmap: JMap[_, _] => jmap.asScala.mapValues(handle).asJava
      case smap: scala.collection.SortedMap[_, _] => JLinkedHashMap(smap.mapValues(handle).toSeq: _*)
      case lmap: scala.collection.immutable.ListMap[_, _] => JLinkedHashMap(lmap.mapValues(handle).toSeq: _*)
      case lmap: scala.collection.mutable.ListMap[_, _] => JLinkedHashMap(lmap.mapValues(handle).toSeq: _*)
      case smap: scala.collection.Map[_, _] => mapAsJavaMapConverter(smap.mapValues(handle)).asJava
      case sseq: scala.Seq[_] => seqAsJavaListConverter(sseq.map(handle)).asJava
      case scol: scala.Iterable[_] => asJavaCollectionConverter(scol.map(handle)).asJavaCollection
      case other: AnyRef => other
      case _ => null
    }) match {
      case null => "-"
      case notNull => notNull
    }

    val data = (beforeEvent.extra ++ event.extra).map { case (k, v) => new Field(k) -> handle(v) }

    logger.log(info, data.asJava)
  }
}
