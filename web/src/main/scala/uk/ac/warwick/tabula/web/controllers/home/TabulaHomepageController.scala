package uk.ac.warwick.tabula.web.controllers.home

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.system.UserNavigationGeneratorImpl
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers._

@Controller class TabulaHomepageController extends BaseController {

	hideDeletedItems

	@RequestMapping(Array("/")) def home(user: CurrentUser): Mav = {
	  Mav("home/view",
			"userNavigation" -> UserNavigationGeneratorImpl(user.apparentUser, forceUpdate = true),
			"jumbotron" -> true
		).noLayoutIf(ajax) // All hail our new Jumbotron overlords
	}

}