package uk.ac.warwick.tabula.data.model

import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.services.FeedbackService
import uk.ac.warwick.tabula.{Fixtures, Mockito, PersistenceTestBase}

import scala.collection.JavaConverters._
import scala.util.Random

// scalastyle:off magic.number

class MarkerFeedbackTest extends PersistenceTestBase with Mockito {

  @Test def fields() {
    val random = new Random
    val feedback = Fixtures.assignmentFeedback(universityId = idFormat(1))

    val firstMarkerFeedback = new MarkerFeedback(feedback)
    val mark1 = random.nextInt(101)
    firstMarkerFeedback.mark = Some(mark1)
    firstMarkerFeedback.mark.get should be(mark1)


    val secondMarkerFeedback = new MarkerFeedback(feedback)
    val mark2 = random.nextInt(101)
    secondMarkerFeedback.mark = Some(mark2)
    secondMarkerFeedback.mark.get should be(mark2)

    feedback.firstMarkerFeedback = firstMarkerFeedback
    feedback.secondMarkerFeedback = secondMarkerFeedback

    firstMarkerFeedback.feedback.id should be(feedback.id)
    secondMarkerFeedback.id should be(feedback.id)
    feedback.firstMarkerFeedback.id should be(firstMarkerFeedback.id)
    feedback.secondMarkerFeedback.id should be(secondMarkerFeedback.id)

  }

  @Transactional
  @Test def deleteFileAttachmentOnDelete() {
    // TAB-667
    val orphanAttachment = flushing(session) {
      val attachment = new FileAttachment
      session.save(attachment)
      attachment
    }

    val feedback = flushing(session) {
      val feedback = Fixtures.assignmentFeedback(universityId = idFormat(1))
      val assignment = Fixtures.assignment("Cool essay")
      feedback.assignment = assignment
      session.save(assignment)
      session.save(feedback)
      feedback
    }

    val (markerFeedback, markerFeedbackAttachment) = flushing(session) {
      val mf = Fixtures.markerFeedback(feedback)

      val attachment = new FileAttachment
      mf.addAttachment(attachment)

      session.save(feedback)
      (mf, attachment)
    }

    // Ensure everything's been persisted
    orphanAttachment.id should not be null
    feedback.id should not be null
    markerFeedback.id should not be null
    markerFeedbackAttachment.id should not be null

    // Can fetch everything from db
    session.get(classOf[FileAttachment], orphanAttachment.id) should be(orphanAttachment)
    session.get(classOf[Feedback], feedback.id) should be(feedback)
    session.get(classOf[MarkerFeedback], markerFeedback.id) should be(markerFeedback)
    session.get(classOf[FileAttachment], markerFeedbackAttachment.id) should be(markerFeedbackAttachment)

    session.clear()

    flushing(session) {
      val feedback2 = session.load(classOf[AssignmentFeedback], feedback.id)
      val mf2 = session.load(classOf[MarkerFeedback], markerFeedback.id)

      feedback2.markerFeedback.remove(mf2) should be(true)
      session.delete(mf2)
    }

    session.clear()

    // Ensure we can't fetch the markerFeedback or attachment, but all the other objects are returned
    session.get(classOf[FileAttachment], orphanAttachment.id) should be(orphanAttachment)
    session.get(classOf[Feedback], feedback.id) should be(feedback)
    session.get(classOf[MarkerFeedback], markerFeedback.id) should be(null)
    session.get(classOf[FileAttachment], markerFeedbackAttachment.id) should be(null)

  }


  /** Zero-pad integer to a 7 digit string */
  def idFormat(i: Int): String = "%07d" format i
}
