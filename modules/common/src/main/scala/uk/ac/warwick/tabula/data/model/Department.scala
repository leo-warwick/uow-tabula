package uk.ac.warwick.tabula.data.model

import javax.persistence._

import org.hibernate.annotations.{BatchSize, Type}
import org.hibernate.criterion.Restrictions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.convert.ConvertibleConverter
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupAllocationMethod, WeekRange}
import uk.ac.warwick.tabula.data.model.permissions.{CustomRoleDefinition, DepartmentGrantedRole}
import uk.ac.warwick.tabula.data.{AliasAndJoinType, PostLoadBehaviour, ScalaRestriction}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.{DepartmentalAdministratorRoleDefinition, ExtensionManagerRoleDefinition, RoleDefinition, SelectorBuiltInRoleDefinition}
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.services.permissions.PermissionsService

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.xml.NodeSeq

@Entity @Access(AccessType.FIELD)
class Department extends GeneratedId
	with PostLoadBehaviour with HasSettings with HasNotificationSettings with PermissionsTarget with Serializable with ToEntityReference with Logging{

	import Department._

	type Entity = Department

	@Column(unique = true)
	var code: String = _

	@Column(name = "name")
	var fullName: String = _

	def name: String = shortName.maybeText.getOrElse(fullName)

	var shortName: String = _

	@OneToMany(mappedBy="parent", fetch = FetchType.LAZY)
	@BatchSize(size=200)
	var children:JSet[Department] = JHashSet()

	// returns a list containing this department and all departments that are descendants of this department
	def descendants: List[Department] = {
		def getDescendantsDepts(department: Department): List[Department] = {
			if (department.children.asScala.isEmpty) List(department)
			else department :: department.children.asScala.toList.flatMap(d => getDescendantsDepts(d))
		}
		getDescendantsDepts(this)
	}

	@ManyToOne(fetch = FetchType.LAZY, optional=true)
	var parent: Department = _

	// No orphanRemoval as it makes it difficult to move modules between Departments.
	@OneToMany(mappedBy="adminDepartment", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = false)
	@BatchSize(size=200)
	var modules: JList[Module] = JArrayList()

	@OneToMany(mappedBy="adminDepartment", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = false)
	@BatchSize(size=200)
	var routes:JList[Route] = JArrayList()

	@OneToMany(mappedBy = "department", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BatchSize(size=200)
	var moduleTeachingInfo: JSet[ModuleTeachingInformation] = JHashSet()

	@OneToMany(mappedBy = "department", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BatchSize(size=200)
	var routeTeachingInfo: JSet[RouteTeachingInformation] = JHashSet()

	@OneToMany(mappedBy = "department", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BatchSize(size=200)
	var feedbackTemplates: JList[FeedbackTemplate] = JArrayList()

	@OneToMany(mappedBy = "department", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BatchSize(size=200)
	private val _markingWorkflows: JSet[MarkingWorkflow] = JHashSet()
	def markingWorkflows: Seq[MarkingWorkflow] = _markingWorkflows.asScala.toSeq.sorted
	def addMarkingWorkflow(markingWorkflow: MarkingWorkflow): Boolean = _markingWorkflows.add(markingWorkflow)
	def removeMarkingWorkflow(markingWorkflow: MarkingWorkflow): Boolean = _markingWorkflows.remove(markingWorkflow)

	// TAB-2388 Disable orphanRemoval as Module Managers were unintentionally being removed in certain circumstances
	@OneToMany(mappedBy="department", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = false)
	@BatchSize(size=200)
	var customRoleDefinitions: JList[CustomRoleDefinition] = JArrayList()

	def collectFeedbackRatings: Boolean = getBooleanSetting(Settings.CollectFeedbackRatings) getOrElse false
	def collectFeedbackRatings_= (collect: Boolean): Unit = settings += (Settings.CollectFeedbackRatings -> collect)

	// settings for extension requests
	def allowExtensionRequests: Boolean = getBooleanSetting(Settings.AllowExtensionRequests) getOrElse false
	def allowExtensionRequests_= (allow: Boolean): Unit = settings += (Settings.AllowExtensionRequests -> allow)

	def extensionGuidelineSummary: String = getStringSetting(Settings.ExtensionGuidelineSummary).orNull
	def extensionGuidelineSummary_= (summary: String): Unit = settings += (Settings.ExtensionGuidelineSummary -> summary)

	def extensionGuidelineLink: String = getStringSetting(Settings.ExtensionGuidelineLink).orNull
	def extensionGuidelineLink_= (link: String): Unit = settings += (Settings.ExtensionGuidelineLink -> link)

	def showStudentName: Boolean = getBooleanSetting(Settings.ShowStudentName) getOrElse false
	def showStudentName_= (showName: Boolean): Unit = settings += (Settings.ShowStudentName -> showName)

	def plagiarismDetectionEnabled: Boolean = getBooleanSetting(Settings.PlagiarismDetection, default = true)
	def plagiarismDetectionEnabled_= (enabled: Boolean): Unit = settings += (Settings.PlagiarismDetection -> enabled)

	def turnitinExcludeBibliography: Boolean = getBooleanSetting(Settings.TurnitinExcludeBibliography, default = true)
	def turnitinExcludeBibliography_= (exclude: Boolean): Unit = settings += (Settings.TurnitinExcludeBibliography -> exclude)

	def turnitinExcludeQuotations: Boolean = getBooleanSetting(Settings.TurnitinExcludeQuotations, default = true)
	def turnitinExcludeQuotations_= (exclude: Boolean): Unit = settings += (Settings.TurnitinExcludeQuotations -> exclude)

	def turnitinSmallMatchWordLimit: Int = getIntSetting(Settings.TurnitinSmallMatchWordLimit, 0)
	def turnitinSmallMatchWordLimit_= (limit: Int): Unit = settings += (Settings.TurnitinSmallMatchWordLimit -> limit)

	def turnitinSmallMatchPercentageLimit: Int = getIntSetting(Settings.TurnitinSmallMatchPercentageLimit, 0)
	def turnitinSmallMatchPercentageLimit_= (limit: Int): Unit = settings += (Settings.TurnitinSmallMatchPercentageLimit -> limit)

	def assignmentInfoView: String = getStringSetting(Settings.AssignmentInfoView) getOrElse Assignment.Settings.InfoViewType.Default
	def assignmentInfoView_= (setting: String): Unit = settings += (Settings.AssignmentInfoView -> setting)

	def autoGroupDeregistration: Boolean = getBooleanSetting(Settings.AutoGroupDeregistration, default = true)
	def autoGroupDeregistration_=(dereg: Boolean) { settings += (Settings.AutoGroupDeregistration -> dereg) }

	def studentsCanScheduleMeetings: Boolean = getBooleanSetting(Settings.StudentsCanScheduleMeetings, default = true)
	def studentsCanScheduleMeetings_=(canDo: Boolean) { settings += (Settings.StudentsCanScheduleMeetings -> canDo) }

	def uploadCourseworkMarksToSits: Boolean = getBooleanSetting(Settings.UploadCourseworkMarksToSits, default = false)
	def uploadCourseworkMarksToSits_=(enabled: Boolean) { settings += (Settings.UploadCourseworkMarksToSits -> enabled) }

	def uploadExamMarksToSits: Boolean = getBooleanSetting(Settings.UploadExamMarksToSits, default = false)
	def uploadExamMarksToSits_=(enabled: Boolean) { settings += (Settings.UploadExamMarksToSits -> enabled) }

	def canUploadMarksToSitsForYear(year: AcademicYear, module: Module): Boolean = {
		if (module.degreeType != DegreeType.Undergraduate && module.degreeType != DegreeType.Postgraduate) {
			logger.warn(s"Can't upload marks for module $module since degreeType ${module.degreeType} can't be identified as UG or PG")
			return false
		}
		canUploadMarksToSitsForYear(year, module.degreeType)
	}

	def canUploadMarksToSitsForYear(year: AcademicYear, degreeType: DegreeType): Boolean = {
		val markUploadMap: Option[Map[String, String]] = degreeType match {
			case DegreeType.Undergraduate => getStringMapSetting(Settings.CanUploadMarksToSitsForYearUg)
			case DegreeType.Postgraduate => getStringMapSetting(Settings.CanUploadMarksToSitsForYearPg)
			case _ =>
				logger.warn(s"Can't upload marks for degree type $degreeType since it can't be identified as UG or PG")
				return false
		}
		// marks are uploadable until a department is explicitly closed for the year by the Exams Office
		markUploadMap match {
			case None => true // there isn't even a settings map at all for this so hasn't been closed yet for the year
			case Some(markMap: Map[String, String]) =>
				markMap.get(year.toString) match {
					case None => true // no setting for this year/ugpg combo for the department - so hasn't been closed yet for the year
					case Some(booleanStringValue: String) => booleanStringValue.toBoolean
				}
		}
	}

	def setUploadMarksToSitsForYear(year: AcademicYear, degreeType: DegreeType, canUpload: Boolean): Unit = {
		val (markUploadMap, mapKey) = degreeType match {
			case DegreeType.Undergraduate => (getStringMapSetting(Settings.CanUploadMarksToSitsForYearUg), Settings.CanUploadMarksToSitsForYearUg)
			case DegreeType.Postgraduate => (getStringMapSetting(Settings.CanUploadMarksToSitsForYearPg), Settings.CanUploadMarksToSitsForYearPg)
			case _ => throw new IllegalStateException("setUploadMarksToSitsForYear called with invalid degreeType")
		}

		val markMap = markUploadMap.getOrElse(new collection.mutable.HashMap[String, String]())

		settings += (mapKey -> (markMap + (year.toString -> canUpload.toString)))
	}

	def getStudentRelationshipSource(relationshipType: StudentRelationshipType): StudentRelationshipSource =
		getStringMapSetting(Settings.StudentRelationshipSource)
			.flatMap {
			_.get(relationshipType.id)
		}.fold(relationshipType.defaultSource)(StudentRelationshipSource.fromCode)

	def setStudentRelationshipSource (relationshipType: StudentRelationshipType, source: StudentRelationshipSource): Unit = {
		val map = getStringMapSetting(Settings.StudentRelationshipSource, Map())
		val newMap = map + (relationshipType.id -> source.dbValue)

		settings += (Settings.StudentRelationshipSource -> newMap)
	}

	def studentRelationshipSource: Map[String, String] = getStringMapSetting(Settings.StudentRelationshipSource) getOrElse Map()
	def studentRelationshipSource_= (setting: Map[String, String]): Unit = settings += (Settings.StudentRelationshipSource -> setting)

	def studentRelationshipDisplayed: Map[String, String] = getStringMapSetting(Settings.StudentRelationshipDisplayed) getOrElse Map()
	def studentRelationshipDisplayed_= (setting: Map[String, String]): Unit = settings += (Settings.StudentRelationshipDisplayed -> setting)

	def getStudentRelationshipDisplayed(relationshipType: StudentRelationshipType): Boolean =
		studentRelationshipDisplayed
			.get(relationshipType.id).fold(relationshipType.defaultDisplay)(_.toBoolean)

	def setStudentRelationshipDisplayed(relationshipType: StudentRelationshipType, isDisplayed: Boolean): Unit = {
		studentRelationshipDisplayed = studentRelationshipDisplayed + (relationshipType.id -> isDisplayed.toString)
	}

	def getStudentRelationshipExpected(relationshipType: StudentRelationshipType, courseType: CourseType): Option[Boolean] =
		getStringMapSetting(Settings.StudentRelationshipExpected).flatMap(_.get(s"${relationshipType.id}-${courseType.code}").map(_.toBoolean))
	def setStudentRelationshipExpected(relationshipType: StudentRelationshipType, courseType: CourseType, isExpected: Boolean): Unit =
		settings += (Settings.StudentRelationshipExpected ->
			(getStringMapSetting(Settings.StudentRelationshipExpected).getOrElse(Map()) + (s"${relationshipType.id}-${courseType.code}" -> isExpected.toString))
		)

	@transient
	var relationshipService: RelationshipService = Wire[RelationshipService]

	def displayedStudentRelationshipTypes: Seq[StudentRelationshipType] =
		relationshipService.allStudentRelationshipTypes.filter { getStudentRelationshipDisplayed }

	def isStudentRelationshipTypeForDisplay(relationshipType: StudentRelationshipType): Boolean = displayedStudentRelationshipTypes.contains(relationshipType)

	def weekNumberingSystem: String = getStringSetting(Settings.WeekNumberingSystem) getOrElse WeekRange.NumberingSystem.Default
	def weekNumberingSystem_= (wnSystem: String): Unit = settings += (Settings.WeekNumberingSystem -> wnSystem)

  def defaultGroupAllocationMethod: SmallGroupAllocationMethod =
		getStringSetting(Settings.DefaultGroupAllocationMethod).map(SmallGroupAllocationMethod(_)).getOrElse(SmallGroupAllocationMethod.Default)
  def defaultGroupAllocationMethod_= (method:SmallGroupAllocationMethod): Unit =  settings += (Settings.DefaultGroupAllocationMethod->method.dbValue)

	def assignmentGradeValidation: Boolean = getBooleanSetting(Settings.AssignmentGradeValidation) getOrElse false
	def assignmentGradeValidation_= (validation: Boolean): Unit = settings += (Settings.AssignmentGradeValidation -> validation)

	def autoMarkMissedMonitoringPoints: Boolean = getBooleanSetting(Settings.AutoMarkMissedMonitoringPoints, default = false)
	def autoMarkMissedMonitoringPoints_=(enabled: Boolean) { settings += (Settings.AutoMarkMissedMonitoringPoints -> enabled) }

	def missedMonitoringPointsNotificationLevels: Department.Settings.MissedMonitoringPointsNotificationLevels =
		Department.Settings.MissedMonitoringPointsNotificationLevels(
			getIntSetting(Settings.MissedMonitoringPointsNotificationLevelLow, default = Department.Settings.MissedMonitoringPointsNotificationLevels.Defaults.Low),
			getIntSetting(Settings.MissedMonitoringPointsNotificationLevelMedium, default = Department.Settings.MissedMonitoringPointsNotificationLevels.Defaults.Medium),
			getIntSetting(Settings.MissedMonitoringPointsNotificationLevelHigh, default = Department.Settings.MissedMonitoringPointsNotificationLevels.Defaults.High)
		)

	def missedMonitoringPointsNotificationLevels_=(levels: Department.Settings.MissedMonitoringPointsNotificationLevels) {
		settings += (Settings.MissedMonitoringPointsNotificationLevelLow -> levels.low)
		settings += (Settings.MissedMonitoringPointsNotificationLevelMedium -> levels.medium)
		settings += (Settings.MissedMonitoringPointsNotificationLevelHigh -> levels.high)
	}

	// FIXME belongs in Freemarker
	def formattedGuidelineSummary:String = Option(extensionGuidelineSummary).fold("")({ raw =>
		val Splitter = """\s*\n(\s*\n)+\s*""".r // two+ newlines, with whitespace
		val nodes = Splitter.split(raw).map { p => <p>{p}</p>	}
		(NodeSeq fromSeq nodes).toString()
	})

	@transient
	var permissionsService: PermissionsService = Wire[PermissionsService]
	@transient
	lazy val owners: UnspecifiedTypeUserGroup = permissionsService.ensureUserGroupFor(this, DepartmentalAdministratorRoleDefinition)
	@transient
	lazy val extensionManagers: UnspecifiedTypeUserGroup = permissionsService.ensureUserGroupFor(this, ExtensionManagerRoleDefinition)

	def isOwnedBy(userId:String): Boolean = owners.knownType.includesUserId(userId)

	def canRequestExtension: Boolean = allowExtensionRequests
	def isExtensionManager(user:String): Boolean = extensionManagers!=null && extensionManagers.knownType.includesUserId(user)

	def addFeedbackForm(form:FeedbackTemplate): Boolean = feedbackTemplates.add(form)

	def copySettingsFrom(other: Department): Unit = {
		ensureSettings
		settings ++= other.settings
	}

	def copyExtensionManagersFrom(other: Department): Unit = {
		extensionManagers.copyFrom(other.extensionManagers)
	}

	override def postLoad() {
		ensureSettings
	}

	@OneToMany(mappedBy="scope", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
	@ForeignKey(name="none")
	@BatchSize(size=200)
	var grantedRoles:JList[DepartmentGrantedRole] = JArrayList()

	@Type(`type` = "uk.ac.warwick.tabula.data.model.DepartmentFilterRuleUserType")
	@Column(name="FilterRuleName")
	var filterRule: FilterRule = AllMembersFilterRule

	def includesMember(m: Member, d: Option[Department]): Boolean = Option(parent) match {
		case None => filterRule.matches(m, d)
		case Some(p) => filterRule.matches(m, d) && p.includesMember(m, d)
	}


	def subDepartmentsContaining(member: Member): Stream[Department] = {
		if (!includesMember(member, Option(this))) {
			Stream.empty // no point looking further down the tree if this level doesn't contain the required member
		} else {
			this #:: children.asScala.flatMap(child => child.subDepartmentsContaining(member)).toStream
		}
	}

	def replacedRoleDefinitionFor(roleDefinition: RoleDefinition): Option[CustomRoleDefinition] = {
		def matches(customRoleDefinition: CustomRoleDefinition) = {
			roleDefinition match {
				case roleDefinition: SelectorBuiltInRoleDefinition[_] =>
					customRoleDefinition.baseRoleDefinition match {
						case customRoleDefinition: SelectorBuiltInRoleDefinition[_]
							if (customRoleDefinition.getClass == roleDefinition.getClass) &&
								(roleDefinition <= customRoleDefinition) => true
						case _ => false
					}
				case _ => roleDefinition == customRoleDefinition.baseRoleDefinition
			}
		}

		customRoleDefinitions.asScala.filter { _.replacesBaseDefinition }.find(matches) match {
			case Some(role) => Option(role)
			case _ if hasParent => parent.replacedRoleDefinitionFor(roleDefinition)
			case _ => None
		}
	}

	def permissionsParents: Stream[Department] = Option(parent).toStream
	override def humanReadableId: String = name
	override def urlSlug: String = code

	/** The 'top' ancestor of this department, or itself if
	  * it has no parent.
	  */
	@tailrec
	final def rootDepartment: Department =
		if (parent == null) this
		else parent.rootDepartment

	def hasParent: Boolean = parent != null

	def hasChildren: Boolean = !children.isEmpty

	def isUpstream: Boolean = !hasParent

	override def toString: String = "Department(" + code + ")"

	def toEntityReference: DepartmentEntityReference = new DepartmentEntityReference().put(this)

}

object Department {

	object FilterRule {
		// Define a way to get from a String to a FilterRule, for use in a ConvertibleConverter
		implicit val factory: (String) => FilterRule = { name: String => withName(name) }

		val allFilterRules: Seq[FilterRule] = {
			val inYearRules = (1 until 9).map(InYearFilterRule)
			Seq(AllMembersFilterRule, UndergraduateFilterRule, PostgraduateFilterRule, DepartmentRoutesFilterRule) ++ inYearRules
		}

		def withName(name: String): FilterRule = {
			allFilterRules.find(_.name == name).get
		}
	}

	sealed trait FilterRule extends Convertible[String] {
		val name: String
		val courseTypes: Seq[CourseType]
		def matches(member: Member, department: Option[Department]): Boolean
		def getName: String = name // for Spring
		def value: String = name
		def restriction(aliasPaths: Map[String, Seq[(String, AliasAndJoinType)]], department: Option[Department] = None): Option[ScalaRestriction]
	}

	case object UndergraduateFilterRule extends FilterRule {
		val name = "UG"
		val courseTypes: Seq[CourseType] = CourseType.ugCourseTypes
		def matches(member: Member, department: Option[Department]): Boolean = member match {
			case s: StudentMember => s.mostSignificantCourseDetails.flatMap { cd => Option(cd.currentRoute) }.flatMap { route => Option(route.degreeType) } match {
				case Some(DegreeType.Undergraduate) => true
				case _ => false
			}
			case _ => false
		}
		def restriction(aliasPaths: Map[String, Seq[(String, AliasAndJoinType)]], department: Option[Department] = None): Option[ScalaRestriction] = {
			ScalaRestriction.is("route.degreeType", DegreeType.Undergraduate, aliasPaths("route"):_*)
		}
	}

	case object PostgraduateFilterRule extends FilterRule {
		val name = "PG"
		val courseTypes: Seq[CourseType] = CourseType.pgCourseTypes
		def matches(member: Member, department: Option[Department]): Boolean = member match {
			case s: StudentMember => s.mostSignificantCourseDetails.flatMap { cd => Option(cd.currentRoute) }.flatMap { route => Option(route.degreeType) } match {
				case Some(DegreeType.Undergraduate) => false
				case _ => true
			}
			case _ => false
		}
		def restriction(aliasPaths: Map[String, Seq[(String, AliasAndJoinType)]], department: Option[Department] = None): Option[ScalaRestriction] = {
			ScalaRestriction.isNot("route.degreeType", DegreeType.Undergraduate, aliasPaths("route"):_*)
		}
	}

	case object AllMembersFilterRule extends FilterRule {
		val name = "All"
		val courseTypes: Seq[CourseType] = CourseType.all
		def matches(member: Member, department: Option[Department]) = true
		def restriction(aliasPaths: Map[String, Seq[(String, AliasAndJoinType)]], department: Option[Department] = None): Option[ScalaRestriction] = {
			None
		}
	}


	case class InYearFilterRule(year:Int) extends FilterRule {
		val name=s"Y$year"
		val courseTypes: Seq[CourseType] = CourseType.all
		def matches(member: Member, department: Option[Department]): Boolean = member match{
			case s:StudentMember => s.mostSignificantCourseDetails.exists(_.latestStudentCourseYearDetails.yearOfStudy == year)
			case _=>false
		}
		def restriction(aliasPaths: Map[String, Seq[(String, AliasAndJoinType)]], department: Option[Department] = None): Option[ScalaRestriction] = {
			ScalaRestriction.is("latestStudentCourseYearDetails.yearOfStudy", year, aliasPaths("latestStudentCourseYearDetails"):_*)
		}
	}

	case object DepartmentRoutesFilterRule extends FilterRule {
		val name = "DepartmentRoutes"
		val courseTypes: Seq[CourseType] = CourseType.all
		def matches(member: Member, department: Option[Department]): Boolean = member match {
			case s: StudentMember => s.mostSignificantCourseDetails.flatMap { cd => Option(cd.currentRoute) }.exists{r => department.contains(r.adminDepartment)}
			case _ => false
		}
		def restriction(aliasPaths: Map[String, Seq[(String, AliasAndJoinType)]], department: Option[Department] = None): Option[ScalaRestriction] = {
			department.flatMap(d => ScalaRestriction.is("route.adminDepartment", d, aliasPaths("route"):_*))
		}
	}

	case class CompositeFilterRule(rules:Seq[FilterRule]) extends FilterRule{
		val name: String = rules.map(_.name).mkString(",")
		val courseTypes: Seq[CourseType] = CourseType.all
		def matches(member:Member, department: Option[Department]): Boolean = rules.forall(_.matches(member, department))
		def restriction(aliasPaths: Map[String, Seq[(String, AliasAndJoinType)]], department: Option[Department] = None): Option[ScalaRestriction] = {
			val restrictions = rules.flatMap(_.restriction(aliasPaths, department))
			ScalaRestriction.custom(conjunction(restrictions.map(_.underlying):_*), restrictions.flatMap(_.aliases.toSeq):_*)
		}
	}


	object Settings {
		val CollectFeedbackRatings = "collectFeedbackRatings"

		val AllowExtensionRequests = "allowExtensionRequests"
		val ExtensionGuidelineSummary = "extensionGuidelineSummary"
		val ExtensionGuidelineLink = "extensionGuidelineLink"

		val ShowStudentName = "showStudentName"
		val AssignmentInfoView = "assignmentInfoView"

		val PlagiarismDetection = "plagiarismDetection"
		val TurnitinExcludeBibliography = "turnitinExcludeBibliography"
		val TurnitinExcludeQuotations = "turnitinExcludeQuotations"
		val TurnitinSmallMatchWordLimit = "turnitinSmallMatchWordLimit"
		val TurnitinSmallMatchPercentageLimit = "turnitinSmallMatchPercentageLimit"

		val StudentRelationshipSource = "studentRelationshipSource"
		val StudentRelationshipDisplayed = "studentRelationshipDisplayed"
		val StudentRelationshipExpected = "studentRelationshipExpected"

		val WeekNumberingSystem = "weekNumberSystem"

		val DefaultGroupAllocationMethod = "defaultGroupAllocationMethod"

		val AutoGroupDeregistration = "autoGroupDeregistration"

		val StudentsCanScheduleMeetings = "studentsCanScheduleMeetings"

		val UploadCourseworkMarksToSits = "uploadMarksToSits"
		val UploadExamMarksToSits = "uploadExamMarksToSits"
		val CanUploadMarksToSitsForYearUg = "canUploadMarksToSitsForYearUG"
		val CanUploadMarksToSitsForYearPg = "canUploadMarksToSitsForYearPG"

		val AssignmentGradeValidation = "assignmentGradeValidation"

		val AutoMarkMissedMonitoringPoints = "autoMarkMissedMonitoringPoints"
		val MissedMonitoringPointsNotificationLevelLow = "missedMonitoringPointsNotificationLevelLow"
		val MissedMonitoringPointsNotificationLevelMedium = "missedMonitoringPointsNotificationLevelMedium"
		val MissedMonitoringPointsNotificationLevelHigh = "missedMonitoringPointsNotificationLevelHigh"
		object MissedMonitoringPointsNotificationLevels {
			object Defaults {
				val Low = 3
				val Medium = 6
				val High = 8
			}
		}
		case class MissedMonitoringPointsNotificationLevels(low: Int, medium: Int, high: Int)
	}
}

// converter for spring
class DepartmentFilterRuleConverter extends ConvertibleConverter[String, Department.FilterRule]