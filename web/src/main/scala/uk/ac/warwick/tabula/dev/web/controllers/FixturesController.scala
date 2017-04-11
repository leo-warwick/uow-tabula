package uk.ac.warwick.tabula.dev.web.controllers

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.dev.web.commands.FixturesCommand
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/fixtures/setup"))
class FixturesController {

	@RequestMapping
	def apply(cmd: FixturesCommand): Mav = {
		cmd.apply()
		Mav("sysadmin/fixture-setup-success")
	}

}