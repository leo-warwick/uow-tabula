package uk.ac.warwick.tabula.permissions

import org.apache.commons.lang3.builder.{EqualsBuilder, HashCodeBuilder}
import uk.ac.warwick.tabula.CaseObjectEqualityFixes
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

import scala.reflect.ClassTag

sealed abstract class Permission(val description: String) extends CaseObjectEqualityFixes[Permission] {
  val getName: String = Permissions.shortName(getClass.asInstanceOf[Class[_ <: Permission]])

  val isScoped = true
}

sealed abstract class ScopelessPermission(description: String) extends Permission(description) {
  override val isScoped = false
}

sealed abstract class SelectorPermission[A <: PermissionsSelector[A]](val selector: PermissionsSelector[A], description: String)
  extends Permission(description) {

  override val getName: String = SelectorPermission.shortName(getClass.asInstanceOf[Class[_ <: SelectorPermission[A]]])

  def <=[B <: PermissionsSelector[B]](other: SelectorPermission[B]): Boolean = other match {
    case that: SelectorPermission[A] => selector <= that.selector.asInstanceOf[PermissionsSelector[A]]
    case _ => false
  }

  override def equals(other: Any): Boolean = other match {
    case that: SelectorPermission[A] =>
      new EqualsBuilder()
        .append(getName, that.getName)
        .append(selector, that.selector)
        .build()
    case _ => false
  }

  override def hashCode(): Int =
    new HashCodeBuilder()
      .append(getName)
      .append(selector)
      .build()

  override def toString: String = "%s(%s)".format(super.toString(), selector)
}

trait PermissionsSelector[A <: PermissionsSelector[A]] {
  def id: String

  def description: String

  def isWildcard = false

  def <=(that: PermissionsSelector[A]): Boolean = that match {
    case any if any.isWildcard => true
    case any => this == any
  }
}

object PermissionsSelector {
  val AnyId: String = "*" // A special ID for converting to and from the catch-all selector

  def Any[A <: PermissionsSelector[A] : ClassTag]: PermissionsSelector[A] = new PermissionsSelector[A] {
    val id: String = AnyId

    val description = "*"

    override val isWildcard = true

    override def <=(that: PermissionsSelector[A]): Boolean = {
      // Any is only <= other wildcards
      that.isWildcard
    }

    override val toString = "*"

    override val hashCode: Int = id.hashCode

    override def equals(other: Any): Boolean = other match {
      case that: PermissionsSelector[A @unchecked] =>
        new EqualsBuilder()
          .append(id, that.id)
          .build()
      case _ => false
    }

  }
}

case class CheckablePermission(permission: Permission, scope: Option[PermissionsTarget])

object CheckablePermission {
  def apply(permission: ScopelessPermission): CheckablePermission = new CheckablePermission(permission, None)

  def apply(permission: Permission, scope: PermissionsTarget): CheckablePermission = new CheckablePermission(permission, Some(scope))
}

object SelectorPermission {
  private val ObjectClassPrefix = Permissions.getClass.getName

  def of[A <: PermissionsSelector[A]](name: String, selector: Object): SelectorPermission[A] = {
    try {
      // Go through the magical hierarchy
      val clz = Class.forName(ObjectClassPrefix + name.replace('.', '$'))
      clz.getConstructors()(0).newInstance(selector).asInstanceOf[SelectorPermission[A]]
    } catch {
      case _: ClassNotFoundException => throw new IllegalArgumentException("Selector permission " + name + " not recognised")
    }
  }

  def shortName(clazz: Class[_ <: SelectorPermission[_]]): String
  = clazz.getName.substring(ObjectClassPrefix.length, clazz.getName.length).replace('$', '.')
}

/* To avoid nasty namespace/scope clashes, stick all of this in a Permission object */
object Permissions {

  private val ObjectClassPrefix = Permissions.getClass.getName

  /**
    * Create an Permission from an action name (e.g. "Module.Create").
    * Most likely useful in view templates, for permissions checking.
    *
    * Note that, like the templates they're used in, the correctness isn't
    * checked at runtime.
    */
  def of(name: String): Permission = {
    try {
      // Go through the magical hierarchy
      val clz = Class.forName(ObjectClassPrefix + name.replace('.', '$') + "$")
      clz.getDeclaredField("MODULE$").get(null).asInstanceOf[Permission]
    } catch {
      case _: ClassNotFoundException => throw new IllegalArgumentException("Permission " + name + " not recognised")
      case _: ClassCastException => throw new IllegalArgumentException("Permission " + name + " is not an endpoint of the hierarchy")
    }
  }

