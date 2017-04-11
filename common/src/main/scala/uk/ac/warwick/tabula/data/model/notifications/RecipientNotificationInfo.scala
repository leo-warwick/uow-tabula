package uk.ac.warwick.tabula.data.model.notifications

import javax.persistence.{Column, Entity, JoinColumn, ManyToOne}

import uk.ac.warwick.tabula.data.model.{GeneratedId, Notification}
import org.hibernate.annotations.Type
import org.joda.time.DateTime
import uk.ac.warwick.userlookup.User

/**
 * Used to store properties against each recipient notification pair such as if an email has been sent to
 * the recipient or if the recipient has chosen to dismiss the notification from their streams.
 */

@Entity
class RecipientNotificationInfo extends GeneratedId {

	def this(notification: Notification[_,_], recipient: User) {
		this()
		this.notification = notification
		this.recipient = recipient
	}

	@ManyToOne
	@JoinColumn(name = "notification_id")
	var notification: Notification[_, _] = null

	@Column(nullable=false)
	@Type(`type`="uk.ac.warwick.tabula.data.model.SSOUserType")
	var recipient: User = null

	@Column(nullable=false)
	var dismissed: Boolean = false

	@Column(name="email_sent", nullable=false)
	var emailSent: Boolean = false

	var attemptedAt: DateTime = null

}