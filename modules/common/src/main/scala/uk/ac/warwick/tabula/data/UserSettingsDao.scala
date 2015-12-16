package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.UserSettings

trait UserSettingsDao {
	def getByUserId(userId: String) : Option[UserSettings]
	def save(user: CurrentUser, usersettings: UserSettings)
}

abstract class AbstractUserSettingsDao extends UserSettingsDao with HelperRestrictions {
	self: SessionComponent =>

	def getByUserId(userId: String) : Option[UserSettings] = {
		session.newCriteria[UserSettings]
			.add(is("userId", userId))
			.uniqueResult
	}

	def save(user: CurrentUser, newSettings: UserSettings) =  {
		val existingSettings = getByUserId(user.apparentId)
		val settingsToSave = existingSettings match {
			case Some(settings) => settings
			case None => new UserSettings(user.apparentId)
		}
		settingsToSave ++= newSettings
		session.saveOrUpdate(settingsToSave)
	}
}

@Repository("userSettingsDao")
class UserSettingsDaoImpl extends AbstractUserSettingsDao with Daoisms

trait UserSettingsDaoComponent {
	def userSettingsDao: UserSettingsDao
}

trait AutowiringUserSettingsDaoComponent extends UserSettingsDaoComponent {
	override val userSettingsDao = Wire[UserSettingsDao]
}