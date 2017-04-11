package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model._
import org.hibernate.criterion.{Order, Restrictions}
import org.hibernate.criterion.Order._
import org.hibernate.criterion.Restrictions._
import uk.ac.warwick.tabula.AcademicYear
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import scala.collection.JavaConverters._

trait AssessmentMembershipDaoComponent {
	val membershipDao: AssessmentMembershipDao
}

trait AutowiringAssessmentMembershipDaoComponent extends AssessmentMembershipDaoComponent {
	val membershipDao: AssessmentMembershipDao = Wire[AssessmentMembershipDao]
}

/**
 * TODO Rename all of this to be less Assignment-centric
 */
trait AssessmentMembershipDao {
	def find(assignment: AssessmentComponent): Option[AssessmentComponent]
	def find(group: UpstreamAssessmentGroup): Option[UpstreamAssessmentGroup]
	def find(group: AssessmentGroup): Option[AssessmentGroup]

	def save(group: AssessmentGroup): Unit
	def save(assignment: AssessmentComponent): AssessmentComponent
	def save(group: UpstreamAssessmentGroup)
	def save(member: UpstreamAssessmentGroupMember)

	def delete(group: AssessmentGroup): Unit

	def getAssessmentGroup(id: String): Option[AssessmentGroup]
	def getUpstreamAssessmentGroup(id:String): Option[UpstreamAssessmentGroup]
	def getAssessmentComponent(id: String): Option[AssessmentComponent]
	def getAssessmentComponent(group: UpstreamAssessmentGroup): Option[AssessmentComponent]

	/**
	 * Get all AssessmentComponents that appear to belong to this module.
	 *
	 *  Typically used to provide possible candidates to link to an app assignment,
	 *  in conjunction with #getUpstreamAssessmentGroups.
	 */
	def getAssessmentComponents(module: Module, inUseOnly: Boolean): Seq[AssessmentComponent]
	def getAssessmentComponents(department: Department, includeSubDepartments: Boolean): Seq[AssessmentComponent]
	def getAssessmentComponents(moduleCode: String, inUseOnly: Boolean): Seq[AssessmentComponent]

	/**
	 * Get all assessment groups that can serve this assignment this year.
	 * Should return as many groups as there are distinct OCCURRENCE values for a given
	 * assessment group code, which most of the time is just 1.
	 */
	def getUpstreamAssessmentGroups(component: AssessmentComponent, academicYear: AcademicYear): Seq[UpstreamAssessmentGroup]
	def getUpstreamAssessmentGroups(registration: ModuleRegistration): Seq[UpstreamAssessmentGroup]
	def getUpstreamAssessmentGroupsNotIn(ids: Seq[String], academicYears: Seq[AcademicYear]): Seq[String]

	def emptyMembers(groupsToEmpty:Seq[String]): Int

	def countPublishedFeedback(assignment: Assignment): Int
	def countFullFeedback(assignment: Assignment): Int

	/**
	 * Get SITS enrolled assignments/small group sets *only* - doesn't include any assignments where someone
	 * has modified the members group. Also doesn't take into account assignments where the
	 * user has been manually excluded. AssignmentMembershipService.getEnrolledAssignemnts
	 * takes this into account.
	 */
	def getSITSEnrolledAssignments(user: User): Seq[Assignment]
	def getSITSEnrolledSmallGroupSets(user: User): Seq[SmallGroupSet]

	def save(gb: GradeBoundary): Unit
	def deleteGradeBoundaries(marksCode: String): Unit
	def getGradeBoundaries(marksCode: String): Seq[GradeBoundary]
}

@Repository
class AssessmentMembershipDaoImpl extends AssessmentMembershipDao with Daoisms with Logging {

	def getSITSEnrolledAssignments(user: User): Seq[Assignment] =
		session.newQuery[Assignment]("""select a
			from
				Assignment a
					join a.assessmentGroups ag
					join ag.assessmentComponent.upstreamAssessmentGroups uag
					join uag.members uagms with uagms.universityId = :universityId
			where
					uag.academicYear = a.academicYear and
					uag.occurrence = ag.occurrence and
					a.deleted = false and a._archived = false and a._hiddenFromStudents = false""")
			.setString("universityId", user.getWarwickId)
			.distinct.seq

