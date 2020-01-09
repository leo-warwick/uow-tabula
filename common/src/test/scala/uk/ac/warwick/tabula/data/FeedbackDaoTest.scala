package uk.ac.warwick.tabula.data

import org.hibernate.Session
import uk.ac.warwick.tabula.{Fixtures, PersistenceTestBase}

class FeedbackDaoTest extends PersistenceTestBase {

  val dao = new AbstractFeedbackDao with ExtendedSessionComponent {
    override def session: Session = FeedbackDaoTest.this.session
  }

  @Test def crud(): Unit = transactional { tx =>
    // TAB-2415
    session.enableFilter("notDeleted")

    val f1 = Fixtures.assignmentFeedback("0205225")
    val f2 = Fixtures.assignmentFeedback("0205225")
    val f3 = Fixtures.assignmentFeedback("0205226")

    val ass1 = Fixtures.assignment("ass1")
    val ass2 = Fixtures.assignment("ass2")

    session.save(ass1)
    session.save(ass2)
    session.flush()

    ass1.feedbacks.add(f1)
    f1.assignment = ass1
    ass1.feedbacks.add(f3)
    f3.assignment = ass1
    ass2.feedbacks.add(f2)
    f2.assignment = ass2

    session.saveOrUpdate(ass1)
    session.saveOrUpdate(ass2)
    session.saveOrUpdate(f1)
    session.saveOrUpdate(f2)
    session.saveOrUpdate(f3)
    session.flush()

    val mf1 = Fixtures.markerFeedback(f1)
    val mf2 = Fixtures.markerFeedback(f2)

    session.saveOrUpdate(f1)
    session.saveOrUpdate(mf1)
    session.saveOrUpdate(f2)
    session.saveOrUpdate(mf2)
    session.flush()

    val mf3 = Fixtures.markerFeedback(f3)
    session.saveOrUpdate(f3)
    session.saveOrUpdate(mf3)
    session.flush()
    session.clear()

    dao.getFeedback(f1.id) should be(Some(f1))
    dao.getFeedback(f2.id) should be(Some(f2))
    dao.getFeedback(f3.id) should be(Some(f3))
    dao.getFeedback("blah") should be(None)

    dao.getMarkerFeedback(mf1.id).map(_.feedback) should be(Some(f1))
    dao.getMarkerFeedback(mf2.id).map(_.feedback) should be(Some(f2))
    dao.getMarkerFeedback(mf3.id).map(_.feedback) should be(Some(f3))
    dao.getMarkerFeedback("blah") should be(None)

    dao.getFeedbackByUsercode(ass1, "0205225") should be(Some(f1))
    dao.getFeedbackByUsercode(ass2, "0205225") should be(Some(f2))
    dao.getFeedbackByUsercode(ass1, "0205226") should be(Some(f3))
    dao.getFeedbackByUsercode(ass2, "0205226") should be(None)
    session.flush()

    dao.delete(dao.getFeedback(f1.id).get)
    session.flush()

    dao.getFeedbackByUsercode(ass1, "0205225") should be(None)
    dao.getFeedback(f1.id) should be(None)
    dao.getMarkerFeedback(mf1.id) should be(None)
  }

}