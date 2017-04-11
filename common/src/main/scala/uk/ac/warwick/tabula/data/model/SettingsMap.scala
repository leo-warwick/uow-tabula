package uk.ac.warwick.tabula.data.model

import javax.persistence.Lob

import org.hibernate.annotations.Type
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.data.model.HasSettings._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.userlookup.User

object HasSettings {
	trait Setting[A] {
		def key: String
		def default: A
		def allSettings: HasSettings

		def value: A
		def isDefined: Boolean = allSettings.getSetting(key).isDefined
		def value_=(v: A) { allSettings.settings += key -> v }
	}

	/** Wrapper for get/put operations on a given string setting. */
	case class StringSetting(allSettings: HasSettings)(val key: String, val default: String) extends Setting[String] {
		def value: String = allSettings.getStringSetting(key, default)
	}

	case class IntSetting(allSettings: HasSettings)(val key: String, val default: Int) extends Setting[Int] {
		def value: Int = allSettings.getIntSetting(key, default)
	}

	case class BooleanSetting(allSettings: HasSettings)(val key: String, val default: Boolean) extends Setting[Boolean] {
		def value: Boolean = allSettings.getBooleanSetting(key, default)
	}

	case class StringMapSetting(allSettings: HasSettings)(val key: String, val default: Map[String, String]) extends Setting[Map[String, String]] {
		def value: Map[String, String] = allSettings.getStringMapSetting(key, default)
	}

	case class StringSeqSetting(allSettings: HasSettings)(val key: String, val default: Seq[String]) extends Setting[Seq[String]] {
		def value: Seq[String] = allSettings.getStringSeqSetting(key, default)
	}

	case class UserSeqSetting(allSettings: HasSettings)(val key: String, val default: Seq[User], userLookup: UserLookupService) extends Setting[Seq[User]] {
		def value: Seq[User] = allSettings.getStringSeqSetting(key).map(_.map(userLookup.getUserByUserId).filter(_.isFoundUser)).getOrElse(default)
		override def value_=(v: Seq[User]) { allSettings.settings += key -> v.map { _.getUserId }.filter { _.hasText }.toList }
	}
}

trait HasSettings {

	@Lob
	@Type(`type` = "uk.ac.warwick.tabula.data.model.JsonMapUserType")
	protected var settings: Map[String, Any] = Map()

	def settingsIterator: Iterator[(String, Any)] = settings.iterator

	protected def getSetting(key: String): Option[Any] = Option(settings).flatMap { _.get(key) }

	protected def getStringSetting(key: String): Option[String] = getSetting(key) match {
		case Some(value: String) => Some(value)
		case _ => None
	}
	protected def getIntSetting(key: String): Option[Int] = getSetting(key) match {
		case Some(value: Int) => Some(value)
		case _ => None
	}
	protected def getBooleanSetting(key: String): Option[Boolean] = getSetting(key) match {
		case Some(value: Boolean) => Some(value)
		case _ => None
	}
	protected def getStringSeqSetting(key: String): Option[Seq[String]] = getSetting(key) match {
		case Some(value: Seq[_]) => Some(value.asInstanceOf[Seq[String]])
		case _ => None
	}
	protected def getStringMapSetting(key: String): Option[Map[String, String]] = {
		parseMapFromAny(getSetting(key)).map(_.mapValues(_.asInstanceOf[String]))
	}
	protected def getUserSeqSetting(key: String): Option[Seq[String]] = getSetting(key) match {
		case Some(value: Seq[_]) => Some(value.asInstanceOf[Seq[String]])
		case _ => None
	}

	protected def parseMapFromAny(possibleMapOption: Option[Any]): Option[Map[String, Any]] = possibleMapOption match {
		case Some(value: Map[_, _]) => Some(value.asInstanceOf[Map[String, Any]])
		case Some(value: collection.mutable.Map[_, _]) => Some(value.toMap.asInstanceOf[Map[String, String]])
		case _ => None
	}

	def StringSetting: (String, String) => StringSetting = HasSettings.StringSetting(this)(_, _)
	def StringMapSetting: (String, Map[String, String]) => StringMapSetting = HasSettings.StringMapSetting(this)(_, _)
	def StringSeqSetting: (String, Seq[String]) => StringSeqSetting = HasSettings.StringSeqSetting(this)(_, _)
	def IntSetting: (String, Int) => IntSetting = HasSettings.IntSetting(this)(_, _)
	def BooleanSetting: (String, Boolean) => BooleanSetting = HasSettings.BooleanSetting(this)(_, _)
	def UserSeqSetting: (String, Seq[User], UserLookupService) => UserSeqSetting = HasSettings.UserSeqSetting(this)(_, _, _)

	protected def getStringSetting(key: String, default: => String): String = getStringSetting(key).getOrElse(default)
	protected def getIntSetting(key: String, default: => Int): Int = getIntSetting(key).getOrElse(default)
	protected def getBooleanSetting(key: String, default: => Boolean): Boolean = getBooleanSetting(key).getOrElse(default)
	protected def getStringSeqSetting(key: String, default: => Seq[String]): Seq[String] = getStringSeqSetting(key).getOrElse(default)
	protected def getStringMapSetting(key: String, default: => Map[String, String]): Map[String, String] = getStringMapSetting(key).getOrElse(default)

	protected def settingsSeq: Seq[(String, Any)] = Option(settings).map { _.toSeq }.getOrElse(Nil)

	protected def ensureSettings {
		if (settings == null) settings = Map()
	}

	protected def nestedSettings(path: String): NestedSettings =
		new NestedSettings(this, path)
}

class NestedSettings(parent: HasSettings, path: String) extends ToString {
	def StringSetting(key: String, default: String): StringSetting = HasSettings.StringSetting(parent)(s"$path.$key", default)
	def StringMapSetting(key: String, default: Map[String, String]): StringMapSetting = HasSettings.StringMapSetting(parent)(s"$path.$key", default)
	def StringSeqSetting(key: String, default: Seq[String]): StringSeqSetting = HasSettings.StringSeqSetting(parent)(s"$path.$key", default)
	def IntSetting(key: String, default: Int): IntSetting = HasSettings.IntSetting(parent)(s"$path.$key", default)
	def BooleanSetting(key: String, default: Boolean): BooleanSetting = HasSettings.BooleanSetting(parent)(s"$path.$key", default)
	def UserSeqSetting(key: String, default: Seq[User], userLookup: UserLookupService): UserSeqSetting = HasSettings.UserSeqSetting(parent)(s"$path.$key", default, userLookup)

	def toStringProps: List[(String, Any)] = parent.settingsIterator.toSeq.filter { case (key, _) => key.startsWith(path + ".") }.map { case (key, v) => key.substring(path.length + 1) -> v }.toList
}

/**
 * Danger!
 *
 * Exposing the ++= methods publicly means that other classes can directly manipulate the keys of this map.
 * If your class expects keys to have certain specified values (e.g. enums) then mix in HasSettings and
 * expose type-safe getters and setters instead of mixing in SettingsMap directly
 */

trait SettingsMap extends HasSettings {

	protected def -=(key: String): this.type = {
		settings -= key
		this
	}

	protected def +=(kv: (String, Any)): this.type = {
		settings += kv
		this
	}

	protected def ++=(sets: (String, Any)*): this.type = {
		settings ++= sets
		this
	}

	def ++=(other: SettingsMap): this.type = {
		this.settings ++= other.settings
		this
	}
}