package uk.ac.warwick.tabula.commands.scheduling

import java.io.InputStream

import com.google.common.io.ByteSource
import org.mockito.Mockito._
import uk.ac.warwick.tabula.data.{FileDao, FileDaoComponent, FileHasherComponent, SHAFileHasherComponent}
import uk.ac.warwick.tabula.services.objectstore.{LegacyAwareObjectStorageService, ObjectStorageService, ObjectStorageServiceComponent}
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.util.files.hash.FileHasher

class ObjectStorageMigrationCommandTest extends TestBase with Mockito {

	@Test def noLegacyStore(): Unit = {
		val command = new ObjectStorageMigrationCommandInternal with SHAFileHasherComponent with ObjectStorageServiceComponent with FileDaoComponent {
			val objectStorageService: ObjectStorageService = smartMock[ObjectStorageService]
			val fileDao: FileDao = smartMock[FileDao]
		}

		command.applyInternal() should be ('empty)
	}

	private trait CommandTestSupport extends FileHasherComponent with ObjectStorageServiceComponent with FileDaoComponent {
		val defaultStoreService: ObjectStorageService = smartMock[ObjectStorageService]
		val legacyStoreService: ObjectStorageService = smartMock[ObjectStorageService]

		val objectStorageService = new LegacyAwareObjectStorageService(
			defaultService = defaultStoreService,
			legacyService = legacyStoreService
		)

		val fileDao: FileDao = smartMock[FileDao]
		val fileHasher: FileHasher = smartMock[FileHasher]
	}

	@Test def transfer(): Unit = {
		val command = new ObjectStorageMigrationCommandInternal with CommandTestSupport

		when(command.fileDao.getAllFileIds(None)) thenReturn Set("1", "2", "3", "4", "5", "6", "7", "8", "9")
		when(command.defaultStoreService.listKeys()) thenReturn Set("1", "2", "13", "4", "15", "16", "7", "18", "19").toStream

		val blob3data = mock[InputStream]
		val blob5data = mock[InputStream]
		val blob6data = mock[InputStream]
		val blob8data = mock[InputStream]
		val blob9data = mock[InputStream]

		val metadata3 = ObjectStorageService.Metadata(3, "application/custom-3", None)
		val metadata5 = ObjectStorageService.Metadata(5, "application/custom-5", None)
		val metadata6 = ObjectStorageService.Metadata(6, "application/custom-6", None)
		val metadata8 = ObjectStorageService.Metadata(8, "application/custom-8", None)
		val metadata9 = ObjectStorageService.Metadata(9, "application/custom-9", None)

		when(command.legacyStoreService.fetch("3")) thenReturn Some(blob3data)
		when(command.legacyStoreService.fetch("5")) thenReturn Some(blob5data)
		when(command.legacyStoreService.fetch("6")) thenReturn Some(blob6data)
		when(command.legacyStoreService.fetch("8")) thenReturn Some(blob8data)
		when(command.legacyStoreService.fetch("9")) thenReturn Some(blob9data)

		when(command.legacyStoreService.metadata("3")) thenReturn Some(metadata3)
		when(command.legacyStoreService.metadata("5")) thenReturn Some(metadata5)
		when(command.legacyStoreService.metadata("6")) thenReturn Some(metadata6)
		when(command.legacyStoreService.metadata("8")) thenReturn Some(metadata8)
		when(command.legacyStoreService.metadata("9")) thenReturn Some(metadata9)

		when(command.fileHasher.hash(blob3data)) thenReturn "hash3"
		when(command.fileHasher.hash(blob5data)) thenReturn "hash5"
		when(command.fileHasher.hash(blob6data)) thenReturn "hash6"
		when(command.fileHasher.hash(blob8data)) thenReturn "hash8"
		when(command.fileHasher.hash(blob9data)) thenReturn "hash9"

		command.applyInternal() should be (Set("3", "5", "6", "8", "9"))

		verify(command.defaultStoreService, times(1)).push(isEq("3"), any[ByteSource], isEq(metadata3.copy(fileHash = Some("hash3"))))
		verify(command.defaultStoreService, times(1)).push(isEq("5"), any[ByteSource], isEq(metadata5.copy(fileHash = Some("hash5"))))
		verify(command.defaultStoreService, times(1)).push(isEq("6"), any[ByteSource], isEq(metadata6.copy(fileHash = Some("hash6"))))
		verify(command.defaultStoreService, times(1)).push(isEq("8"), any[ByteSource], isEq(metadata8.copy(fileHash = Some("hash8"))))
		verify(command.defaultStoreService, times(1)).push(isEq("9"), any[ByteSource], isEq(metadata9.copy(fileHash = Some("hash9"))))
	}

}