  def shortName(clazz: Class[_ <: Permission]): String
  = clazz.getName.substring(ObjectClassPrefix.length, clazz.getName.length - 1).replace('$', '.')

  /* ScopelessPermissions are Permissions that can be resolved without having to worry about scope */
  case object UserPicker extends ScopelessPermission("Use the user picker")
  case object GodMode extends ScopelessPermission("Enable god mode")
  case object ManageMaintenanceMode extends ScopelessPermission("Manage maintenance mode settings")
  case object ManageEmergencyMessage extends ScopelessPermission("Manage emergency message")
  case object ImportSystemData extends ScopelessPermission("Import data from other systems")
  case object ReplicaSyncing extends ScopelessPermission("Manually run replica syncing")
  case object ViewAuditLog extends ScopelessPermission("View and search the audit log")
  case object ViewObjectStorage extends ScopelessPermission("View and search object storage")
  case object DownloadZipFromJob extends ScopelessPermission("Download ZIP file from Job")
  case object ManageSyllabusPlusLocations extends ScopelessPermission("Manage Syllabus+ location mappings")
  case object ReviewMitCircsDepartments extends ScopelessPermission("See a list of students without a valid mitcircs department")

  // Masquerade no longer scopeless, can only masquerade as users who have a Member record against scope
  case object Masquerade extends Permission("Masquerade as other users")

  object RolesAndPermissions {
    case object Create extends Permission("Add roles and permissions")
    case object Read extends Permission("View roles and permissions")
    case object Update extends Permission("Edit roles and permissions")
    case object Delete extends Permission("Remove roles and permissions")

    case object ManageCustomRoles extends Permission("Manage custom roles")
  }

  object Department {
    case object ArrangeRoutesAndModules extends Permission("Sort routes and modules into sub-departments")
    case object ViewManualMembershipSummary extends Permission("View all assignments and small group sets with manually added students")
    case object ManageExtensionSettings extends Permission("Manage extension settings")
    case object ManageDisplaySettings extends Permission("Manage display settings")
    case object ManageNotificationSettings extends Permission("Manage notification settings")
    case object ManageMarkingDescriptors extends Permission("Manage marking descriptors")
    case object DownloadFeedbackReport extends Permission("Generate a feedback report")
    case object ManageProfiles extends Permission("Manage student profiles")
    case object Manage extends Permission("Manage sub-departments")
    case object Reports extends Permission("Generate reports")
    case object ExamGrids extends Permission("Generate exam grids")
  }

  object Module {
    // We don't Read a module, we ManageAssignments on it
    case object Administer extends Permission("Administer")
    case object ManageAssignments extends Permission("Manage assignments")
    case object ManageExams extends Permission("Manage exams")
    case object ManageSmallGroups extends Permission("Manage small groups")
    case object Create extends Permission("Add a module")
    case object Update extends Permission("Edit a module")
    case object Delete extends Permission("Remove a module")
    case object ViewTimetable extends Permission("View a module's timetable")
  }

  object Route {
    case object Administer extends Permission("Administer")
    case object Manage extends Permission("Manage routes")
  }

  object Assignment {
    case object ImportFromExternalSystem extends Permission("Import assignments from SITS")
    case object Archive extends Permission("Archive an assignment")
    case object MarkOnBehalf extends Permission("Mark submissions on behalf of a marker")
    case object Create extends Permission("Add an assignment")
    case object Read extends Permission("View an assignment's settings")
    case object Update extends Permission("Edit an assignment")
    case object Delete extends Permission("Delete an assignment")
  }

  object Exam {
    case object Read extends Permission("View an exam's settings")
    case object Create extends Permission("Add an exam")
    case object Update extends Permission("Edit an exam")
    case object Delete extends Permission("Delete an exam")
  }

