package uk.ac.warwick.tabula.services.marks

import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.RecordedDecision
import uk.ac.warwick.tabula.data.{AutowiringDecisionDaoComponent, AutowiringTransactionalComponent, DecisionDaoComponent, TransactionalComponent}
import uk.ac.warwick.tabula.services.healthchecks.SitsQueueService

trait DecisionService extends SitsQueueService {
  def saveOrUpdate(decision: RecordedDecision): RecordedDecision
  def findDecisions(sprCodes: Seq[String]): Seq[RecordedDecision]
  def allNeedingWritingToSits: Seq[RecordedDecision]
  def mostRecentlyWrittenToSitsDate: Option[DateTime]
}

class AbstractDecisionService extends DecisionService {
  self: DecisionDaoComponent with TransactionalComponent =>

  override def saveOrUpdate(decision: RecordedDecision): RecordedDecision = transactional() {
    decisionDao.saveOrUpdate(decision)
  }

  override def findDecisions(sprCodes: Seq[String]): Seq[RecordedDecision] = transactional(readOnly = true) {
    decisionDao.findDecisions(sprCodes)
  }

  override def allNeedingWritingToSits: Seq[RecordedDecision] = transactional(readOnly = true) {
    decisionDao.allNeedingWritingToSits
  }

  override def mostRecentlyWrittenToSitsDate: Option[DateTime] = transactional(readOnly = true) {
    decisionDao.mostRecentlyWrittenToSitsDate
  }
}


@Service("decisionService")
class AutowiringDecisionService
  extends AbstractDecisionService
    with AutowiringDecisionDaoComponent
    with AutowiringTransactionalComponent


trait DecisionServiceComponent {
  def decisionService: DecisionService
}

trait AutowiringDecisionServiceComponent extends DecisionServiceComponent {
  var decisionService: DecisionService = Wire[DecisionService]
}
