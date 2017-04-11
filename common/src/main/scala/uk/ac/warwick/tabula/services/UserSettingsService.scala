package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.data.model.UserSettings
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.helpers.Logging
import com.fasterxml.jackson.databind.ObjectMapper
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.spring.Wire

trait UserSettingsServiceComponent {
	def userSettingsService: UserSettingsService
}

trait AutowiringUserSettingsServiceComponent extends UserSettingsServiceComponent {
	var userSettingsService: UserSettingsService = Wire[UserSettingsService]
}

trait UserSettingsService {
	def getByUserId(userId: String) : Option[UserSettings]
	def save(user: CurrentUser, usersettings: UserSettings)
}

@Service(value = "userSettingsService")
class UserSettingsServiceImpl extends UserSettingsService with Daoisms with Logging {

	val json: ObjectMapper = Wire.auto[ObjectMapper]

	def getByUserId(userId: String) : Option[UserSettings] = {
		session.newCriteria[UserSettings]
			.add(is("userId", userId))
			.uniqueResult
	}

	def save(user: CurrentUser, newSettings: UserSettings): Unit =  {
		val existingSettings = getByUserId(user.apparentId)
		val settingsToSave = existingSettings match {
			case Some(settings) => settings
			case None => new UserSettings(user.apparentId)
		}
		settingsToSave ++= newSettings
		session.saveOrUpdate(settingsToSave)
	}

}