  object Submission {
    case object ViewPlagiarismStatus extends Permission("View plagiarism status for a coursework submission")
    case object ManagePlagiarismStatus extends Permission("Manage a coursework submission's plagiarism status")
    case object CheckForPlagiarism extends Permission("Check a coursework submission for plagiarism")
    case object SendReceipt extends Permission("Send a receipt for a coursework submission")
    case object ReleaseForMarking extends Permission("Release a coursework submission for marking")
    case object Create extends Permission("Add a coursework submission")
    case object Read extends Permission("View a coursework submission")
    case object Update extends Permission("Edit a coursework submission")
    case object Delete extends Permission("Remove a coursework submission")
    case object CreateOnBehalfOf extends Permission("Add a coursework submission on behalf of another member")
  }

  object Feedback {
    case object Publish extends Permission("Release feedback to a student")
    case object UnPublish extends Permission("Unpublish a students feedback")
    case object Rate extends Permission("Rate feedback received")
    case object Manage extends Permission("Manage feedback")
    case object Read extends Permission("View feedback")
    case object DownloadMarksTemplate extends Permission("Download a marks template for all marks")
  }

  object MarkerFeedback {
    case object Manage extends Permission("Manage marker feedback")
    case object DownloadMarksTemplate extends Permission("Download a marks template for own marks")
  }

  object Marks {
    case object MarksManagement extends ScopelessPermission("Marks management across all departments")
    case object UploadToSits extends ScopelessPermission("Upload marks to SITS")
    case object ReviewComponentMarksUpload extends ScopelessPermission("See the progress of departmental component mark upload")
    case object OverwriteAgreedMarks extends Permission("Overwrite agreed marks")
    case object ModifyResits extends Permission("Modify resits")
  }

  object Extension {
    case object MakeRequest extends Permission("Make an extension request")
    case object Create extends Permission("Create an extension for a student")
    case object Read extends Permission("View extensions and extension requests")
    case object Update extends Permission("Edit an extension")
    case object Delete extends Permission("Remove an extension")
  }

  object FeedbackTemplate {
    case object Read extends Permission("View a feedback template")
    case object Manage extends Permission("Manage feedback templates")
  }

  object MarkingWorkflow {
    case object Read extends Permission("View a marking workflow")
    case object Manage extends Permission("Manage marking workflows")
  }

  object Profiles {
    case object Search extends ScopelessPermission("Search student profiles")
    case object ViewSearchResults extends Permission("View profile search results")

    object Read {
      case object Core extends Permission("View a member's name, Warwick email, job title and University ID")

      // a hardcoded check in ViewProfileCommand stops users seeing profiles for different departments - this allows us to bypass that check
      case object CoreCrossDepartment extends Permission("View profiles for any department")
      case object CoreStale extends Permission("View profiles of stale members")
      case object Photo extends Permission("View a member's photo")

      /* We can split these back into DateOfBirth, Nationality and HomeEmail if any role requires a subset */
      case object PrivateDetails extends Permission("View a member's date of birth, nationality, and alternative email address")
      case object Gender extends Permission("View a member's gender")
      case object NextOfKin extends Permission("View a member's next of kin")
      case object HomeAndTermTimeAddresses extends Permission("View a member's home and term-time addresses")
      case object TelephoneNumber extends Permission("View a member's telephone number")
      case object MobileNumber extends Permission("View a member's mobile number")
      case object Usercode extends Permission("View a member's usercode")
      case object SmallGroups extends Permission("View a member's small groups")
      case object Coursework extends Permission("View a member's coursework")
      case object Timetable extends Permission("View a member's personal timetable")
      case object TimetablePrivateFeed extends Permission("View a member's private timetable feed")
      case object Tier4VisaRequirement extends Permission("View a member's tier 4 visa requirement")
      case object CasUsed extends Permission("View whether a CAS has been used by a student to obtain a visa")
      case object Disability extends Permission("View a member's reported disability")
      case object AccreditedPriorLearning extends Permission("View a student's accredited prior learning")
      case object ReasonableAdjustments extends Permission("View a student's reasonable adjustments")
      case object ReasonableAdjustmentsNotes extends Permission("View a student's reasonable adjustments notes")

      object StudentCourseDetails {
        case object Core extends Permission("View a student's basic course, route and department details")
        case object Status extends Permission("View a student's enrolment and study status")
        case object SpecialExamArrangements extends Permission("View a student's special exam arrangements")
      }

      case class RelationshipStudents(relationshipType: PermissionsSelector[StudentRelationshipType])
        extends SelectorPermission(relationshipType, "View a member's students")

