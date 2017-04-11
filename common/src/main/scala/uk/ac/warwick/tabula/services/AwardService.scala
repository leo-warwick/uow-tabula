package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.{Award}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.{AwardDaoComponent, AutowiringAwardDaoComponent}

/**
 * Handles data about awards
 */
trait AwardServiceComponent {
	def awardService: AwardService
}


trait AutowiringAwardServiceComponent extends AwardServiceComponent {
	var awardService: AwardService = Wire[AwardService]
}

trait AwardService {
	def awardFromCode(code: String): Option[Award]
}

abstract class AbstractAwardService extends AwardService {
	self: AwardDaoComponent =>

	def awardFromCode(code: String): Option[Award] = code.maybeText.flatMap {
		someCode => awardDao.getByCode(someCode.toLowerCase)
	}
}

@Service ("awardService")
class AwardServiceImpl extends AbstractAwardService with AutowiringAwardDaoComponent
