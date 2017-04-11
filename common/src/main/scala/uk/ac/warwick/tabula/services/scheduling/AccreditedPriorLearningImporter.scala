package uk.ac.warwick.tabula.services.scheduling

import java.sql.{ResultSet, Types}
import javax.sql.DataSource

import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.commands.scheduling.imports.ImportAccreditedPriorLearningCommand
import uk.ac.warwick.tabula.data.MemberDaoImpl
import uk.ac.warwick.tabula.data.model.MemberUserType.Student
import uk.ac.warwick.tabula.sandbox.SandboxData
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConversions._
import scala.collection.immutable.Iterable

/**
 * Import accredited prior learning data from SITS.
 *
 */

trait AccreditedPriorLearningImporter {
	def getAccreditedPriorLearning(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]): Seq[ImportAccreditedPriorLearningCommand]
}

@Profile(Array("dev", "test", "production"))
@Service
class AccreditedPriorLearningImporterImpl extends AccreditedPriorLearningImporter with TaskBenchmarking {
	import AccreditedPriorLearningImporter._

	var sits: DataSource = Wire[DataSource]("sitsDataSource")

	lazy val accreditedPriorLearningQuery = new AccreditedPriorLearningQuery(sits)

	def getAccreditedPriorLearning(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]):
		Seq[ImportAccreditedPriorLearningCommand] = {

		benchmarkTask("Fetch accredited prior learning") {
			membersAndCategories.filter { _.member.userType == Student }.flatMap { mac =>
				val universityId = mac.member.universityId
				accreditedPriorLearningQuery.executeByNamedParam(Map("universityId" -> universityId)).toSeq.map(row => new ImportAccreditedPriorLearningCommand(row))
			}.seq
		}
	}
}

@Profile(Array("sandbox")) @Service
class SandboxAccreditedPriorLearningImporter extends AccreditedPriorLearningImporter {
	var memberDao: MemberDaoImpl = Wire.auto[MemberDaoImpl]

	def getAccreditedPriorLearning(membersAndCategories: Seq[MembershipInformation], users: Map[String, User]): Seq[ImportAccreditedPriorLearningCommand] =
		membersAndCategories flatMap { mac =>
			val universityId = mac.member.universityId
			val ssoUser = users(universityId)

			mac.member.userType match {
				case Student => studentAccreditedPriorLearningDetails(universityId, ssoUser)
				case _ => Seq()
			}
		}

	def studentAccreditedPriorLearningDetails(universityId: String, ssoUser: User): Iterable[ImportAccreditedPriorLearningCommand] = {
		for {
			(code, d) <- SandboxData.Departments
			route <- d.routes.values.toSeq
			if (route.studentsStartId to route.studentsEndId).contains(universityId.toInt)
			moduleCode <- route.moduleCodes
		} yield {

			val row = AccreditedPriorLearningRow(
				scjCode = "%s/1".format(universityId),
				awardCode = "BA",
				sequenceNumber = 1,
				academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now).toString,
				cats = new JBigDecimal(15),
				levelCode = "2",
				reason = "Exemption of 30 CATS for 3 terms of Open Studies Languages"
			)

			new ImportAccreditedPriorLearningCommand(row)
		}
	}
}

object AccreditedPriorLearningImporter {
	val sitsSchema: String = Wire.property("${schema.sits}")

	// Choose the most significant course, flagged by SCJ_UDFA value of Y in the Student Course Join (SCJ) table
	// if there is one for the student's SPR (Student Programme Route) record.
	// If there is no most signif SCJ for this SPR, choose the SCJ with the max sequence number.
	def AccreditedPriorLearning = f"""
		select scj.scj_code, -- Student Course Join code
			sac.awd_code,  -- award code, e.g. "BA"
			sac.sac_seq, -- sequence code on the Student Award Credits table
			sac.ayr_code,
			sac.sac_crdt, -- credit value e.g. 30
			sac.lev_code, -- level code, e.g. 1, M1
			sac.sac_resn -- description e.g. "Exemption of 30 CATs from optional modules"
		from $sitsSchema.ins_stu stu -- student table

		join $sitsSchema.ins_spr spr -- join to Student Program Route table to get SPR code
			on spr.spr_stuc = stu.stu_code

		join $sitsSchema.cam_sac sac -- join to Student Award Crecits table
			on sac.spr_code = spr.spr_code

	join $sitsSchema.srs_scj scj -- join to Student Course Join table to get the SCJ code
		on (
			scj.scj_sprc = sac.spr_code -- join to Student Award Credits table
			and (
				scj.scj_udfa = 'Y' -- the current SCJ
				or (
					scj.scj_seq2 = ( -- or, if no current, then the latest SCJ
						select max(scj2.scj_seq2) from $sitsSchema.srs_scj scj2
							where scj2.scj_sprc = spr.spr_code
							and not exists (select * from $sitsSchema.srs_scj where scj_udfa in ('Y','y') and scj.scj_sprc = sac.spr_code)
					)
				)
			)
		)

		where stu.stu_code = :universityId and sac.ayr_code is not null"""


	def mapResultSet(resultSet: ResultSet): AccreditedPriorLearningRow = {
		new AccreditedPriorLearningRow(resultSet.getString("scj_code"), // Student Course Join code
		resultSet.getString("awd_code"), // award code
		resultSet.getInt("sac_seq"), // student award credit sequence code
		resultSet.getString("ayr_code"),
		resultSet.getBigDecimal("sac_crdt"), // credit
		resultSet.getString("lev_code"), // level code
		resultSet.getString("sac_resn")) // description
	}

	class AccreditedPriorLearningQuery(ds: DataSource)
		extends MappingSqlQuery[AccreditedPriorLearningRow](ds, AccreditedPriorLearning) {
		declareParameter(new SqlParameter("universityId", Types.VARCHAR))
		compile()
		override def mapRow(resultSet: ResultSet, rowNumber: Int): AccreditedPriorLearningRow = mapResultSet(resultSet)
	}
}

case class AccreditedPriorLearningRow(
	val scjCode: String,
	val awardCode: String,
	val sequenceNumber: Int,
	val academicYear: String,
	val cats: JBigDecimal,
	val levelCode: String,
	val reason: String) {}
