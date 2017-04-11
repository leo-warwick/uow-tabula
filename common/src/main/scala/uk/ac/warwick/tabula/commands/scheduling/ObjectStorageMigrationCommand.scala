package uk.ac.warwick.tabula.commands.scheduling

import java.io.InputStream

import com.google.common.io.ByteSource
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.scheduling.ObjectStorageMigrationCommand._
import uk.ac.warwick.tabula.data.{AutowiringFileDaoComponent, FileDaoComponent, FileHasherComponent, SHAFileHasherComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.objectstore.{AutowiringObjectStorageServiceComponent, LegacyAwareObjectStorageService, ObjectStorageServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}

object ObjectStorageMigrationCommand {
	type CommandType = Appliable[Set[String]]

	// The number of keys to transfer per run
	val BatchSize = 1000

	def apply(): CommandType =
		new ObjectStorageMigrationCommandInternal
			with ComposableCommand[Set[String]]
			with ObjectStorageMigrationPermissions
			with AutowiringObjectStorageServiceComponent
			with AutowiringFileDaoComponent
			with SHAFileHasherComponent
			with Unaudited with ReadOnly
}

class ObjectStorageMigrationCommandInternal extends CommandInternal[Set[String]] with TaskBenchmarking {
	self: ObjectStorageServiceComponent with FileDaoComponent with FileHasherComponent =>

	override def applyInternal(): Set[String] = objectStorageService match {
		case legacyAware: LegacyAwareObjectStorageService =>
			val defaultStore = legacyAware.defaultService
			val legacyStore = legacyAware.legacyService

			val legacyKeys = benchmarkTask("Get all FileAttachment IDs") { fileDao.getAllFileIds(None) }
			val defaultKeys = benchmarkTask("Get all object store keys") { defaultStore.listKeys().toSet }

			val keysToMigrate = legacyKeys -- defaultKeys

			benchmarkTask(s"Migrate $BatchSize keys") {
				keysToMigrate.take(BatchSize).flatMap { key =>
					val data = new ByteSource {
						override def openStream(): InputStream = legacyStore.fetch(key).orNull
					}

					for {
						metadata <- legacyStore.metadata(key)
						fileHash <- legacyStore.fetch(key).map(fileHasher.hash) // Intentionally a different stream to above
					} yield {
						logger.info(s"Migrating blob (size: ${metadata.contentLength} bytes) for key $key to default store")
						defaultStore.push(key, data, metadata.copy(fileHash = Some(fileHash)))
						key
					}
				}
			}
		case _ =>
			logger.warn("No legacy aware object storage service found - can this be removed?")
			Set.empty
	}
}

trait ObjectStorageMigrationPermissions extends RequiresPermissionsChecking {
	override def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.ReplicaSyncing)
	}
}