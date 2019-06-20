package uk.ac.warwick.tabula.commands.scheduling

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.scheduling.ObjectStorageMigrationCommand._
import uk.ac.warwick.tabula.data.{AutowiringFileDaoComponent, FileDaoComponent, FileHasherComponent, SHAFileHasherComponent}
import uk.ac.warwick.tabula.helpers.ExecutionContexts.global
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.objectstore.{AutowiringObjectStorageServiceComponent, LegacyAwareObjectStorageService, ObjectStorageServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

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

      val legacyKeys = benchmarkTask("Get all FileAttachment IDs") {
        fileDao.getAllFileIds(None)
      }

      Await.result(
        defaultStore.listKeys().map(_.toSet).flatMap { defaultKeys =>
          val keysToMigrate = legacyKeys -- defaultKeys

          benchmarkTask(s"Migrate $BatchSize keys") {
            Future.sequence(keysToMigrate.take(BatchSize).map { key =>
              legacyStore.fetch(key).flatMap {
                case null => Future.successful(None)
                case obj =>
                  logger.info(s"Migrating blob (size: ${obj.size} bytes) for key $key to default store")
                  defaultStore.push(key, obj, obj.metadata.get.copy(fileHash = Some(fileHasher.hash(obj.openStream()))))
                    .map(_ => Some(key))
              }
            })
          }
        },
        Duration.Inf
      ).flatten
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