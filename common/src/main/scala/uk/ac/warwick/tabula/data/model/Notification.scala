package uk.ac.warwick.tabula.data.model

import javax.persistence._

import org.hibernate.ObjectNotFoundException
import org.hibernate.annotations.{BatchSize, Type}
import org.joda.time.DateTime
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.util.Assert
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.convert.FetchByUniIdOrUsercode
import uk.ac.warwick.tabula.data.model.notifications.RecipientNotificationInfo
import uk.ac.warwick.tabula.helpers.FoundUser
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.userlookup.User

import scala.beans.BeanProperty
import scala.collection.JavaConverters._

object Notification {

	/**
	 * By default, the priority that a notification must be at or above in order to generate an email.
	 */
	val PriorityEmailThreshold = NotificationPriority.Info

	/**
	 * A little explanation...
	 * Without this, every single Notification class needs a constructor that calls a super
	 * init class, which is annoying boilerplate. Since they all have an empty constructor
	 * for Hibernate, this method can be used to fill in the required properties on a new
	 * instance of any Notification.
	 *
	 * e.g.
	 *
	 *    Notification.init(new SubmissionNotification, agent, Seq(submission), assignment)
	 *
	 * If the compiler complains about a call to init, it's usually one of these things
	 *  - the item type doesn't implement ToEntityReference
	 *  - the target type doesn't implement ToEntityReference
	 *  - there's a target but the notification doesn't implement NotificationWithTarget
	 *  - the types do not exactly match (if your notification entity is MeetingRecord,
	 *    you have to pass it a MeetingRecord var, it will get confused if you pass it a
	 *    ScheduledMeetingRecord even though it's a subtype. You can still pass it a subtype,
	 *    just have to cast it or put it in an appropriately typed var)
	 *
	 * Some of the above may be solvable things caused by our inability to understand type system.
	 *
	 * "item" (or "items" in the case of multiple items) is the persisted entity the notification draws information from
	 * e.g. Feedback
	 * "target" is a secondary persisted item you might want to draw information from e.g. Assignment - normally "target"
	 * is a parent of "item"
	 */

	// factory for multiple items with a target
	def init[A >: Null <: ToEntityReference, B >: Null <: ToEntityReference, C <: NotificationWithTarget[A, B]]
			(notification: C, agent: User, items: Seq[A], target: B): C = {
		notification.created = DateTime.now
		notification.agent = agent
		if (target != null) {
			notification.target = target.toEntityReference.asInstanceOf[EntityReference[B]]
		}
		notification.addItems(items)
		notification
	}

	// factory for single items with a target
	def init[A >: Null <: ToEntityReference, B >: Null <: ToEntityReference, C <: NotificationWithTarget[A, B]]
		(notification: C, agent: User, item: A, target: B): C = init[A,B,C](notification, agent, Seq(item), target)

	// factory for multiple items without a target
	def init[A >: Null <: ToEntityReference, C <: Notification[A, Unit]]
			(notification: C, agent: User, items: Seq[A]): C = {
		notification.created = DateTime.now
		notification.agent = agent
		notification.addItems(items)
		notification
	}

	// factory for single items without a target
	def init[A >: Null <: ToEntityReference, C <: Notification[A, Unit]]
		(notification: C, agent: User, item: A): C = init[A,C](notification, agent, Seq(item))

}

/**
 * Notifications have a similar structure to Open Social Activities
 * One of the common things we will want to do with notifications is
 * feed them into Open Social activity streams.
 *
 * A notification could be generated when a student submits an assignment
 * in this case ....
 * agent = the student submitting the assignment
 * verb = submit
 * _object = the submission
 * target = the assignment that we are submitting to
 * title = "Submission made"
 * content = "X made a submission to assignment Y on {date_time}"
 * url = "/path/to/assignment/with/this/submission/highlighted"
 * recipients = who is interested in this notification
 */
