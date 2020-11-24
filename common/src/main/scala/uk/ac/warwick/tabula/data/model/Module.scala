package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.hibernate.annotations.{BatchSize, Proxy, Type}
import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesAffectedAssessment
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.{ModuleAssistantRoleDefinition, ModuleManagerRoleDefinition}
import uk.ac.warwick.tabula.services.permissions.PermissionsService

import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.util.matching.Regex

@Entity
@Proxy
@NamedQueries(Array(
  new NamedQuery(name = "module.code", query = "select m from Module m where code = :code"),
  new NamedQuery(name = "module.adminDepartment", query = "select m from Module m where adminDepartment = :adminDepartment")))
class Module extends GeneratedId with PermissionsTarget with Serializable {

  def this(code: String = null, adminDepartment: Department = null) = {
    this()
    this.code = code
    this.adminDepartment = adminDepartment
  }

  var code: String = _
  var name: String = _
  var shortName: String = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.DegreeTypeUserType")
  var degreeType: DegreeType = _ // ug or pg

  // The managers are markers/moderators who upload feedback.
  // They can also publish feedback.
  // Module assistants can't publish feedback
  @transient
  var permissionsService: PermissionsService = Wire.auto[PermissionsService]
  @transient
  lazy val managers: UnspecifiedTypeUserGroup = permissionsService.ensureUserGroupFor(this, ModuleManagerRoleDefinition)
  @transient
  lazy val assistants: UnspecifiedTypeUserGroup = permissionsService.ensureUserGroupFor(this, ModuleAssistantRoleDefinition)

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id")
  var adminDepartment: Department = _

  @deprecated("TAB-2589 to be explicit, this should use adminDepartment or teachingDepartments", "84")
  def department: Department = adminDepartment

  @deprecated("TAB-2589 to be explicit, this should use adminDepartment or teachingDepartments", "84")
  def department_=(d: Department): Unit = {
    adminDepartment = d
  }

  @OneToMany(mappedBy = "module", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @BatchSize(size = 200)
  var teachingInfo: JSet[ModuleTeachingInformation] = JHashSet()

  def teachingDepartments: mutable.Set[Department] = teachingInfo.asScala.map(_.department) + adminDepartment

  def permissionsParents: LazyList[Department] = Option(adminDepartment).to(LazyList)

  override def humanReadableId: String = code.toUpperCase + " " + name

  override def urlSlug: String = code

  @OneToMany(mappedBy = "module", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
  @BatchSize(size = 100)
  @OrderBy("closeDate")
  var assignments: JList[Assignment] = JArrayList()

  def hasLiveAssignments: Boolean = Option(assignments) match {
    case Some(a) => a.asScala.exists(_.isAlive)
    case None => false
  }

  @OneToMany(mappedBy = "module", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
  @BatchSize(size = 200)
  var groupSets: JList[SmallGroupSet] = JArrayList()

  var active: Boolean = _

  var missingFromImportSince: DateTime = _

  override def toString: String = "Module[" + code + "]"


  // true if at least one of this module's SmallGroupSets has not been released to //both students and staff.
  def hasUnreleasedGroupSets(academicYear: AcademicYear): Boolean = {
    val allGroupSets = groupSets.asScala.filter(_.academicYear == academicYear)
    allGroupSets.exists(!_.fullyReleased)
  }

  var sitsDepartmentCode: String = _

  // added to allow us to join in MitCircsSubmissionDao.affectedModulesForDepartment to get the set of modules that are impacted by mitcircs claims
  // this _must_ remain lazy
  @OneToMany(mappedBy = "_module", fetch = FetchType.LAZY)
  private var _affectedAssessments: JList[MitigatingCircumstancesAffectedAssessment] = JArrayList()
}

object Module extends Logging {

  private val ModuleCodePatternString = "(?i)([a-z]{2,3}[a-z0-9][a-z0-9.][a-z0-9])"
  // <modulecode> "-" <cats>
  // where cats can be a decimal number.
  private val ModuleCatsPattern = new Regex(ModuleCodePatternString + """-(\d+(?:\.\d+)?)""")

  private val ModuleCodeOnlyPattern = new Regex(ModuleCodePatternString)

  private val WebgroupPattern = new Regex("""(.+?)-(.+)""")

  def nameFromWebgroupName(groupName: String): String = groupName match {
    case WebgroupPattern(dept, name) => name
    case _ => groupName
  }

  def stripCats(fullModuleName: String): Option[String] = {
    Option(fullModuleName).flatMap(name => name.replaceAll("\\s+", "") match {
      case ModuleCatsPattern(module, _) => Option(module)
      case ModuleCodeOnlyPattern(module) => Option(module)
      case _ =>
        logger.warn(s"Module name $fullModuleName did not fit expected module code pattern")
        None
    })
  }

  def extractCats(fullModuleName: String): Option[String] = fullModuleName.replaceAll("\\s+", "") match {
    case ModuleCatsPattern(_, cats) => Some(cats)
    case ModuleCodeOnlyPattern(_) => None
    case _ => None
  }

  // For sorting a collection by module code. Either pass to the sort function,
  // or expose as an implicit val.
  val CodeOrdering: Ordering[Module] = Ordering.by[Module, String](_.code)
  val NameOrdering: Ordering[Module] = Ordering.by { module: Module => (module.name, module.code) }

  // Companion object is one of the places searched for an implicit Ordering, so
  // this will be the default when ordering a list of modules.
  implicit val defaultOrdering = CodeOrdering

}
