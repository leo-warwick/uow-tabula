package uk.ac.warwick.tabula.data.model.forms
import scala.collection.JavaConverters._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.PersistenceTestBase
import uk.ac.warwick.tabula.data.model.{Submission, Assignment, FileAttachment}
import uk.ac.warwick.userlookup.User

// scalastyle:off magic.number

class ExtensionTest extends PersistenceTestBase {

  @Test def testExtension() {

    val assignment = new Assignment
		assignment.setDefaultBooleanProperties()
    assignment.closeDate = new DateTime(2012, 7, 12, 12, 0)
		assignment.openEnded = false

		withFakeTime(new DateTime(2012, 7, 22, 14, 42)) {
			val extension = new Extension()
			extension._universityId = "1170836"
			extension.usercode = "cuslaj"
			extension.expiryDate = new DateTime(2012, 8, 12, 12, 0)
			extension.reason = "My hands have turned to flippers. Like the ones that dolphins have. It makes writing and typing super hard. Pity me."
			extension.approve("That sounds awful. Have an extra month. By then you should be able to write as well as any Cetacea.")

			assignment.addExtension(extension)
		}

    val cuslaj = new User("cuslaj")
    cuslaj.setWarwickId("1170836")

    val cuscao = new User("cuscao")
    cuscao.setWarwickId("1122334")

    withFakeTime(dateTime(2012, 8)) {
      assignment.isWithinExtension(cuslaj) should be (true)  // has an extension so can submit
      assignment.isWithinExtension(cuscao) should be (false) // cannot submit
    }

    withFakeTime(dateTime(2012, 7)) {
      for (i <- 1 to 10) {
        val newSubmission = new Submission
				newSubmission.usercode = idFormat(i)
				newSubmission._universityId = idFormat(i)
        newSubmission.submittedDate = new DateTime
        assignment.submissions add newSubmission
      }
    }

    withFakeTime(dateTime(2012, 8)) {
      for (i <- 11 to 15) {
        val newSubmission = new Submission
				newSubmission.usercode = idFormat(i)
				newSubmission._universityId = idFormat(i)
        newSubmission.submittedDate = new DateTime
        assignment.submissions add newSubmission
      }
      val newSubmission = new Submission
			newSubmission._universityId = "1170836"
      newSubmission.usercode = "cuslaj"
      newSubmission.submittedDate = new DateTime
      assignment.submissions add newSubmission
    }

    val lateSubmissions = assignment.submissions.asScala.filter(assignment.isLate).map(_._universityId)
    lateSubmissions should be ((11 to 15) map idFormat)

  }

	@Test def deleteFileAttachmentOnDelete(): Unit = transactional{tx=>
		// TAB-667
		val orphanAttachment = flushing(session) {
			val attachment = new FileAttachment

			session.save(attachment)
			attachment
		}

		val (extension, extensionAttachment) = flushing(session) {
			val extension = new Extension
			extension._universityId = idFormat(1)
			extension.usercode = "steve"

			val assignment = new Assignment
			session.save(assignment)

			extension.assignment = assignment

			val attachment = new FileAttachment
			extension.addAttachment(attachment)

			session.save(extension)
			(extension, attachment)
		}

		// Ensure everything's been persisted
		orphanAttachment.id should not be null
		extension.id should not be null
		extensionAttachment.id should not be null

		// Can fetch everything from db
		flushing(session) {
			session.get(classOf[FileAttachment], orphanAttachment.id) should be (orphanAttachment)
			session.get(classOf[Extension], extension.id) should be (extension)
			session.get(classOf[FileAttachment], extensionAttachment.id) should be (extensionAttachment)
		}

		flushing(session) { session.delete(extension) }

		// Ensure we can't fetch the extension or attachment, but all the other objects are returned
		flushing(session) {
			session.get(classOf[FileAttachment], orphanAttachment.id) should be (orphanAttachment)
			session.get(classOf[Extension], extension.id) should be (null)
			session.get(classOf[FileAttachment], extensionAttachment.id) should be (null)
		}
	}

  /** Zero-pad integer to a 7 digit string */
  def idFormat(i:Int): String = "%07d" format i

  @Test def flags() {
	  val extension = new Extension

	  extension.isManual should be (true)
	  extension.awaitingReview should be (false)

	  extension.requestedOn = DateTime.now

	  extension.isManual should be (false)
	  extension.awaitingReview should be (true)

	  extension.approve()

	  extension.awaitingReview should be (false)
  }

	@Test def expiryDateAdjustment(): Unit = {
		val assignment = new Assignment
		assignment.setDefaultBooleanProperties()
		assignment.closeDate = DateTime.now().plusHours(1)

		val extension = new Extension
		extension.assignment = assignment

		extension.requestedExpiryDate = DateTime.now().plusHours(3)

		extension.expiryDate = DateTime.now().plusHours(3)
		extension.approve(null)

		extension should not be 'expiryDateAdjusted

		extension.expiryDate = DateTime.now().plusHours(2)

		extension shouldBe 'expiryDateAdjusted
	}
}
