package uk.ac.warwick.tabula.data.model

import javax.persistence._

import org.apache.commons.codec.digest.DigestUtils
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.groups.admin.DownloadRegistersAsPdfHelper
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService

import scala.Seq
import scala.collection._

@Entity
class UserSettings extends GeneratedId with SettingsMap with HasNotificationSettings with PermissionsTarget {
	import UserSettings._

	@transient
	var moduleAndDepartmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]

	@Column(unique = true)
	var userId: String = _

	def alertsSubmission: String = getStringSetting(Settings.AlertsSubmission).getOrElse(AlertsNoteworthySubmissions)
	def alertsSubmission_= (alert: String): Unit = settings += (Settings.AlertsSubmission -> alert)

	def hiddenIntros: Seq[String] = getStringSeqSetting(Settings.HiddenIntros).getOrElse(Nil)
	def hiddenIntros_= (hiddenIntro: Seq[String]): Unit = settings += (Settings.HiddenIntros -> hiddenIntro)

	// It's okay for this to be null - the null choice is to use the department's setting
	def weekNumberingSystem: String = getStringSetting(Settings.WeekNumberingSystem).orNull
	def weekNumberingSystem_= (wnSystem: String): Unit = settings += (Settings.WeekNumberingSystem -> wnSystem)

	def bulkEmailSeparator: String = getStringSetting(Settings.BulkEmailSeparator).getOrElse(DefaultBulkEmailSeparator)
	def bulkEmailSeparator_= (separator: String): Unit = settings += (Settings.BulkEmailSeparator -> separator)

	def profilesDefaultView: String = getStringSetting(Settings.ProfilesDefaultView).getOrElse(DefaultProfilesDefaultView)
	def profilesDefaultView_= (view: String): Unit = settings += (Settings.ProfilesDefaultView -> view)

	// Active department should be Optional; if the user has chosen one yet they must pick the initial value
	def activeDepartment: Option[Department] = getStringSetting(Settings.ActiveDepartment).flatMap(moduleAndDepartmentService.getDepartmentByCode)
	def activeDepartment_= (department: Department): Unit = settings += (Settings.ActiveDepartment -> department.code)

	// Active academic year should be Optional; we handle getting the latest year elsewhere
	def activeAcademicYear: Option[AcademicYear] = getIntSetting(Settings.ActiveAcademicYear).map(y => AcademicYear(y))
	def activeAcademicYear_= (academicYear: AcademicYear): Unit = settings += (Settings.ActiveAcademicYear -> academicYear.startYear)

	def registerPdfShowPhotos: Boolean = getStringSetting(Settings.RegisterPdf.ShowPhotos).forall(Settings.fromForceBooleanString)
	def registerPdfShowPhotos_= (showPhotos: Boolean): Unit = settings += (Settings.RegisterPdf.ShowPhotos -> Settings.forceBooleanString(showPhotos))

	def registerPdfDisplayName: String = getStringSetting(Settings.RegisterPdf.DisplayName).getOrElse(DownloadRegistersAsPdfHelper.DisplayName.Name)
	def registerPdfDisplayName_= (name: String): Unit = settings += (Settings.RegisterPdf.DisplayName -> name)

	def registerPdfDisplayCheck: String = getStringSetting(Settings.RegisterPdf.DisplayCheck).getOrElse(DownloadRegistersAsPdfHelper.DisplayCheck.Checkbox)
	def registerPdfDisplayCheck_= (check: String): Unit = settings += (Settings.RegisterPdf.DisplayCheck -> check)

	def registerPdfSortOrder: String = getStringSetting(Settings.RegisterPdf.SortOrder).getOrElse(DownloadRegistersAsPdfHelper.SortOrder.Module)
	def registerPdfSortOrder_= (check: String): Unit = settings += (Settings.RegisterPdf.SortOrder -> check)

	def string(key: String): String = getStringSetting(key).orNull

	def this(userId: String) = {
		this()
		this.userId = userId
	}

	override def toString: String = "UserSettings [" + settings + "]"

	def permissionsParents: Stream[Nothing] = Stream.empty
}

object UserSettings {


	val AlertsAllSubmissions = "allSubmissions"
	val AlertsNoteworthySubmissions = "lateSubmissions"
	val AlertsNoSubmissions = "none"

	val DefaultBulkEmailSeparator = ";"

	val DefaultProfilesDefaultView = "gadget"

	object Settings {
		val AlertsSubmission = "alertsSubmission"
		val HiddenIntros = "hiddenIntros"
		val WeekNumberingSystem = "weekNumberSystem"
		val BulkEmailSeparator = "bulkEmailSeparator"
		val ProfilesDefaultView = "profilesDefaultView"
		val ActiveDepartment = "activeDepartment"
		val ActiveAcademicYear = "activeAcademicYear"

		object RegisterPdf {
			val ShowPhotos = "registerPdfShowPhotos"
			val DisplayName = "registerPdfDisplayName"
			var DisplayCheck = "registerPdfDisplayCheck"
			var SortOrder = "registerPdfSortOrder"
		}
			
		def hiddenIntroHash(mappedPage: String, setting: String): String = {
			val popover = mappedPage + ":" + setting
			val shaHash = DigestUtils.shaHex(popover)

			shaHash
		}

		// So that 'string' works on a boolean setting, force the boolean value to a string
		// Has to be a string other than "true" as that is parsed into the boolean
		val StringForcedTrue = "t"
		val StringForcedFalse = "f"

		def forceBooleanString(value: Boolean): String = value match {
			case true => StringForcedTrue
			case false => StringForcedFalse
		}

		def fromForceBooleanString(value: String): Boolean = value match {
			case StringForcedTrue => true
			case StringForcedFalse => false
		}
	}
}
