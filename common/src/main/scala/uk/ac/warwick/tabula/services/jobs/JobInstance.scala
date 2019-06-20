package uk.ac.warwick.tabula.services.jobs

import org.joda.time.DateTime
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._

import scala.collection.JavaConverters._

/**
  * Interface for a Job to update its status in the database.
  *
  * The main implementation of this is JobInstanceImpl which is
  * stores in the database.
  */
trait JobInstance {
  type JsonMap = Map[String, Any]

  var propsMap: Map[String, Any]

  def jobType: String

  var id: String

  def optInt(name: String): Option[Int] = propsMap.get(name).map(_.toString.toInt)

  def getString(name: String): String = propsMap(name).toString

  def setString(name: String, value: String): Unit = propsMap = propsMap + (name -> value)

  def getStrings(name: String): Seq[String] = propsMap(name) match {
    case seq: Seq[String]@unchecked => seq
    case jList => jList.asInstanceOf[JList[String]].asScala
  }

  def setStrings(name: String, value: Seq[String]): Unit = propsMap = propsMap + (name -> value.asJava)

  def getStringMap(name: String): Map[String, String] = propsMap(name) match {
    case map: Map[String, String]@unchecked => map
    case jMap => jMap.asInstanceOf[JMap[String, String]].asScala.toMap
  }

  def setStringMap(name: String, value: Map[String, String]): Unit = propsMap = propsMap + (name -> value.asJava)

  var createdDate: DateTime
  var updatedDate: DateTime

  var status: String
  var progress: Int

  var started: Boolean
  var finished: Boolean
  var succeeded: Boolean

  var schedulerInstance: String

  def json: JsonMap

  def user: CurrentUser

  def userId: String
}