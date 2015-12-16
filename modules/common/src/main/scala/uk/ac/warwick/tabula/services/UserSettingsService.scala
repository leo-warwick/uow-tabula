package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.data.model.UserSettings
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.AutowiringUserSettingsDaoComponent
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.spring.Wire

trait UserSettingsServiceComponent {
	def userSettingsService: UserSettingsService
}

trait AutowiringUserSettingsServiceComponent extends UserSettingsServiceComponent {
	var userSettingsService = Wire[UserSettingsService]
}

trait UserSettingsService {
	def getByUserId(userId: String) : Option[UserSettings]
	def save(user: CurrentUser, usersettings: UserSettings)
}

@Service(value = "userSettingsService")
class UserSettingsServiceImpl extends UserSettingsService with Logging with AutowiringUserSettingsDaoComponent {
	def getByUserId(userId: String) : Option[UserSettings] = userSettingsDao.getByUserId(userId)
	def save(user: CurrentUser, newSettings: UserSettings) = userSettingsDao.save(user, newSettings)
}