@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="notification_type")
abstract class Notification[A >: Null <: ToEntityReference, B]
	extends GeneratedId with Serializable with HasSettings with PermissionsTarget with NotificationPreSaveBehaviour {

	self: MyWarwickDiscriminator =>

	@transient final val dateOnlyFormatter = DateFormats.NotificationDateOnly
	@transient final val dateTimeFormatter = DateFormats.NotificationDateTime

	def permissionsParents: Stream[Nothing] = Stream.empty

	@Column(nullable=false)
	@Type(`type`="uk.ac.warwick.tabula.data.model.SSOUserType")
	final var agent: User = _ // the actor in open social activity speak

	@OneToMany(mappedBy="notification", fetch=FetchType.LAZY, targetEntity=classOf[EntityReference[_]], cascade=Array(CascadeType.ALL))
	@BatchSize(size = 1)
	var items: JList[EntityReference[A]] = JArrayList()

	def entities: Seq[A] = items.asScala.map(_.entity)

	var created: DateTime = _

	// the default priority is info. More important notifications should manually set this value to something higher.
	@Type(`type` = "uk.ac.warwick.tabula.data.model.NotificationPriorityUserType")
	var priority: NotificationPriority = NotificationPriority.Info

	// The priority, or if it is null then the default value of Info.
	def priorityOrDefault: NotificationPriority = Option(priority).getOrElse(NotificationPriority.Info)

	@OneToMany(mappedBy="notification", fetch=FetchType.LAZY, cascade=Array(CascadeType.ALL))
	var recipientNotificationInfos: JList[RecipientNotificationInfo] = JArrayList()

	// when performing operations on recipientNotificationInfos you should use this to fetch a users info.
	private def getOrCreateRecipientNotificationInfo(user: User) = {
		recipientNotificationInfos.asScala.find(_.recipient == user).getOrElse {
			val newInfo = new RecipientNotificationInfo(this, user)
			recipientNotificationInfos.add(newInfo)
			newInfo
		}
	}

	def dismiss(user: User): Unit = {
		user match {
			case FoundUser(_) =>
				val info = getOrCreateRecipientNotificationInfo(user)
				info.dismissed = true
			case _ =>
		}
	}

	def unDismiss(user: User): Unit = {
		user match {
			case FoundUser(_) =>
				val info = getOrCreateRecipientNotificationInfo(user)
				info.dismissed = false
			case _ =>
		}
	}

	def isDismissed(user: User): Boolean = recipientNotificationInfos.asScala.exists(ni => ni.recipient == user && ni.dismissed)

	@Column(name = "listeners_processed")
	private var _listenersProcessed: Boolean = false

	def markListenersProcessed(): Unit = {
		_listenersProcessed = true
	}

	// Not persisted, null for transient instances
	@Column(name = "notification_type", insertable = false, updatable = false)
	private var _notificationType: String = _
	def notificationType: String = _notificationType.maybeText.getOrElse(getClass.getAnnotation(classOf[DiscriminatorValue]).value)

	// HasSettings provides the JSONified settings field... ---> HERE <---

	@transient def verb: String
	@transient def title: String
	@transient def content: FreemarkerModel
	@transient def url: String

	/**
	 * URL title will be used to generate the links in notifications
	 *
	 * Activities will use - <a href=${url}>${urlTitle}</a>  (first letter of url title will be converted to upper case)
	 * Emails will use - Please visit [${url}] to ${urlTitle}
	 */
	@transient def urlTitle: String
	@transient def recipients: Seq[User]

	def addItems(seq: Seq[A]): Notification[A, B] = {
		val x = seq.map { _.toEntityReference }.asInstanceOf[Seq[EntityReference[A]]]
		x.foreach { _.notification = this }
		items.addAll(x.asJava)
		this
	}

	def safeTitle: Option[String] = try {
		Some(title)
	} catch {
		// Can happen if reference to an entity has since been deleted, e.g.
		// a submission is resubmitted and the old submission is removed.
		case _: ObjectNotFoundException => None
	}

	// This used to implement the trait that the Hibernate listener listens to.
	// But we call this manually instead, so it doesn't have that trait and isn't
	// called by Hibernate.
	final def preSave(newRecord: Boolean) {
		onPreSave(newRecord)
		// Generate recipientNotificationInfos for non-null recipients
		// (users could be null if inflating user entities that no longer exist in membership)
		recipients.flatMap(Option(_)).foreach(getOrCreateRecipientNotificationInfo)
	}
	def onPreSave(newRecord: Boolean) {}

	override def toString = s"Notification[${if (id != null) id else "transient " + hashCode}]{${Option(agent).fold("(no agent)") { _.getFullName }}, $verb, ${items.getClass.getSimpleName}}"
}

/**
 * A notification type that has a target must extend this class.
 * We used to have target in Notification but some types don't have a target,
 * and there'd be no valid type for B that could be set to a null EntityReference.
 * So for those types the target parameter is not defined.
 */
@Entity
abstract class NotificationWithTarget[A >: Null <: ToEntityReference, B >: Null <: AnyRef] extends Notification[A,B] {

	self: MyWarwickDiscriminator =>

	@Access(value=AccessType.PROPERTY)
	@OneToOne(cascade = Array(CascadeType.ALL), targetEntity = classOf[EntityReference[B]], fetch = FetchType.LAZY)
	@BeanProperty
	var target: EntityReference[B] = _
}

object FreemarkerModel {
	trait ContentType
	case object Plain extends ContentType
	case object Html extends ContentType
}
case class FreemarkerModel(template:String, model:Map[String,Any], contentType: FreemarkerModel.ContentType = FreemarkerModel.Plain)

trait SingleItemNotification[A >: Null <: ToEntityReference] {
	self: Notification[A, _] =>

	def item: EntityReference[A] =
		try {
			items.get(0)
		} catch {
			case _: IndexOutOfBoundsException => throw new ObjectNotFoundException("", "")
		}
}

/** Stores a single recipient as a User ID in the Notification table. */
trait UserIdRecipientNotification extends SingleRecipientNotification with NotificationPreSaveBehaviour {

	this : UserLookupComponent =>

	var recipientUserId: String = _
	def recipient: User = userLookup.getUserByUserId(recipientUserId)

