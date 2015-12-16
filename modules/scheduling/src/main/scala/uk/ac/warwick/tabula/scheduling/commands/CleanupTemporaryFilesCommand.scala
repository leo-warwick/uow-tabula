package uk.ac.warwick.tabula.scheduling.commands

import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.FileAttachmentService

class CleanupTemporaryFilesCommand extends Command[Unit] {

	PermissionCheck(Permissions.ReplicaSyncing)

	var service = Wire[FileAttachmentService]

	override def applyInternal() = transactional() {
		service.deleteOldTemporaryFiles()
	}

	override def describe(d: Description) {}
}