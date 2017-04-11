package uk.ac.warwick.tabula.dev.web.commands

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.JBigDecimal
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model.{ModuleRegistration, StudentMember}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

class ModuleRegistrationFixtureCommand extends CommandInternal[Seq[ModuleRegistration]] with Logging {
	this: SessionComponent with TransactionalComponent  =>

	var memberDao: MemberDao = Wire[MemberDaoImpl]
	var moduleDao: ModuleDao = Wire[ModuleDaoImpl]

	var moduleCode: String = _
	var universityIds: String = _
	var academicYear: AcademicYear = _

	protected def applyInternal(): Seq[ModuleRegistration] =
		transactional() {
			val module = moduleDao.getByCode(moduleCode).get
			val cats = new JBigDecimal(12.0)

			val regs: Seq[ModuleRegistration] =
				for {
					uniId <- universityIds.split(",")
					student <- memberDao.getByUniversityId(uniId).filter { _.isInstanceOf[StudentMember] }.toSeq
					scd <- student.asInstanceOf[StudentMember].freshStudentCourseDetails
				} yield {
					val modReg = new ModuleRegistration(scd, module, cats, academicYear, "A")
					session.save(modReg)
					scd.addModuleRegistration(modReg)
					session.save(scd)
					session.flush()

					modReg
				}

			regs
		}
}
object ModuleRegistrationFixtureCommand{
	def apply(): ModuleRegistrationFixtureCommand with ComposableCommand[Seq[ModuleRegistration]] with Daoisms with AutowiringTransactionalComponent with Unaudited with PubliclyVisiblePermissions ={
		new ModuleRegistrationFixtureCommand
			with ComposableCommand[Seq[ModuleRegistration]]
			with Daoisms
		  with AutowiringTransactionalComponent
			with Unaudited
			with PubliclyVisiblePermissions

	}
}