      object ModuleRegistration {
        case object Core extends Permission("View a student's module registrations")
        case object Results extends Permission("View a student's module results")
      }
    }

    object StudentRelationship {
      case class Read(relationshipType: PermissionsSelector[StudentRelationshipType])
        extends SelectorPermission(relationshipType, "View a student relationship")

      case class Manage(relationshipType: PermissionsSelector[StudentRelationshipType])
        extends SelectorPermission(relationshipType, "Manage student relationships")
    }

    object MeetingRecord {
      case class Read(relationshipType: PermissionsSelector[StudentRelationshipType])
        extends SelectorPermission(relationshipType, "View a meeting record")

      case class ReadDetails(relationshipType: PermissionsSelector[StudentRelationshipType])
        extends SelectorPermission(relationshipType, "View the contents of a meeting record")

      case class Manage(relationshipType: PermissionsSelector[StudentRelationshipType])
        extends SelectorPermission(relationshipType, "Manage meeting records")

      case object Approve extends Permission("Approve a meeting record")
    }

    object ScheduledMeetingRecord {
      case class Manage(relationshipType: PermissionsSelector[StudentRelationshipType])
        extends SelectorPermission(relationshipType, "Manage scheduled meeting records")

      case object Confirm extends Permission("Confirm whether a scheduled meeting record took place")
    }

  }

  object SmallGroups {
    case object Archive extends Permission("Archive small groups")
    case object Create extends Permission("Create small groups")
    case object Read extends Permission("View small groups")
    case object ReadMembership extends Permission("View small group membership")
    case object Update extends Permission("Edit small groups")
    case object Delete extends Permission("Delete small groups")
    case object Allocate extends Permission("Allocate students to small groups")
    case object AllocateSelf extends Permission("Allocate the current user to a small group")
    case object ImportFromExternalSystem extends Permission("Import small groups from Syllabus+")
    case object UpdateMembership extends ScopelessPermission("Update department small group membership")
  }

  object SmallGroupEvents {
    case object Register extends Permission("Record attendance on small group events")
    case object ViewRegister extends Permission("View attendance at small group events")
  }

  object UserSettings {
    case object Update extends Permission("Edit user settings")
  }

  object MonitoringPoints {
    case object Manage extends Permission("Manage monitoring points")
    case object View extends Permission("View monitoring points")
    case object Record extends Permission("Record monitoring points")
    case object OverwriteReported extends Permission("Overwrite monitoring points that have been reported")
    case object Report extends Permission("Report monitoring points")
    case object Export extends ScopelessPermission("Export monitoring points to SITS")
    case object UpdateMembership extends ScopelessPermission("Update attendance monitoring scheme membership")
  }

  object MonitoringPointTemplates {
    case object View extends ScopelessPermission("View monitoring point templates")
    case object Manage extends ScopelessPermission("Manage monitoring point templates")
  }

  object StudentRelationshipType {
    case object Read extends ScopelessPermission("View student relationship types")
    case object Manage extends ScopelessPermission("Manage student relationship types")
  }

  object MemberNotes {
    case object Read extends Permission("View member notes")
    case object ReadMetadata extends Permission("View metadata on member notes")
    case object Create extends Permission("Create member notes")
    case object Update extends Permission("Edit member notes")
    case object Delete extends Permission("Delete member notes")
  }

  object Notification {
    case object Dismiss extends Permission("Dismiss and restore notifications")
  }

  object Timetabling {
    case object ViewDraft extends Permission("View draft releases of the timetable")
    case object Checker extends ScopelessPermission("View timetable feeds for students")
  }

  object MitigatingCircumstancesSubmission {
    case object Modify extends Permission("Create and modify mitigating circumstances submissions")
    case object Manage extends Permission("List, create, update and delete mitigating circumstances submissions and associated messages.")
    case object Read extends Permission("View the details and messages associated with a mitigating circumstances submission")
    case object ViewGrading extends Permission("View the grading of an existing mitigating circumstances submission")
    case object ViewOutcomes extends Permission("View the outcomes of an existing mitigating circumstances submission")
    case object Share extends Permission("Share draft mitigating circumstances submissions")
  }

  object MitigatingCircumstancesPanel {
    case object Read extends Permission("View the details and submissions on mitigating circumstances panels")
    case object Modify extends Permission("Create and modify mitigating circumstances panels")
  }
}