	def getSITSEnrolledSmallGroupSets(user: User): Seq[SmallGroupSet] =
		session.newQuery[SmallGroupSet]("""select sgs
			from SmallGroupSet sgs
				join sgs.assessmentGroups ag
				join ag.assessmentComponent.upstreamAssessmentGroups uag
				join uag.members uagms with uagms.universityId = :universityId
			where
				uag.academicYear = sgs.academicYear and
				uag.occurrence = ag.occurrence and
				sgs.deleted = false and sgs.archived = false""")
			.setString("universityId", user.getWarwickId)
			.distinct.seq

	/**
	 * Tries to find an identical AssessmentComponent in the database, based on the
	 * fact that moduleCode and sequence uniquely identify the assignment.
	 */
	def find(assignment: AssessmentComponent): Option[AssessmentComponent] =
		session.newCriteria[AssessmentComponent]
			.add(is("moduleCode", assignment.moduleCode))
			.add(is("sequence", assignment.sequence))
			.uniqueResult

	def find(group: UpstreamAssessmentGroup): Option[UpstreamAssessmentGroup] =
		session.newCriteria[UpstreamAssessmentGroup]
			.add(is("assessmentGroup", group.assessmentGroup))
			.add(is("academicYear", group.academicYear))
			.add(is("moduleCode", group.moduleCode))
			.add(is("occurrence", group.occurrence))
			.add(is("sequence", group.sequence))
			.uniqueResult

	def find(group: AssessmentGroup): Option[AssessmentGroup] = {
		if (group.assignment == null && group.smallGroupSet == null) None
		else {
			val criteria = session.newCriteria[AssessmentGroup]
				.add(is("assessmentComponent", group.assessmentComponent))
				.add(is("occurrence", group.occurrence))

			if (group.assignment != null) {
				criteria.add(is("assignment", group.assignment))
			} else {
				criteria.add(is("smallGroupSet", group.smallGroupSet))
			}

			criteria.uniqueResult
		}
	}

	def save(group: AssessmentGroup): Unit = session.saveOrUpdate(group)

	def save(assignment: AssessmentComponent): AssessmentComponent =
		find(assignment)
			.map { existing =>
			if (existing needsUpdatingFrom assignment) {
				existing.copyFrom(assignment)
				session.update(existing)
			}

			existing
		}
			.getOrElse { session.save(assignment); assignment }

	def save(group: UpstreamAssessmentGroup): Unit =
		find(group).getOrElse { session.save(group) }

	def save(member: UpstreamAssessmentGroupMember): Unit = session.saveOrUpdate(member)


	def getAssessmentGroup(id:String): Option[AssessmentGroup] = getById[AssessmentGroup](id)

	def getUpstreamAssessmentGroup(id:String): Option[UpstreamAssessmentGroup] = getById[UpstreamAssessmentGroup](id)

	def delete(group: AssessmentGroup) {
		group.assignment.assessmentGroups.remove(group)
		session.delete(group)
		session.flush()
	}

	def getAssessmentComponent(id: String): Option[AssessmentComponent] = getById[AssessmentComponent](id)

	def getAssessmentComponent(group: UpstreamAssessmentGroup): Option[AssessmentComponent] = {
		session.newCriteria[AssessmentComponent]
			.add(is("moduleCode", group.moduleCode))
			.add(is("assessmentGroup", group.assessmentGroup))
			.uniqueResult
	}

	/** Just gets components of type Assignment for this module, not all components. */
	def getAssessmentComponents(module: Module, inUseOnly: Boolean): Seq[AssessmentComponent] = {
		val c = session.newCriteria[AssessmentComponent]
			.add(Restrictions.like("moduleCode", module.code.toUpperCase + "-%"))
			.addOrder(Order.asc("sequence"))

		if (inUseOnly) {
			c.add(is("inUse", true))
		}

		c.seq
	}

