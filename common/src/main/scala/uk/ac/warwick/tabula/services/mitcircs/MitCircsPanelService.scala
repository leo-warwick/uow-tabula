package uk.ac.warwick.tabula.services.mitcircs

import org.joda.time.LocalDate
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.commands.MemberOrUser
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesPanel
import uk.ac.warwick.tabula.data.{AutowiringMitCircsPanelDaoComponent, HibernateHelpers, MitCircsPanelDaoComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, SecurityServiceComponent}

trait MitCircsPanelService {
  def get(id: String): Option[MitigatingCircumstancesPanel]
  def saveOrUpdate(panel: MitigatingCircumstancesPanel): MitigatingCircumstancesPanel
  def list(department: Department, academicYear: AcademicYear): Seq[MitigatingCircumstancesPanel]
  def panels(user: CurrentUser): Set[MitigatingCircumstancesPanel]
  def getPanels(user: MemberOrUser): Set[MitigatingCircumstancesPanel]
  def getPanels(user: MemberOrUser, startInclusive: LocalDate, endInclusive: LocalDate): Set[MitigatingCircumstancesPanel]
}

abstract class AbstractMitCircsPanelService extends MitCircsPanelService {
  self: MitCircsPanelDaoComponent with PermissionsServiceComponent with SecurityServiceComponent =>

  override def get(id: String): Option[MitigatingCircumstancesPanel] = transactional(readOnly = true) {
    mitCircsPanelDao.get(id)
  }

  override def saveOrUpdate(panel: MitigatingCircumstancesPanel): MitigatingCircumstancesPanel = transactional() {
    mitCircsPanelDao.saveOrUpdate(panel)
  }

  override def list(department: Department, academicYear: AcademicYear): Seq[MitigatingCircumstancesPanel] = transactional(readOnly = true) {
    mitCircsPanelDao.list(department, academicYear)
  }

  // TODO - nuke this if we are happy with getPanels instead - caching means that this won't show new panels if the panel list was fetched recently (but changes to an existing panels usergroup do bust the cache)
  def panels(user: CurrentUser): Set[MitigatingCircumstancesPanel] = transactional(readOnly = true) {
    // TODO - something something type erasure in permissionsService.getGrantedRolesFor - if trying to fetch an MCOs panels you get - java.lang.ClassCastException: uk.ac.warwick.tabula.data.model.Department$HibernateProxy$cZdKomEW cannot be cast to uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesPanel
    permissionsService.getAllPermissionDefinitionsFor[MitigatingCircumstancesPanel](user, Permissions.MitigatingCircumstancesSubmission.Read)
      .collect { case p: MitigatingCircumstancesPanel => p }
      .filter(panel => securityService.can(user, Permissions.MitigatingCircumstancesSubmission.Read, panel))
      .map(HibernateHelpers.initialiseAndUnproxy)
  }

  def getPanels(user: MemberOrUser): Set[MitigatingCircumstancesPanel] = transactional(readOnly = true) {
    mitCircsPanelDao.getPanels(user)
      .map(HibernateHelpers.initialiseAndUnproxy) // :ytho:
  }

  def getPanels(user: MemberOrUser, startInclusive: LocalDate, endInclusive: LocalDate): Set[MitigatingCircumstancesPanel] = transactional(readOnly = true) {
    mitCircsPanelDao.getPanels(user, startInclusive, endInclusive)
      .map(HibernateHelpers.initialiseAndUnproxy)
  }
}

@Service("mitCircsPanelService")
class AutowiredMitCircsPanelService extends AbstractMitCircsPanelService
  with AutowiringMitCircsPanelDaoComponent
  with AutowiringPermissionsServiceComponent
  with AutowiringSecurityServiceComponent

trait MitCircsPanelServiceComponent {
  def mitCircsPanelService: MitCircsPanelService
}

trait AutowiringMitCircsPanelServiceComponent extends MitCircsPanelServiceComponent {
  var mitCircsPanelService: MitCircsPanelService = Wire[MitCircsPanelService]
}
