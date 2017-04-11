package uk.ac.warwick.tabula.commands.scheduling

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.groups.admin.reusable.{FindStudentsForDepartmentSmallGroupSetCommandFactory, FindStudentsForDepartmentSmallGroupSetCommandFactoryImpl, UpdateStudentsForDepartmentSmallGroupSetCommandFactory, UpdateStudentsForDepartmentSmallGroupSetCommandFactoryImpl}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, FeaturesComponent}

import scala.collection.JavaConverters._

object UpdateLinkedDepartmentSmallGroupSetsCommand {
	def apply() =
		new UpdateLinkedDepartmentSmallGroupSetsCommandInternal(
			FindStudentsForDepartmentSmallGroupSetCommandFactoryImpl,
			UpdateStudentsForDepartmentSmallGroupSetCommandFactoryImpl
		)	with ComposableCommandWithoutTransaction[Seq[DepartmentSmallGroupSet]]
			with AutowiringFeaturesComponent
			with AutowiringProfileServiceComponent
			with AutowiringSmallGroupServiceComponent
			with UpdateLinkedDepartmentSmallGroupSetsDescription
			with UpdateLinkedDepartmentSmallGroupSetsPermissions
}

class UpdateLinkedDepartmentSmallGroupSetsCommandInternal(
	findStudentsCommandFactory: FindStudentsForDepartmentSmallGroupSetCommandFactory,
	updateCommandFactory: UpdateStudentsForDepartmentSmallGroupSetCommandFactory
) extends CommandInternal[Seq[DepartmentSmallGroupSet]]	with Logging with TaskBenchmarking {

	self: FeaturesComponent with SmallGroupServiceComponent =>

	override def applyInternal(): Seq[DepartmentSmallGroupSet] = {
		val setsToUpdate = transactional(readOnly = true) { smallGroupService.listDepartmentSetsForMembershipUpdate }

		logger.info(s"${setsToUpdate.size} sets need membership updating")

		setsToUpdate.foreach { set =>
			val staticStudentIds = transactional(readOnly = true) {
				val cmd = findStudentsCommandFactory.apply(set.department, set)
				cmd.populate()
				cmd.doFind = true
				cmd.apply().staticStudentIds
			}
			transactional() {
				val updateCommand = updateCommandFactory.apply(set.department, set)
				updateCommand.linkToSits = true
				updateCommand.filterQueryString = set.memberQuery
				updateCommand.staticStudentIds.clear()
				updateCommand.staticStudentIds.addAll(staticStudentIds)
				updateCommand.includedStudentIds.clear()
				updateCommand.includedStudentIds.addAll(set.members.knownType.includedUserIds.asJava)
				updateCommand.excludedStudentIds.clear()
				updateCommand.excludedStudentIds.addAll(set.members.knownType.excludedUserIds.asJava)
				updateCommand.apply()
			}
		}

		setsToUpdate
	}

}

trait UpdateLinkedDepartmentSmallGroupSetsPermissions extends RequiresPermissionsChecking {

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroups.UpdateMembership)
	}

}

trait UpdateLinkedDepartmentSmallGroupSetsDescription extends Describable[Seq[DepartmentSmallGroupSet]] {

	override lazy val eventName = "UpdateLinkedDepartmentSmallGroupSets"

	override def describe(d: Description) {

	}
}
