package uk.ac.warwick.tabula.commands.profiles

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.MemberNote
import uk.ac.warwick.tabula.helpers.StringUtils.StringToSuperString
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.services.fileserver._

import scala.collection.JavaConverters._


class DownloadMemberNoteFilesCommand (val memberNote: MemberNote) extends Command[Option[RenderableFile]] with ReadOnly {

		PermissionCheck(Permissions.MemberNotes.Read, memberNote)

		var filename: String = _

		var zipService: ZipService = Wire.auto[ZipService]

		private var fileFound: Boolean = _

		/**
		 * If filename is unset, it returns a renderable Zip of all files.
		 * If filename is set, it will return a renderable attachment if found.
		 * In either case if it's not found, None is returned.
		 */
		def applyInternal(): Option[RenderableFile] = {
			val result: Option[RenderableFile] =
				filename match {
					case filename: String if filename.hasText => {
						memberNote.attachments.asScala.find(_.name == filename).map(new RenderableAttachment(_))
					}
					case _ => Some(zipped(memberNote))
				}

			fileFound = result.isDefined
			result
		}

		private def zipped(memberNote: MemberNote) = zipService.getSomeMemberNoteAttachmentsZip(memberNote)

		override def describe(d: Description): Unit = {
			d.property("filename", filename)
		}

		override def describeResult(d: Description) {
			d.property("fileFound", fileFound)
		}
	}