	override def onPreSave(newRecord: Boolean) {
		Assert.notNull(recipientUserId, "recipientUserId must be set")
	}
}

trait NotificationPreSaveBehaviour {
	def onPreSave(newRecord: Boolean)
}

/** Stores a single recipient as a University ID in the Notification table. */
trait UniversityIdRecipientNotification extends SingleRecipientNotification with NotificationPreSaveBehaviour {
	this : UserLookupComponent =>

	var recipientUniversityId: String = _
	def recipient: User = userLookup.getUserByWarwickUniId(recipientUniversityId)

	override def onPreSave(newRecord: Boolean) {
		Assert.notNull(recipientUniversityId, "recipientUniversityId must be set")
	}
}

/**
	* NOPE!!! - Not to be used for new notifications
	*
	* If you need to support external users you must use UserIdRecipientNotification
	*
	* This is only here so we don't have to migrate a bunch of existing notification data as part of TAB-4838
	* The horrible thing about this is that 'recipientUniversityId' may will usercodes for these Notifications after this is merged
	*/
trait UniversityIdOrUserIdRecipientNotification extends SingleRecipientNotification with NotificationPreSaveBehaviour
	with FetchByUniIdOrUsercode {

	this : UserLookupComponent =>

	var recipientUniversityId: String = null
	def recipient: User = fetchUser(recipientUniversityId)

	override def onPreSave(newRecord: Boolean) {
		Assert.notNull(recipientUniversityId, "recipientIdentifier must be set")
	}
}

trait SingleRecipientNotification {
	def recipient: User
	def recipients: Seq[User] = {
		Seq(recipient)
	}
}

trait HasNotificationAttachment {
	def generateAttachments(helper: MimeMessageHelper): Unit
}

trait NotificationSettings extends NestedSettings {
	def enabled = BooleanSetting("enabled", default = true)
}

trait HasNotificationSettings {
	self: HasSettings =>

	def notificationSettings(notificationType: String): NotificationSettings =
		new NestedSettings(this, "notifications." + notificationType) with NotificationSettings
}

trait ConfigurableNotification {
	self: Notification[_, _] =>

	@transient private val userSettingsService = Wire[UserSettingsService]

	protected def departmentSettings: NotificationSettings = configuringDepartment.notificationSettings(notificationType)
	protected def userSettings(u: User): Option[NotificationSettings] = userSettingsService.getByUserId(u.getUserId).map { _.notificationSettings(notificationType) }

	private def enabledForDepartment: Boolean = departmentSettings.enabled.value
	private def enabledForUser(u: User): Boolean = userSettings(u).fold(true) { _.enabled.value }

	// The department related to the notification's entity - i.e. the one that configures this notification
	def configuringDepartment: Department

	final def recipients: Seq[User] = if (enabledForDepartment) allRecipients.filter(enabledForUser) else Seq()
	def allRecipients: Seq[User]
}

trait ActionRequiredNotification extends MyWarwickNotification {

	self: Notification[_, _] =>

	@transient var notificationService: NotificationService = Wire[NotificationService]

	@transient final protected val _completed = BooleanSetting("completed", false)
	protected def completed_=(isCompleted: Boolean) { _completed.value = isCompleted }
	def completed: Boolean = _completed.value

	@transient final protected val _completedBy = StringSetting("completedBy", "")
	protected def completedBy_=(userId: String) { _completedBy.value = userId }
	def completedBy: String = _completedBy.value

	@transient final protected val _completedOn = StringSetting("completedOn", "")
	protected def completedOn_=(dateTime: DateTime) { _completedOn.value = dateTime.getMillis.toString }
	def completedOn = new DateTime(_completedOn.value.toLong)

	def actionCompleted(user: User)

	def notificationItems: JList[EntityReference[_]] = items.asInstanceOf[JList[EntityReference[_]]]

	def isRecipient(user: User): Boolean = recipientNotificationInfos.asScala.exists(_.recipient == user)

}

trait AllCompletedActionRequiredNotification extends ActionRequiredNotification {

	self: Notification[_, _] =>

	override final def actionCompleted(user: User): Unit = transactional() {
		dismiss(user)
		completed = true
		completedBy = user.getUserId
		completedOn = DateTime.now
	}
}

trait RecipientCompletedActionRequiredNotification extends ActionRequiredNotification {

	self: Notification[_, _] =>

	override final def actionCompleted(user: User): Unit = transactional() {
		dismiss(user)
	}

	final def actionCompletedByOther(user: User): Unit = transactional() {
		completed = true
		completedBy = user.getUserId
		completedOn = DateTime.now
	}
}

/**
	* Controls whether a notification is sent to My Warwick as an activity or a notification
	*/
sealed trait MyWarwickDiscriminator {
	self: Notification[_, _] =>
}

trait MyWarwickNotification extends MyWarwickDiscriminator {
	self: Notification[_, _] =>
}

trait MyWarwickActivity extends MyWarwickDiscriminator {
	self: Notification[_, _] =>
}