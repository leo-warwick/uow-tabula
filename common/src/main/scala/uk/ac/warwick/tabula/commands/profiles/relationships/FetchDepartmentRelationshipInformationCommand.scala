package uk.ac.warwick.tabula.commands.profiles.relationships

import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.profiles.relationships.FetchDepartmentRelationshipInformationCommand.Actions
import uk.ac.warwick.tabula.data.model.{Department, Route, StudentRelationshipType}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, AutowiringRelationshipServiceComponent, ProfileServiceComponent, RelationshipServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{ItemNotFoundException, UniversityId}

import scala.collection.JavaConverters._

object FetchDepartmentRelationshipInformationCommand {

	object Actions {
		val DistributeAll = "DistributeAll"
		val DistributeSelected = "DistributeSelected"
		val RemoveSingle = "RemoveSingle"
		val RemoveFromAll = "RemoveFromAll"
		val AddAdditionalEntities = "AddAdditionalEntities"
	}

	val DepartmentAgents = "DepartmentAgents"
	val NonDepartmentAgents = "NonDepartmentAgents"

	def apply(department: Department, relationshipType: StudentRelationshipType) =
		new FetchDepartmentRelationshipInformationCommandInternal(department, relationshipType)
			with AutowiringRelationshipServiceComponent
			with AutowiringProfileServiceComponent
			with ComposableCommand[StudentAssociationResult]
			with FetchDepartmentRelationshipInformationPermissions
			with FetchDepartmentRelationshipInformationCommandState
			with FetchDepartmentRelationshipInformationCommandRequest
			with FetchDepartmentRelationshipInformationCommandBindListener
			with ReadOnly with Unaudited
}


