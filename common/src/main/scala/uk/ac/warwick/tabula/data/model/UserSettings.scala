package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.apache.commons.codec.digest.DigestUtils
import org.hibernate.annotations.Proxy
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.groups.admin.DownloadRegistersAsPdfHelper
import uk.ac.warwick.tabula.data.convert.FiniteDurationConverter
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService

import scala.concurrent.duration._

@Entity
@Proxy
class UserSettings extends GeneratedId with SettingsMap with HasNotificationSettings with PermissionsTarget {

  import UserSettings._

  @transient
  var moduleAndDepartmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]

  @Column(unique = true)
  var userId: String = _

  def alertsSubmission: String = getStringSetting(Settings.AlertsSubmission).getOrElse(AlertsNoteworthySubmissions)

  def alertsSubmission_=(alert: String): Unit = settings += (Settings.AlertsSubmission -> alert)

  def newAssignmentSettings: String = getStringSetting(Settings.NewAssignmentSettings).getOrElse(NewAssignmentPrefill)

  def newAssignmentSettings_=(prefill: String): Unit = settings += (Settings.NewAssignmentSettings -> prefill)

  def hiddenIntros: Seq[String] = getStringSeqSetting(Settings.HiddenIntros).getOrElse(Nil)

  def hiddenIntros_=(hiddenIntro: Seq[String]): Unit = settings += (Settings.HiddenIntros -> hiddenIntro)

  // It's okay for this to be null - the null choice is to use the department's setting
  def weekNumberingSystem: String = getStringSetting(Settings.WeekNumberingSystem).orNull

  def weekNumberingSystem_=(wnSystem: String): Unit = settings += (Settings.WeekNumberingSystem -> wnSystem)

  def bulkEmailSeparator: String = getStringSetting(Settings.BulkEmailSeparator).getOrElse(DefaultBulkEmailSeparator)

  def bulkEmailSeparator_=(separator: String): Unit = settings += (Settings.BulkEmailSeparator -> separator)

  // Active department should be Optional; if the user has chosen one yet they must pick the initial value
  def activeDepartment: Option[Department] = getStringSetting(Settings.ActiveDepartment).flatMap(moduleAndDepartmentService.getDepartmentByCode)

  def activeDepartment_=(department: Department): Unit = settings += (Settings.ActiveDepartment -> department.code)

  // Active academic year should be Optional; we handle getting the latest year elsewhere
  def activeAcademicYear: Option[AcademicYear] = getIntSetting(Settings.ActiveAcademicYear).map(y => AcademicYear(y))

  def activeAcademicYear_=(academicYear: AcademicYear): Unit = settings += (Settings.ActiveAcademicYear -> academicYear.startYear)

  def registerPdfShowPhotos: Boolean = getStringSetting(Settings.RegisterPdf.ShowPhotos).forall(Settings.fromForceBooleanString)

  def registerPdfShowPhotos_=(showPhotos: Boolean): Unit = settings += (Settings.RegisterPdf.ShowPhotos -> Settings.forceBooleanString(showPhotos))

  def registerPdfDisplayName: String = getStringSetting(Settings.RegisterPdf.DisplayName).getOrElse(DownloadRegistersAsPdfHelper.DisplayName.Name)

  def registerPdfDisplayName_=(name: String): Unit = settings += (Settings.RegisterPdf.DisplayName -> name)

  def registerPdfDisplayCheck: String = getStringSetting(Settings.RegisterPdf.DisplayCheck).getOrElse(DownloadRegistersAsPdfHelper.DisplayCheck.Checkbox)

  def registerPdfDisplayCheck_=(check: String): Unit = settings += (Settings.RegisterPdf.DisplayCheck -> check)

  def registerPdfSortOrder: String = getStringSetting(Settings.RegisterPdf.SortOrder).getOrElse(DownloadRegistersAsPdfHelper.SortOrder.Module)

  def registerPdfSortOrder_=(check: String): Unit = settings += (Settings.RegisterPdf.SortOrder -> check)

  def courseworkShowEmptyModules: Boolean = getBooleanSetting(Settings.CourseworkShowEmptyModules).getOrElse(DefaultCourseworkShowEmptyModules)

  def courseworkShowEmptyModules_=(showEmptyModules: Boolean): Unit = settings += (Settings.CourseworkShowEmptyModules -> showEmptyModules)

  def deptAdminReceiveStudentComments: Boolean = getBooleanSetting(Settings.ReceiveStudentComments).getOrElse(DefaultReceiveStudentComments)

  def deptAdminReceiveStudentComments_=(receiveStudentComments: Boolean): Unit = settings += (Settings.ReceiveStudentComments -> receiveStudentComments)

  def batchedNotifications: FiniteDuration =
    getStringSetting(Settings.BatchedNotifications)
      .map(FiniteDurationConverter.asDuration)
      .getOrElse(DefaultBatchedNotificationsSetting)

  def batchedNotifications_=(delay: FiniteDuration): Unit =
    settings += (Settings.BatchedNotifications -> FiniteDurationConverter.asString(delay))

  def string(key: String): String = getStringSetting(key).orNull

  def this(userId: String) = {
    this()
    this.userId = userId
  }

  override def toString: String = "UserSettings [" + settings + "]"

  def permissionsParents: LazyList[PermissionsTarget] = LazyList.empty
}

object UserSettings {
  val AlertsAllSubmissions = "allSubmissions"
  val AlertsNoteworthySubmissions = "lateSubmissions"
  val AlertsNoSubmissions = "none"

  val NewAssignmentPrefill = "restoreRecent"
  val NewAssignmentNothing = "none"

  val DefaultBulkEmailSeparator = ";"

  val DefaultCourseworkShowEmptyModules = true
  val DefaultReceiveStudentComments = true

  val DefaultBatchedNotificationsSetting: FiniteDuration = 10.minutes

  object Settings {
    val AlertsSubmission = "alertsSubmission"
    val NewAssignmentSettings = "newAssignmentSettings"
    val HiddenIntros = "hiddenIntros"
    val WeekNumberingSystem = "weekNumberSystem"
    val BulkEmailSeparator = "bulkEmailSeparator"
    val ActiveDepartment = "activeDepartment"
    val ActiveAcademicYear = "activeAcademicYear"
    val CourseworkShowEmptyModules = "courseworkShowEmptyModules"
    val ReceiveStudentComments = "receiveStudentComments"
    val BatchedNotifications = "batchedNotifications"

    object RegisterPdf {
      val ShowPhotos = "registerPdfShowPhotos"
      val DisplayName = "registerPdfDisplayName"
      var DisplayCheck = "registerPdfDisplayCheck"
      var SortOrder = "registerPdfSortOrder"
    }

    def hiddenIntroHash(mappedPage: String, setting: String): String = {
      val popover = mappedPage + ":" + setting
      val shaHash = DigestUtils.sha1Hex(popover)

      shaHash
    }

    // So that 'string' works on a boolean setting, force the boolean value to a string
    // Has to be a string other than "true" as that is parsed into the boolean
    val StringForcedTrue = "t"
    val StringForcedFalse = "f"

    @deprecated("Do not use for new settings, use a proper Boolean instead", since = "2019.12.3")
    def forceBooleanString(value: Boolean): String = if (value) StringForcedTrue else StringForcedFalse

    @deprecated("Do not use for new settings, use a proper Boolean instead", since = "2019.12.3")
    def fromForceBooleanString(value: String): Boolean = value match {
      case StringForcedTrue => true
      case StringForcedFalse => false
    }
  }

}
