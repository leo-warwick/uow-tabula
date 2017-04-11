package uk.ac.warwick.tabula.web

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.web.controllers.sysadmin.SysadminController

class ApplicationTest extends AppContextTestBase {

	@Autowired var sysadminController: SysadminController = _

	@Test def itWorks = {
		assert(beans.containsBean("userLookup"))
	}

	// Can resolve message codes from any controller
	@Test def messageResolving = {
		sysadminController.getMessage("NotEmpty") should be("You need to put something here.")
		sysadminController.getMessage("userId.notingroup", "alvin") should be("The usercode alvin isn't in this group.")
	}

}
