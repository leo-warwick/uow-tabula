package uk.ac.warwick.tabula.web.controllers.profiles.timetables

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController

@RequestMapping(Array("/profiles/timetable/regeneratehash"))
@Controller
class RegenerateTimetableHashController extends ProfilesController {

	@RequestMapping(method=Array(POST))
	def generateNewHash(): Mav = transactional() {
		profileService.regenerateTimetableHash(currentMember)
		Redirect(Routes.Profile.timetable(currentMember))
	}

}
