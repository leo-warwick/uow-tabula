package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.{Level}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.{LevelDaoComponent, AutowiringLevelDaoComponent}

/**
 * Handles data about levels
 */
trait LevelServiceComponent {
	def levelService: LevelService
}


trait AutowiringLevelServiceComponent extends LevelServiceComponent {
	var levelService: LevelService = Wire[LevelService]
}

trait LevelService {
	def levelFromCode(code: String): Option[Level]
}

abstract class AbstractLevelService extends LevelService {
	self: LevelDaoComponent =>

	def levelFromCode(code: String): Option[Level] = code.maybeText.flatMap {
		someCode => levelDao.getByCode(someCode.toLowerCase)
	}
}

@Service ("levelService")
class LevelServiceImpl extends AbstractLevelService with AutowiringLevelDaoComponent
