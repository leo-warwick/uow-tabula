package uk.ac.warwick.tabula.data.model.notifications

import javax.persistence.{DiscriminatorValue, Entity}
import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, ProfileService}
import uk.ac.warwick.userlookup.User

@Entity
@DiscriminatorValue("UAMAuditChaserNotification")
class UAMAuditChaserNotification extends UAMAuditNotification {
	override val templateLocation = "/WEB-INF/freemarker/emails/uam_audit_chaser_email.ftl"
}
