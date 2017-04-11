package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.system.BindListener
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.FileAttachment

trait GroupsObjects[A >: Null, B >: Null] extends PopulateOnForm {

	/** Mapping from B to an ArrayList of As. */
	var mapping: JMap[B, JList[A]] =
		LazyMaps.create { key: B => JArrayList(): JList[A] }.asJava

	var unallocated: JList[A] = LazyLists.createWithFactory { () => null } // grower, not a shower

	def populate(): Unit
	def sort(): Unit
}

trait GroupsObjectsWithFileUpload[A >: Null, B >: Null] extends GroupsObjects[A, B] with BindListener {
	var file: UploadedFile = new UploadedFile

	def validateUploadedFile(result: BindingResult): Unit
	def extractDataFromFile(file: FileAttachment, result: BindingResult): Map[B, JList[A]]

	override def onBind(result: BindingResult) {
		validateUploadedFile(result)

		if (!result.hasErrors) {
			transactional() {
				file.onBind(result)
				if (!file.attached.isEmpty) {
					processFiles(file.attached.asScala)
				}

				def processFiles(files: Seq[FileAttachment]) {
					val data = files.filter(_.hasData).flatMap { extractDataFromFile(_, result) }.toMap

					mapping.clear()
					mapping.putAll( data.asJava )
				}
			}
		}
	}
}