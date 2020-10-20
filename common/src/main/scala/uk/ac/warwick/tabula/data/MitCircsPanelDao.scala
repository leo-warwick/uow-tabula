package uk.ac.warwick.tabula.data

import org.hibernate.criterion.Order._
import org.joda.time.LocalDate
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{MemberOrUser, TaskBenchmarking}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesPanel
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

trait MitCircsPanelDaoComponent {
  val mitCircsPanelDao: MitCircsPanelDao
}

trait AutowiringMitCircsPanelDaoComponent extends MitCircsPanelDaoComponent {
  val mitCircsPanelDao: MitCircsPanelDao = Wire[MitCircsPanelDao]
}

trait MitCircsPanelDao {
  def get(id: String): Option[MitigatingCircumstancesPanel]
  def saveOrUpdate(submission: MitigatingCircumstancesPanel): MitigatingCircumstancesPanel
  def list(department: Department, academicYear: AcademicYear): Seq[MitigatingCircumstancesPanel]
  def getPanels(user: MemberOrUser): Set[MitigatingCircumstancesPanel]
  def getPanels(user: MemberOrUser, startInclusive: LocalDate, endInclusive: LocalDate): Set[MitigatingCircumstancesPanel]
}

@Repository
class MitCircsPanelDaoImpl extends MitCircsPanelDao
  with Daoisms with TaskBenchmarking with AutowiringUserLookupComponent {

  override def get(id: String): Option[MitigatingCircumstancesPanel] = getById[MitigatingCircumstancesPanel](id)

  override def saveOrUpdate(panel: MitigatingCircumstancesPanel): MitigatingCircumstancesPanel = {
    session.saveOrUpdate(panel)
    panel
  }

  override def list(department: Department, academicYear: AcademicYear): Seq[MitigatingCircumstancesPanel] =
    session.newCriteria[MitigatingCircumstancesPanel]
      .add(is("department", department))
      .add(is("academicYear", academicYear))
      .addOrder(asc("_date"))
      .addOrder(asc("_endDate"))
      .addOrder(asc("lastModified"))
      .seq

  override def getPanels(user: MemberOrUser): Set[MitigatingCircumstancesPanel] = {
    // TODO - would be more efficient to return panels from the query directly but I have no idea what the spell for joins on @Any relationships looks like
    // select distinct p from MitigatingCircumstancesPanel p join GrantedRole r on r.scope = p // <- blows up
    session.newQuery[GrantedRole[MitigatingCircumstancesPanel]](s"""
      select distinct r from GrantedRole r
        inner join r._users ug
        left join ug.staticIncludeUsers static on static = :userId
        left join ug.includeUsers include on include = :userId
        left join ug.excludeUsers exclude on exclude = :userId
      where
        r.scopeType = 'MitigatingCircumstancesPanel' and (static is not null or include is not null) and exclude is null
      """)
      .setString("userId", user.usercode)
      .seq.map(_.scope).toSet
  }

  override def getPanels(user: MemberOrUser, startInclusive: LocalDate, endInclusive: LocalDate): Set[MitigatingCircumstancesPanel] = {
    session.newQuery[GrantedRole[MitigatingCircumstancesPanel]](s"""
      select distinct r from GrantedRole r
        inner join r._users ug
        left join ug.staticIncludeUsers static on static = :userId
        left join ug.includeUsers include on include = :userId
        left join ug.excludeUsers exclude on exclude = :userId
      where
        r.scopeType = 'MitigatingCircumstancesPanel' and (static is not null or include is not null) and exclude is null
      """)
      .setString("userId", user.usercode)
      .seq.map(_.scope)
      .filterNot(panel => (Option(panel.date).isEmpty && Option(panel.endDate).isEmpty
        && panel.date.get.toLocalDate.isBefore(startInclusive) && panel.endDate.get.toLocalDate.isAfter(endInclusive)))
      .toSet

  }

}

