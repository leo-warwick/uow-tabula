package uk.ac.warwick.tabula.data
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.Level
import uk.ac.warwick.spring.Wire

trait LevelDaoComponent {
	val levelDao: LevelDao
}

trait AutowiringLevelDaoComponent extends LevelDaoComponent {
	val levelDao: LevelDao = Wire[LevelDao]
}

trait LevelDao {
	def saveOrUpdate(level: Level)
	def getByCode(code: String): Option[Level]
	def getAllLevelCodes: Seq[String]

}

@Repository
class LevelDaoImpl extends LevelDao with Daoisms {

	def saveOrUpdate(level: Level): Unit = session.saveOrUpdate(level)

	def getByCode(code: String): Option[Level] = {
		session.newQuery[Level]("from StudyLevel level where code = :code").setString("code", code).uniqueResult
	}

	def getAllLevelCodes: Seq[String] =
		session.newQuery[String]("select distinct code from StudyLevel").seq

}
