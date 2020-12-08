package uk.ac.warwick.tabula.services.marks

import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{RecordedResit, UpstreamAssessmentGroup}
import uk.ac.warwick.tabula.data.{AutowiringResitDaoComponent, AutowiringTransactionalComponent, ResitDaoComponent, TransactionalComponent}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.services.healthchecks.SitsQueueService

trait ResitService extends SitsQueueService {
  def saveOrUpdate(resit: RecordedResit): RecordedResit
  def getAllResits(uag: UpstreamAssessmentGroup): Seq[RecordedResit]
  def getAllResitsByGroup(groups: Seq[UpstreamAssessmentGroup]): Map[UpstreamAssessmentGroup, Seq[RecordedResit]]
  def findResits(sprCodes: Seq[String]): Seq[RecordedResit]
  def allNeedingWritingToSits: Seq[RecordedResit]
  def mostRecentlyWrittenToSitsDate: Option[DateTime]
}

class AbstractResitService extends ResitService {
  self: ResitDaoComponent with ModuleAndDepartmentServiceComponent with TransactionalComponent =>

  override def saveOrUpdate(resit: RecordedResit): RecordedResit = transactional() {
    resitDao.saveOrUpdate(resit)
  }

  override def getAllResits(uag: UpstreamAssessmentGroup): Seq[RecordedResit] = transactional(readOnly = true) {
    resitDao.getAllResits(uag)
  }

  override def getAllResitsByGroup(groups: Seq[UpstreamAssessmentGroup]): Map[UpstreamAssessmentGroup, Seq[RecordedResit]] = transactional(readOnly = true) {
    if (groups.isEmpty) Map.empty
    else {
      require(groups.forall(_.academicYear == groups.head.academicYear), "UpstreamAssessmentGroups must all be for the same academic year")

      val academicYear = groups.head.academicYear
      val moduleCodes = groups.map(_.moduleCode).distinct

      val allResits: Map[(String, AcademicYear, String, String), Seq[RecordedResit]] =
        resitDao.getAllForModulesInYear(moduleCodes, academicYear)
          .groupBy(racs => (racs.moduleCode, racs.academicYear, racs.occurrence, racs.sequence))

      groups.map { uag =>
        uag -> allResits.getOrElse((uag.moduleCode, uag.academicYear, uag.occurrence, uag.sequence), Seq.empty)
      }.toMap
    }
  }

  override def findResits(sprCodes: Seq[String]): Seq[RecordedResit] = transactional(readOnly = true) {
    resitDao.findResits(sprCodes)
  }

  override def allNeedingWritingToSits: Seq[RecordedResit] = transactional(readOnly = true) {
    resitDao.allNeedingWritingToSits.filter { resit =>
        moduleAndDepartmentService.getModuleBySitsCode(resit.moduleCode).forall { module =>
          module.adminDepartment.canUploadMarksToSitsForYear(resit.academicYear, module)
        }
    }
  }

  override def mostRecentlyWrittenToSitsDate: Option[DateTime] = transactional(readOnly = true) {
    resitDao.mostRecentlyWrittenToSitsDate
  }
}


@Service("resitService")
class AutowiringResitService
  extends AbstractResitService
    with AutowiringResitDaoComponent
    with AutowiringModuleAndDepartmentServiceComponent
    with AutowiringTransactionalComponent


trait ResitServiceComponent {
  def resitService: ResitService
}

trait AutowiringResitServiceComponent extends ResitServiceComponent {
  var resitService: ResitService = Wire[ResitService]
}
