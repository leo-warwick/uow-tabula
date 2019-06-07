package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.hibernate.annotations.Proxy
import org.joda.time.DateTime

object FileAttachmentToken {
  val DefaultTokenValidityMinutes = 30
}

@Entity
@Proxy(`lazy` = false)
class FileAttachmentToken extends GeneratedId {

  @Column(name = "fileattachment_id")
  var fileAttachmentId: String = _

  var expires: DateTime = _

  @Column(name = "date_used")
  var dateUsed: DateTime = _

}