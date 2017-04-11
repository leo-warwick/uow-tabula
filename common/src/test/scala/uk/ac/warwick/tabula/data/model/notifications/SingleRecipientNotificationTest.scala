package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.{SmallGroupFixture, TestBase}
import uk.ac.warwick.tabula.data.model.SingleRecipientNotification
import uk.ac.warwick.userlookup.User

class SingleRecipientNotificationTest extends TestBase{


  def createNotification(user:User): SingleRecipientNotification ={
    new SingleRecipientNotification {
      val recipient: User = user
    }
  }
  @Test
  def recipientsContainsSingleUser():Unit  = new SmallGroupFixture{
    val n: SingleRecipientNotification = createNotification(recipient)
    n.recipients should be (Seq(recipient))
  }

}
