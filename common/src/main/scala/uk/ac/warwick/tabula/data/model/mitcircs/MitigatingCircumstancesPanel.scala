package uk.ac.warwick.tabula.data.model.mitcircs

import javax.persistence.CascadeType.ALL
import javax.persistence._
import org.hibernate.annotations.{BatchSize, Proxy, Type}
import org.joda.time.{DateTime, LocalTime}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.MitigatingCircumstancesPanelMemberRoleDefinition
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.timetables.{EventOccurrence, RelatedUrl, TimetableEvent, TimetableEventType}
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.{AcademicYear, ToString}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

@Entity
@Proxy
@Access(AccessType.FIELD)
class MitigatingCircumstancesPanel extends GeneratedId with StringId with Serializable
  with ToString
  with PermissionsTarget
  with ToEntityReference {

  override type Entity = MitigatingCircumstancesPanel

  def this(department: Department, academicYear: AcademicYear) = {
    this()
    this.department = department
    this.academicYear = academicYear
  }

  @Column(nullable = false)
  var name: String = _

  @Column(nullable = true, name = "date")
  private var _date: DateTime = _
  def date: Option[DateTime] = Option(_date)
  def date_=(date: Option[DateTime]): Unit = _date = date.orNull

  @Column(nullable = true, name = "endDate")
  private var _endDate: DateTime = _
  def endDate: Option[DateTime] = Option(_endDate)
  def endDate_=(date: Option[DateTime]): Unit = _endDate = date.orNull

  def startTime: Option[LocalTime] = date.map(_.toLocalTime)
  def endTime: Option[LocalTime] = endDate.map(_.toLocalTime)

  @Type(`type` = "uk.ac.warwick.tabula.data.model.LocationUserType")
  @Column(nullable = true, name = "location")
  private var _location: Location = _
  def location: Option[Location] = Option(_location)
  def location_=(location: Option[Location]): Unit = _location = location.orNull

  @ManyToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id")
  var department: Department = _

  @Basic
  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Column(nullable = false)
  var academicYear: AcademicYear = AcademicYear.now()

  @OneToMany(mappedBy = "_panel", fetch = FetchType.LAZY)
  @BatchSize(size = 200)
  private val _submissions: JSet[MitigatingCircumstancesSubmission] = JHashSet()

  // TODO don't know why we'd need to wrap this in a tx when we have OpenSessionInViewInterceptor
  def submissions: Set[MitigatingCircumstancesSubmission] = transactional(readOnly = true) {
    _submissions.asScala.toSet
  }

  def addSubmission(submission: MitigatingCircumstancesSubmission): Unit = {
    submission.panel = this
    _submissions.add(submission)
  }

  def removeSubmission(submission: MitigatingCircumstancesSubmission): Boolean = {
    submission.panel = null
    _submissions.remove(submission)
  }

  @Column(nullable = false)
  var lastModified: DateTime = DateTime.now()

  @transient
  var permissionsService: PermissionsService = Wire[PermissionsService]

  @Type(`type` = "uk.ac.warwick.tabula.data.model.SSOUserType")
  @Column(nullable = true, name = "chair")
  private var _chair: User = _
  def chair: Option[User] = Option(_chair)
  def chair_=(user: Option[User]): Unit = _chair = user.orNull

  @Type(`type` = "uk.ac.warwick.tabula.data.model.SSOUserType")
  @Column(nullable = true, name = "secretary")
  private var _secretary: User = _
  def secretary: Option[User] = Option(_secretary)
  def secretary_=(user: Option[User]): Unit = _secretary = user.orNull

  @transient
  private lazy val _viewers: UnspecifiedTypeUserGroup = permissionsService.ensureUserGroupFor(scope = this, MitigatingCircumstancesPanelMemberRoleDefinition)
  def viewers: Set[User] = _viewers.users
  def viewers_=(userIds: Set[String]): Unit = _viewers.knownType.includedUserIds = userIds

  def members: Set[User] = viewers -- Set(chair, secretary).flatten

  def toEventOccurrence(context: TimetableEvent.Context): Option[EventOccurrence] =
    if (date.isEmpty || endDate.isEmpty) None
    else Some(
      EventOccurrence(
        uid = id,
        name = name,
        title = name,
        description = s"${submissions.size} submission${if (submissions.size != 1) "s" else ""}",
        eventType = TimetableEventType.Other("Panel"),
        start = date.get.toLocalDateTime,
        end = endDate.get.toLocalDateTime,
        location = location,
        onlineDeliveryUrl = None,
        parent = TimetableEvent.Parent(department),
        comments = None,
        staff = viewers.toSeq,
        relatedUrl = Some(RelatedUrl(
          urlString = Routes.mitcircs.Admin.Panels.view(this),
          title = Some("Panel information")
        )),
        attendance = None
      )
    )

  override def toStringProps: Seq[(String, Any)] = Seq(
    "id" -> id
  )

  override def permissionsParents: LazyList[PermissionsTarget] = LazyList(department)

}