class FetchDepartmentRelationshipInformationCommandInternal(val department: Department, val relationshipType: StudentRelationshipType)
	extends CommandInternal[StudentAssociationResult] {

	self: RelationshipServiceComponent with ProfileServiceComponent with FetchDepartmentRelationshipInformationCommandRequest
		with FetchDepartmentRelationshipInformationCommandState =>

	override def applyInternal(): StudentAssociationResult = {
		val initialState = fetchResult
		action match {
			case Actions.DistributeAll =>
				distributeAll(initialState)
				fetchResult
			case Actions.DistributeSelected =>
				distributeSelected(initialState)
				fetchResult
			case Actions.RemoveSingle =>
				removeSingle(initialState)
				fetchResult
			case Actions.RemoveFromAll =>
				removeFromAll(initialState)
				fetchResult
			case Actions.AddAdditionalEntities =>
				addAdditionalEntities(initialState)
				fetchResult
			case _ =>
				initialState
		}
	}

	protected def fetchResult: StudentAssociationResult = {
		updateDbAllocated()
		val (allocated, newUnallocated) = processAllocated
		val unallocated = processUnallocated(dbUnallocated ++ newUnallocated)

		StudentAssociationResult(unallocated, allocated)
	}

	private def processUnallocated(unallocated: Seq[StudentAssociationData]): Seq[StudentAssociationData] = {
		val newlyAllocatedUniIds = additions.asScala.mapValues(_.asScala).values.flatten.toSeq
		unallocated
			.filterNot(student => newlyAllocatedUniIds.contains(student.universityId))
			.filter(student => routes.isEmpty || routes.asScala.map(_.code).contains(student.routeCode))
			.filter(student => yearsOfStudy.isEmpty || yearsOfStudy.asScala.contains(student.yearOfStudy))
			.filter(studentQueryFilter)
	}

	private def studentQueryFilter(student: StudentAssociationData): Boolean = {
		if (query.hasText) {
			if (UniversityId.isValid(query)) {
				student.universityId == query
			} else {
				query.split("""\s+""").map(_.toLowerCase).forall(queryPart =>
					student.firstName.toLowerCase.contains(queryPart) || student.lastName.toLowerCase.contains(queryPart)
				)
			}
		} else {
			true
		}
	}

	private def processAllocated: (Seq[StudentAssociationEntityData], Seq[StudentAssociationData]) = {
		val filteredAllocated = dbAllocated.filter(entity => entityTypes.isEmpty ||
			entityTypes.asScala.contains(FetchDepartmentRelationshipInformationCommand.DepartmentAgents) && entity.isHomeDepartment.getOrElse(false) ||
			entityTypes.asScala.contains(FetchDepartmentRelationshipInformationCommand.NonDepartmentAgents) && !entity.isHomeDepartment.getOrElse(true)
		)

		val allStudents = (dbUnallocated ++ dbAllocated.flatMap(_.students)).distinct

		val allocatedWithRemovals = filteredAllocated.map(entityData => {
			if (removals.containsKey(entityData.entityId)) {
				val theseRemovals = removals.get(entityData.entityId).asScala
				entityData.updateStudents(entityData.students.filterNot(student => theseRemovals.contains(student.universityId)))
			} else {
				entityData
			}
		})

		val allocatedWithAdditionsAndRemovals = allocatedWithRemovals.map(entityData => {
			if (additions.containsKey(entityData.entityId)) {
				entityData.updateStudents(entityData.students ++ additions.get(entityData.entityId).asScala.flatMap(universityId => allStudents.find(_.universityId == universityId)))
			} else {
				entityData
			}
		})

		val originalAllocatedStudents = filteredAllocated.flatMap(_.students).distinct
		val allAllocatedStudents = allocatedWithAdditionsAndRemovals.flatMap(_.students).distinct
		val newUnallocated = originalAllocatedStudents.filterNot(allAllocatedStudents.contains)

		(allocatedWithAdditionsAndRemovals, newUnallocated)
	}

	private def distributeAll(initialState: StudentAssociationResult): Unit = {
		distribute(initialState.unallocated, initialState.allocated.filter(e => entities.contains(e.entityId)))
	}

	private def distributeSelected(initialState: StudentAssociationResult): Unit = {
		distribute(initialState.unallocated.filter(s => allocate.contains(s.universityId)), initialState.allocated.filter(e => entities.contains(e.entityId)))
	}

	private def distribute(students: Seq[StudentAssociationData], entities: Seq[StudentAssociationEntityData]): Unit = {
		def apply(unallocatedStudents: Seq[StudentAssociationData], groupedEntities: Seq[(Int, Seq[StudentAssociationEntityData])]): Seq[StudentAssociationEntityData] = {
			val thisEntityAndSize = groupedEntities.head
			val theseEntities = thisEntityAndSize._2
			val remainingEntities = groupedEntities.tail.flatMap(_._2)
			val numberOfStudentsToAllocate = {
				if (groupedEntities.tail.isEmpty)
					unallocatedStudents.size
				else
					(groupedEntities.tail.head._1 - thisEntityAndSize._1) * thisEntityAndSize._2.size
			}
			val maxStudentsPerGroup = {
				if (numberOfStudentsToAllocate < theseEntities.size)
					1 // the remainder
				else
					Math.floor(numberOfStudentsToAllocate.toFloat/theseEntities.size).toInt // Floor so the groups fill up equally; deal with the remainder on the next pass
			}
			val (studentsToAllocate, remainingStudents) = unallocatedStudents.splitAt(maxStudentsPerGroup * theseEntities.size)
			// TODO - I'd prefer this allocation to do one at a time per group, rather than the first n, then the second n, but can't think of a way to do it
			val groupedStudents = theseEntities.indices.map(index => studentsToAllocate.slice(index * maxStudentsPerGroup, (index + 1) * maxStudentsPerGroup))

			def applyEntities(studentGroups: Seq[Seq[StudentAssociationData]], entities: Seq[StudentAssociationEntityData]): Seq[StudentAssociationEntityData] = {
				val thisEntity = entities.head
				val studentIdsToAdd = studentGroups.head.map(_.universityId).filterNot(s =>
					// Don't add to additions if this association already exists
					dbAllocated.find(_.entityId == thisEntity.entityId).get.students.map(_.universityId).contains(s)
				)
				if (additions.containsKey(thisEntity.entityId)) {
					additions.put(thisEntity.entityId, JArrayList(studentIdsToAdd ++ additions.get(thisEntity.entityId).asScala))
				} else {
					additions.put(thisEntity.entityId, JArrayList(studentIdsToAdd))
				}
				if (removals.containsKey(thisEntity.entityId)) {
					studentGroups.head.foreach(s => removals.get(thisEntity.entityId).remove(s.universityId))
				}
				entities match {
					case entity :: Nil => Seq(entity.updateStudents(studentGroups.head ++ entity.students))
					case entity :: others => Seq(entity.updateStudents(studentGroups.head ++ entity.students)) ++ applyEntities(studentGroups.tail, others)
				}
			}
			val newEntities = applyEntities(groupedStudents, theseEntities)

			remainingStudents match {
				case Nil => newEntities ++ remainingEntities
				case _ => apply(remainingStudents, (newEntities ++ remainingEntities).groupBy(_.students.size).toSeq.sortBy(_._1))
			}
		}

		if (entities.nonEmpty && students.nonEmpty) {
			apply(students, entities.groupBy(_.students.size).toSeq.sortBy(_._1))
		}
	}

	private def removeSingle(initialState: StudentAssociationResult) = {
		initialState.allocated.find(_.entityId == entityToRemoveFrom).foreach(entity =>
			entity.students.find(_.universityId == studentToRemove).foreach(student => {
				// Only add to removals if the association exists in the DB
				if (dbAllocated.find(_.entityId == entity.entityId).get.students.map(_.universityId).contains(student.universityId)) {
					if (removals.containsKey(entity.entityId)) {
						removals.put(entity.entityId, JArrayList(Seq(student.universityId) ++ removals.get(entity.entityId).asScala))
					} else {
						removals.put(entity.entityId, JArrayList(student.universityId))
					}
				}
				if (additions.containsKey(entity.entityId)) {
					additions.get(entity.entityId).remove(student.universityId)
				}
			})
		)
	}

	private def removeFromAll(initialState: StudentAssociationResult) = {
		initialState.allocated.filter(entity => entities.contains(entity.entityId)).foreach(entity => {
			val studentIdsToRemove = entity.students.map(_.universityId).filter(s =>
				// Only add to removals if this association exists in the DB
				dbAllocated.find(_.entityId == entity.entityId).get.students.map(_.universityId).contains(s)
			)
			if (removals.containsKey(entity.entityId)) {
				removals.put(entity.entityId, JArrayList(studentIdsToRemove ++ removals.get(entity.entityId).asScala))
			} else {
				removals.put(entity.entityId, JArrayList(studentIdsToRemove))
			}
			if (additions.containsKey(entity.entityId)) {
				entity.students.foreach(s => additions.get(entity.entityId).remove(s.universityId))
			}
		})
	}

	private def addAdditionalEntities(initialState: StudentAssociationResult) = {
		val members = additionalEntityUserIds.asScala.flatMap(userId => profileService.getAllMembersWithUserId(userId)).distinct
		members.filterNot(m =>
			initialState.allocated.exists(_.entityId == m.universityId) && additionalEntities.contains(m.universityId)
		).foreach(m => additionalEntities.add(m.universityId))
		additionalEntityUserIds.clear()
		updateDbAllocated()
	}

}

trait FetchDepartmentRelationshipInformationPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: FetchDepartmentRelationshipInformationCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		// throw this request out if this relationship can't be edited in Tabula for this department
		if (relationshipType.readOnly(department)) {
			logger.info("Denying access to FetchDepartmentRelationshipInformationCommand since relationshipType %s is read-only".format(relationshipType))
			throw new ItemNotFoundException()
		}
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Manage(mandatory(relationshipType)), mandatory(department))
	}

}

trait FetchDepartmentRelationshipInformationCommandState {

	self: FetchDepartmentRelationshipInformationCommandRequest with RelationshipServiceComponent =>

	def department: Department
	def relationshipType: StudentRelationshipType

	lazy val dbUnallocated: Seq[StudentAssociationData] = relationshipService.getStudentAssociationDataWithoutRelationship(department, relationshipType, Seq())
	var dbAllocated: Seq[StudentAssociationEntityData] = Seq()
	def updateDbAllocated(): Unit = {
		dbAllocated = relationshipService.getStudentAssociationEntityData(department, relationshipType, Option(additionalEntities).map(_.asScala).getOrElse(Seq()))
	}
}

trait FetchDepartmentRelationshipInformationCommandBindListener extends BindListener {

	self: FetchDepartmentRelationshipInformationCommandRequest with FetchDepartmentRelationshipInformationCommandState =>

	override def onBind(result: BindingResult): Unit = {
		updateDbAllocated()

		if (removeSingleCombined.hasText) {
			val split = removeSingleCombined.split("-")
			if (split.size == 3) {
				action = Actions.RemoveSingle
				entityToRemoveFrom = split(1)
				studentToRemove = split(2)
			}
		}
	}

}

trait FetchDepartmentRelationshipInformationCommandRequest extends PermissionsCheckingMethods {

	self: FetchDepartmentRelationshipInformationCommandState =>

	var additions: JMap[String, JList[String]] =
		LazyMaps.create{entityId: String => JArrayList(): JList[String] }.asJava

	var removals: JMap[String, JList[String]] =
		LazyMaps.create{entityId: String => JArrayList(): JList[String] }.asJava

	private def routesForDepartmentAndSubDepartments(department: Department): Seq[Route] =
		(department.routes.asScala ++ department.children.asScala.flatMap { routesForDepartmentAndSubDepartments }).sorted

	lazy val allRoutes: Seq[Route] = ((routesForDepartmentAndSubDepartments(mandatory(department)) match {
		case Nil => routesForDepartmentAndSubDepartments(mandatory(department.rootDepartment))
		case someRoutes => someRoutes
	}) ++ routes.asScala).distinct.sorted(Route.DegreeTypeOrdering)

	var routes: JList[Route] = JArrayList()

	lazy val allYearsOfStudy: Seq[Int] = 1 to FilterStudentsOrRelationships.MaxYearsOfStudy

	var yearsOfStudy: JList[JInteger] = JArrayList()

	val allEntityTypes: Seq[String] = Seq(FetchDepartmentRelationshipInformationCommand.DepartmentAgents, FetchDepartmentRelationshipInformationCommand.NonDepartmentAgents)
	lazy val allEntityTypesLabels: Map[String, String] = Map(
		FetchDepartmentRelationshipInformationCommand.DepartmentAgents -> s"${department.fullName} staff",
		FetchDepartmentRelationshipInformationCommand.NonDepartmentAgents -> "Other staff"
	)

	var entityTypes: JList[String] = JArrayList()

	var query: String = ""

	var action: String = ""

	var allocate: JList[String] = JArrayList()

	var removeSingleCombined: String = ""
	var studentToRemove: String = ""
	var entityToRemoveFrom: String = ""

	var entities: JList[String] = JArrayList()

	var additionalEntityUserIds: JList[String] = JArrayList()
	var additionalEntities: JList[String] = JArrayList()

	var expanded: JMap[String, JBoolean] = JHashMap()

	var preselectStudents: JList[String] = JArrayList()
}
