package uk.ac.warwick.tabula.data.model

import java.sql.Types

import javax.persistence._
import javax.validation.constraints.NotNull
import org.hibernate.`type`.{AbstractSingleColumnStandardBasicType, StandardBasicTypes}
import org.hibernate.annotations.{Proxy, Type}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.permissions.{PermissionsSelector, PermissionsTarget}
import uk.ac.warwick.tabula.services.RelationshipService

@Entity
@Proxy
@Access(AccessType.FIELD)
class StudentRelationshipType extends PermissionsTarget with PermissionsSelector[StudentRelationshipType] with IdEquality {

  @Id
  var id: String = _

  /**
    * The URL identifier for this type. Valid URL path chars only please!!
    */
  @NotNull
  var urlPart: String = _

  /**
    * What you'd call a single actor in this relationship's context, e.g. "personal tutor".
    *
    * This should be lowercase, as it will be used in a sentence. If it's used in other places,
    * the template is expected to capitalise accordingly.
    */
  @NotNull
  var agentRole: String = _

  /**
    * What you'd call a single student in this relationship's context, e.g. "personal tutee"
    *
    * This should be lowercase, as it will be used in a sentence. If it's used in other places,
    * the template is expected to capitalise accordingly.
    */
  @NotNull
  var studentRole: String = _

  /**
    * A description of this relationship type
    */
  var description: String = _

  /**
    * The default source for this relationship's information
    */
  @Type(`type` = "uk.ac.warwick.tabula.data.model.StudentRelationshipSourceUserType")
  var defaultSource: StudentRelationshipSource = StudentRelationshipSource.Local

  /**
    * The presence of a value in this field indicates that the relationship type can be read from the RDX
    * table in SITS as well as including the corresponding RDX_EXTC value.
    */
  var defaultRdxType: String = _

  var defaultDisplay: JBoolean = true

  @Column(name = "expected_ug")
  var expectedUG: JBoolean = false

  @Column(name = "expected_pgt")
  var expectedPGT: JBoolean = false

  @Column(name = "expected_pgr")
  var expectedPGR: JBoolean = false

  @Column(name = "expected_foundation")
  var expectedFoundation: JBoolean = false

  @Column(name = "expected_presessional")
  var expectedPreSessional: JBoolean = false

  @Column(name = "sort_order")
  var sortOrder: Int = 2

  /**
    * Do we expect this member to have a relationship of this type? Controls
    * whether it is hidden when empty, or displayed with a prompt to add.
    */
  def displayIfEmpty(studentCourseDetails: StudentCourseDetails): Boolean = {
    // if a sub-department (not including root/import departments) matches this student check the settings from that department rather than the parent
    val departmentToCheck = Option(studentCourseDetails.department).map(d => d.subDepartmentsContaining(studentCourseDetails.student).filterNot(_.isImportDepartment).lastOption.getOrElse(d))

    studentCourseDetails.courseType match {
      case Some(courseType: CourseType) => courseType match {
        case CourseType.UG =>
          departmentToCheck.flatMap {
            _.getStudentRelationshipExpected(this, courseType)
          }.getOrElse(expectedUG.booleanValue)
        case CourseType.PGT =>
          departmentToCheck.flatMap {
            _.getStudentRelationshipExpected(this, courseType)
          }.getOrElse(expectedPGT.booleanValue)
        case CourseType.PGR =>
          departmentToCheck.flatMap {
            _.getStudentRelationshipExpected(this, courseType)
          }.getOrElse(expectedPGR.booleanValue)
        case CourseType.Foundation =>
          departmentToCheck.flatMap {
            _.getStudentRelationshipExpected(this, courseType)
          }.getOrElse(expectedFoundation.booleanValue)
        case CourseType.PreSessional =>
          departmentToCheck.flatMap {
            _.getStudentRelationshipExpected(this, courseType)
          }.getOrElse(expectedPreSessional.booleanValue)
        case _ => false
      }
      case _ => false
    }
  }

  def displayIfEmpty(courseType: CourseType, department: Department): Boolean = {
    courseType match {
      case CourseType.UG =>
        department.getStudentRelationshipExpected(this, courseType).getOrElse(expectedUG.booleanValue)
      case CourseType.PGT =>
        department.getStudentRelationshipExpected(this, courseType).getOrElse(expectedPGT.booleanValue)
      case CourseType.PGR =>
        department.getStudentRelationshipExpected(this, courseType).getOrElse(expectedPGR.booleanValue)
      case CourseType.Foundation =>
        department.getStudentRelationshipExpected(this, courseType).getOrElse(expectedFoundation.booleanValue)
      case CourseType.PreSessional =>
        department.getStudentRelationshipExpected(this, courseType).getOrElse(expectedPreSessional.booleanValue)
      case _ => false
    }
  }

  def isExpected(studentCourseDetails: StudentCourseDetails): Boolean = displayIfEmpty(studentCourseDetails)

  def isDefaultExpected(courseType: CourseType): Boolean = courseType match {
    case CourseType.UG => expectedUG.booleanValue
    case CourseType.PGT => expectedPGT.booleanValue
    case CourseType.PGR => expectedPGR.booleanValue
    case CourseType.Foundation => expectedFoundation.booleanValue
    case CourseType.PreSessional => expectedPreSessional.booleanValue
    case _ => false
  }

  /**
    * If the source is anything other than local, then this relationship type is read-only
    */
  def readOnly(department: Department): Boolean =
    department.getStudentRelationshipSource(this) != StudentRelationshipSource.Local


  @transient
  var relationshipService: RelationshipService = Wire[RelationshipService]

  def empty: Boolean = relationshipService.countStudentsByRelationship(this) == 0

  def permissionsParents: LazyList[PermissionsTarget] = LazyList.empty

  override def toString: String = "StudentRelationshipType(%s)".format(id)

}

object StudentRelationshipType {
  def apply(id: String, urlPart: String, agentRole: String, studentRole: String): StudentRelationshipType = {
    val relType = new StudentRelationshipType
    relType.id = id
    relType.urlPart = urlPart
    relType.agentRole = agentRole
    relType.studentRole = studentRole
    relType
  }
}

sealed abstract class StudentRelationshipSource(val dbValue: String)

object StudentRelationshipSource {

  case object Local extends StudentRelationshipSource("local")

  case object SITS extends StudentRelationshipSource("sits")

  def fromCode(code: String): StudentRelationshipSource = code match {
    case Local.dbValue => Local
    case SITS.dbValue => SITS
    case null => null
    case _ => throw new IllegalArgumentException()
  }
}

class StudentRelationshipSourceUserType extends AbstractBasicUserType[StudentRelationshipSource, String] {

  val basicType: AbstractSingleColumnStandardBasicType[String] = StandardBasicTypes.STRING

  override def sqlTypes = Array(Types.VARCHAR)

  val nullValue: String = null
  val nullObject: StudentRelationshipSource = null

  override def convertToObject(string: String): StudentRelationshipSource = StudentRelationshipSource.fromCode(string)

  override def convertToValue(relType: StudentRelationshipSource): String = relType.dbValue

}