	/** Just gets components of type Assignment for modules in this department, not all components. */
	def getAssessmentComponents(department: Department, includeSubDepartments: Boolean): Seq[AssessmentComponent] = {
		// TAB-2676 Include modules in sub-departments optionally
		def modules(d: Department): Seq[Module] = d.modules.asScala
		def modulesIncludingSubDepartments(d: Department): Seq[Module] =
			modules(d) ++ d.children.asScala.flatMap(modulesIncludingSubDepartments)

		val deptModules =
			if (includeSubDepartments) modulesIncludingSubDepartments(department)
			else modules(department)

		if (deptModules.isEmpty) Nil
		else {
			val components = safeInSeq(() => {
				session.newCriteria[AssessmentComponent]
					.add(is("inUse", true))
					.addOrder(asc("moduleCode"))
					.addOrder(asc("sequence"))
			}, "module", deptModules)
			components.sortBy { c =>
				(c.moduleCode, c.sequence)
			}
		}
	}

	def getAssessmentComponents(moduleCode: String, inUseOnly: Boolean): Seq[AssessmentComponent] = {
		val c = session.newCriteria[AssessmentComponent]
			.add(is("moduleCode", moduleCode))
			.addOrder(Order.asc("sequence"))
		if (inUseOnly) {
			c.add(is("inUse", true))
		}
		c.seq
	}

	def countPublishedFeedback(assignment: Assignment): Int = {
		session.createSQLQuery("""select count(*) from feedback where assignment_id = :assignmentId and released = 1""")
			.setString("assignmentId", assignment.id)
			.uniqueResult
			.asInstanceOf[Number].intValue
	}

	def countFullFeedback(assignment: Assignment): Int = {  //join f.attachments a
		session.createQuery("""select count(*) from Feedback f
			where f.assignment = :assignment
			and not (actualMark is null and actualGrade is null and f.attachments is empty)""")
			.setEntity("assignment", assignment)
			.uniqueResult
			.asInstanceOf[Number].intValue
	}

	def getUpstreamAssessmentGroups(component: AssessmentComponent, academicYear: AcademicYear): Seq[UpstreamAssessmentGroup] = {
		session.newCriteria[UpstreamAssessmentGroup]
			.add(is("academicYear", academicYear))
			.add(is("moduleCode", component.moduleCode))
			.add(is("assessmentGroup", component.assessmentGroup))
			.add(is("sequence", component.sequence))
			.seq
	}

	def getUpstreamAssessmentGroups(registration: ModuleRegistration): Seq[UpstreamAssessmentGroup] =
		session.newCriteria[UpstreamAssessmentGroup]
			.add(is("academicYear", registration.academicYear))
			.add(is("moduleCode", registration.toSITSCode))
			.add(is("assessmentGroup", registration.assessmentGroup))
			.add(is("occurrence", registration.occurrence))
			.seq

	def getUpstreamAssessmentGroupsNotIn(ids: Seq[String], academicYears: Seq[AcademicYear]): Seq[String] =
		session.newCriteria[UpstreamAssessmentGroup]
			// TODO Is there a way to do not-in with multiple queries?
			.add(not(safeIn("id", ids)))
			.add(safeIn("academicYear", academicYears))
			.seq
			.map(_.id)

	def emptyMembers(groupsToEmpty:Seq[String]): Int = {
		var count = 0
		val partitionedIds = groupsToEmpty.grouped(Daoisms.MaxInClauseCount)
		partitionedIds.map(batch => {
			val numDeleted = transactional() {
				session.createSQLQuery("delete from UpstreamAssessmentGroupMember where group_id in (:batch)")
					.setParameterList("batch", batch.asJava)
					.executeUpdate
			}
			count += 1000
			logger.info(s"Emptied $count groups")
			numDeleted
		}).sum

	}

	def save(gb: GradeBoundary): Unit = {
		session.save(gb)
	}

	def deleteGradeBoundaries(marksCode: String): Unit = {
		getGradeBoundaries(marksCode).foreach(session.delete)
	}

	def getGradeBoundaries(marksCode: String): Seq[GradeBoundary] = {
		session.newCriteria[GradeBoundary]
			.add(is("marksCode", marksCode))
			.seq
	}
}
