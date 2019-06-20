package uk.ac.warwick.tabula.services

import com.google.common.io.ByteSource
import org.scalatest.time.{Millis, Seconds, Span}
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.SHAFileHasherComponent
import uk.ac.warwick.tabula.services.objectstore.{ObjectStorageService, ObjectStorageServiceComponent}

class ZipCreatorTest extends TestBase {

  val transientObjectStore: ObjectStorageService = createTransientObjectStore()

  val creator = new ZipCreator() with SHAFileHasherComponent with ObjectStorageServiceComponent {
    override val objectStorageService: ObjectStorageService = transientObjectStore
  }

  @Test def itWorks {
    val items = Seq(
      ZipFileItem("one.txt", ByteSource.wrap("one".getBytes("UTF-8")), 3),
      ZipFileItem("two.txt", ByteSource.wrap("two".getBytes("UTF-8")), 3),
      ZipFolderItem("folder", Seq(
        ZipFileItem("three.txt", ByteSource.wrap("three".getBytes("UTF-8")), 5),
        ZipFileItem("four.txt", ByteSource.wrap("four".getBytes("UTF-8")), 4)
      ))
    )

    val zip = creator.createUnnamedZip(items).futureValue
    zip.byteSource.openStream() should not be null
    zip.contentType should be("application/zip")

    creator.invalidate(zip.filename).futureValue
    transientObjectStore.fetch(ZipCreator.objectKey(zip.filename)).futureValue.openStream() should be(null)

    val name = "myzip/under/a/folder"
    val namedZip = creator.getZip(name, items).futureValue
    namedZip.byteSource.openStream() should not be null
    namedZip.contentType should be("application/zip")

    // getting zip without any items should effectively be a no-op
    creator.getZip(name, Seq()).futureValue.contentLength should be(namedZip.contentLength)

    creator.invalidate(name).futureValue
    transientObjectStore.fetch(ZipCreator.objectKey(zip.filename)).futureValue.openStream() should be(null)
  }

  @Test def trunc {
    creator.trunc("steve", 100) should be("steve")
    creator.trunc("steve", 5) should be("steve")
    creator.trunc("steve", 3) should be("ste")

    creator.trunc(".htaccess", 3) should be(".ht")

    // We don't include the extension in length calculations to make it easier
    creator.trunc("bill.ted", 3) should be("bil.ted")
    creator.trunc("bill.ted", 5) should be("bill.ted")
  }

}