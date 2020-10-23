package uk.ac.warwick.tabula.data

import org.hibernate.criterion.Order
import org.hibernate.criterion.Projections.max
import org.hibernate.criterion.Restrictions.isNotNull
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Daoisms._
import uk.ac.warwick.tabula.data.model.RecordedDecision

trait DecisionDao {
  def saveOrUpdate(resit: RecordedDecision): RecordedDecision
  def findDecisions(sprCodes: Seq[String]): Seq[RecordedDecision]
  def allNeedingWritingToSits: Seq[RecordedDecision]
  def mostRecentlyWrittenToSitsDate: Option[DateTime]
}

abstract class AbstractDecisionDao extends DecisionDao {
  self: ExtendedSessionComponent
    with HelperRestrictions =>

  override def saveOrUpdate(resit: RecordedDecision): RecordedDecision = {
    session.saveOrUpdate(resit)
    resit
  }

  override def findDecisions(sprCodes: Seq[String]): Seq[RecordedDecision] =
    session.newCriteria[RecordedDecision]
      .add(safeIn("sprCode", sprCodes))
      .seq

  override def allNeedingWritingToSits: Seq[RecordedDecision] =
    session.newCriteria[RecordedDecision]
      .add(isNotNull("_needsWritingToSitsSince"))
      .addOrder(Order.asc("updatedDate"))
      .seq

  override def mostRecentlyWrittenToSitsDate: Option[DateTime] =
    session.newCriteria[RecordedDecision]
      .add(isNotNull("_lastWrittenToSits"))
      .project[DateTime](max("_lastWrittenToSits"))
      .uniqueResult
}

@Repository
class AutowiringDecisionDao
  extends AbstractDecisionDao
    with Daoisms

trait DecisionDaoComponent {
  def decisionDao: DecisionDao
}

trait AutowiringDecisionDaoComponent extends DecisionDaoComponent {
  var decisionDao: DecisionDao = Wire[DecisionDao]
}
