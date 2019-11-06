package uk.ac.warwick.tabula.commands.coursework.departments

import scala.jdk.CollectionConverters._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.commands.{Description, Command}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.validators.UsercodeListValidator
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands.SelfValidating


class ExtensionSettingsCommand(val department: Department) extends Command[Unit] with SelfValidating {

  PermissionCheck(Permissions.Department.ManageExtensionSettings, department)

  var allowExtensionRequests: Boolean = department.allowExtensionRequests
  var extensionGuidelineSummary: String = department.extensionGuidelineSummary
  var extensionGuidelineLink: String = department.extensionGuidelineLink
  var extensionManagers: JList[String] = JArrayList()

  extensionManagers.addAll(department.extensionManagers.knownType.includedUserIds.asJava)

  val validUrl = """^((https?)://|(www2?)\.)[a-z0-9-]+(\.[a-z0-9-]+)+([/?].*)?$"""

  override def validate(errors: Errors): Unit = {
    if (allowExtensionRequests) {
      if (!(extensionGuidelineSummary.hasText || extensionGuidelineLink.hasText)) {
        errors.rejectValue("allowExtensionRequests", "department.settings.noExtensionGuidelines")
      }
      val firstMarkersValidator = new UsercodeListValidator(extensionManagers, "extensionManagers")
      firstMarkersValidator.validate(errors)
    }
    if (extensionGuidelineLink.hasText && !extensionGuidelineLink.matches(validUrl)) {
      errors.rejectValue("extensionGuidelineLink", "department.settings.invalidURL")
    }
  }

  override def applyInternal() {
    transactional() {
      if (features.extensions) {
        department.allowExtensionRequests = allowExtensionRequests
        department.extensionGuidelineSummary = extensionGuidelineSummary
        department.extensionGuidelineLink = extensionGuidelineLink

        department.extensionManagers.knownType.includedUserIds = extensionManagers.asScala.toSet
      }
    }
  }

  // describe the thing that's happening.
  override def describe(d: Description): Unit = {
    d.department(department)
  }
}
