package uk.ac.warwick.tabula.home.web.controllers

import org.springframework.stereotype.Controller

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.userlookup.Group
import collection.JavaConversions._
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.web._
import uk.ac.warwick.tabula.web.controllers._

@Controller class HomeController extends BaseController {

	hideDeletedItems

	@RequestMapping(Array("/")) def home(user: CurrentUser) = 
	  	Mav("home/view", "jumbotron" -> true) // All hail our new Jumbotron overlords
}