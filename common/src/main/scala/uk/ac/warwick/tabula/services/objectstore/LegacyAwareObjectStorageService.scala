package uk.ac.warwick.tabula.services.objectstore

import java.io.InputStream

import com.google.common.io.ByteSource

class LegacyAwareObjectStorageService(val defaultService: ObjectStorageService, val legacyService: ObjectStorageService) extends ObjectStorageService {

  // Services in order of preference
  private val services = Seq(defaultService, legacyService)

  override def keyExists(key: String): Boolean = services.exists(_.keyExists(key))

  override def fetch(key: String): Option[InputStream] = services.find(_.keyExists(key)).flatMap(_.fetch(key))

	override def metadata(key: String): Option[ObjectStorageService.Metadata] = services.find(_.keyExists(key)).flatMap(_.metadata(key))

	override def push(key: String, in: ByteSource, metadata: ObjectStorageService.Metadata): Unit  = services.head.push(key, in, metadata)

	override def delete(key: String): Unit  = services.foreach(_.delete(key))

  /**
    * Not guaranteed to be distinct (unless you call distinct on it) but shouldn't be used anyway.
    */
  override def listKeys(): Stream[String] = services.toStream.flatMap { _.listKeys() }

	override def afterPropertiesSet(): Unit = services.foreach(_.afterPropertiesSet())
}