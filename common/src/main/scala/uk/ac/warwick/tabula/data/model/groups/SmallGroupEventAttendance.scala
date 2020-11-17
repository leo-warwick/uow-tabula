package uk.ac.warwick.tabula.data.model.groups

import javax.persistence._
import javax.validation.constraints._
import org.hibernate.annotations.{BatchSize, Proxy, Type}
import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.{GeneratedId, Module}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState._
import uk.ac.warwick.tabula.permissions.PermissionsTarget

@Access(AccessType.FIELD)
@Entity
@Proxy
@Table(uniqueConstraints = Array(
  new UniqueConstraint(columnNames = Array("occurrence_id", "universityId"))
))
class SmallGroupEventAttendance extends GeneratedId with PermissionsTarget with Serializable {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "occurrence_id")
  var occurrence: SmallGroupEventOccurrence = _

  @NotNull
  var universityId: String = _

  @NotNull
  @Type(`type` = "uk.ac.warwick.tabula.data.model.attendance.AttendanceStateUserType")
  var state: AttendanceState = _

  @Column(name = "online_attendance")
  var onlineAttendance: Boolean = _

  var updatedDate: DateTime = _

  // TAB-7353 - joinedOn is non-null only if the student joined the parent group after this occurrence was due to take place
  // it is set to the date that the student joined the parent group
  var joinedOn: DateTime = _
  def expectedToAttend: Boolean = Option(joinedOn).isEmpty

  @NotNull
  var updatedBy: String = _

  /**
    * SmallGroupEventAttendance that has been added manually represents a student who would
    * not normally attend that SmallGroupEvent (because they're not a member of the SmallGroup.students
    * UserGroup). They have been added manually by a tutor or administrator; if the student is subsequently
    * removed then they will no longer show on the register.
    */
  @Column(name = "added_manually")
  var addedManually: Boolean = false

  /**
    * If addedManually = true, this optionally represent a SmallGroupEventAttendance that this one
    * is marked as replacing. i.e. this SmallGroupEventAttendance "replaces" the other
    * SmallGroupEventAttendance, e.g. if the student has asked to attend a different group for
    * one week only.
    */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "replaces_attendance_id")
  var replacesAttendance: SmallGroupEventAttendance = _

  /**
    * Inverse of replacesAttendance; contains all SmallGroupEventAttendances that are marked as
    * 'replacing' this one. Behaviour where this contains >1 item is undefined.
    */
  @OneToMany(mappedBy = "replacesAttendance")
  @BatchSize(size = 5)
  var replacedBy: JSet[SmallGroupEventAttendance] = JHashSet()

  def permissionsParents: LazyList[SmallGroupEventOccurrence] = LazyList(occurrence)

  def date: Option[LocalDate] = occurrence.date

  def event: SmallGroupEvent = occurrence.event

  def academicYear: AcademicYear = occurrence.academicYear

  def attended: Boolean = state == Attended

  def missed: Boolean = state == MissedAuthorised || state == MissedUnauthorised

  def module: Module = occurrence.module
}
