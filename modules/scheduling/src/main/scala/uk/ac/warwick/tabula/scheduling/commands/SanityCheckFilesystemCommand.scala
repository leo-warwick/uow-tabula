package uk.ac.warwick.tabula.scheduling.commands

import java.io.{File, FileInputStream, FileReader, FileWriter, IOException}

import org.joda.time.DateTime
import org.springframework.util.FileCopyUtils
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.AutowiringFileDaoComponent
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.{AutowiringFileAttachmentServiceComponent, SHAFileHasherComponent}

/**
 * Job to go through each FileAttachment in the database and alert if there
 * isn't a corresponding file on the filesystem.
 *
 * This will ignore any files that have been created since the last sync, if we
 * are a standby.
 */
class SanityCheckFilesystemCommand extends Command[Unit] with ReadOnly
	with AutowiringFileAttachmentServiceComponent
	with AutowiringFileDaoComponent
	with SHAFileHasherComponent {
	import SanityCheckFilesystemCommand._
	import SyncReplicaFilesystemCommand._

	PermissionCheck(Permissions.ReplicaSyncing)

	var fileSyncEnabled = Wire[JBoolean]("${environment.standby:false}")
	var dataDir = Wire[String]("${base.data.dir}")

	lazy val lastSanityCheckJobDetailsFile = new File(new File(dataDir), LastSanityCheckJobDetailsFilename)

	override def applyInternal() = transactional(readOnly = maintenanceModeService.enabled) {
		val startTime = DateTime.now

		timed("Sanity check filesystem") { timer =>
			// TAB-593 we convert allIds to a Seq here, otherwise the for-comprehension will yield a Set
			val allIds = fileAttachmentService.getAllFileIds(lastSyncDate).toSeq

			val checks = for (id <- allIds) yield fileAttachmentService.getData(id) match {
				case Some(file) =>
					// TAB-664 populate file hash if we haven't already
					fileAttachmentService.getFileById(id) map { attachment =>
						val currentHash = fileHasher.hash(new FileInputStream(file))

						if (!attachment.hash.hasText && !maintenanceModeService.enabled) {
							attachment.hash = currentHash

							fileDao.saveOrUpdate(attachment)

							(1, 0)
						} else if (attachment.hash.hasText && attachment.hash != currentHash) {
							logger.error("Expected hash for %s didn't match! Expected %s but was actually %s".format(attachment.id, attachment.hash, currentHash))
							(0, 1)
						} else (1, 0)
					} getOrElse (1, 0)

				case None =>
					// Check whether the file has since been cleaned up
					if (fileAttachmentService.getFileById(id).isEmpty) (0, 0)
					else {
						logger.error("*** File didn't exist for: " + id)
						(0, 1)
					}
			}

			val (successful, unsuccessful) = checks.foldLeft((0, 0)) {(a, b) => (a._1 + b._1, a._2 + b._2)}

			val logString = "successfulFiles," + successful +
				",failedFiles," + unsuccessful +
				",timeTaken," + timer.getTotalTimeMillis +
				",lastSuccessfulRun," + startTime.getMillis
			logger.info(logString)

			try {
				FileCopyUtils.copy(logString, new FileWriter(lastSanityCheckJobDetailsFile))
			} catch {
				case e: IOException =>
					logger.error("Failed to update last sanity check job details: " + logString + " to file: " + lastSanityCheckJobDetailsFile, e)
			}
		}
	}

	lazy val lastSyncDateFile = new File(new File(dataDir), LastSyncedDateFilename)

	private def lastSyncDate = {
		if (fileSyncEnabled && lastSyncDateFile.exists) {
			val line = FileCopyUtils.copyToString(new FileReader(lastSyncDateFile)).trim
			Some(new DateTime(line.toLong))
		} else None
	}

	override def describe(d: Description) {}

}

object SanityCheckFilesystemCommand {
	val LastSanityCheckJobDetailsFilename = "last_sanity_check_filesystem_details"
}