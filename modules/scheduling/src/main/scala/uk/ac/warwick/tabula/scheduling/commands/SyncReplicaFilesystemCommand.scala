package uk.ac.warwick.tabula.scheduling.commands

import java.io.{File, FileInputStream, FileNotFoundException, FileOutputStream, FileReader, FileWriter, IOException}

import org.apache.commons.io.IOUtils
import org.apache.http.HttpStatus
import org.apache.http.util.EntityUtils
import org.joda.time.DateTime
import org.json.{JSONArray, JSONException, JSONObject}
import org.springframework.util.{Assert, FileCopyUtils}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, Description, ReadOnly}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.scheduling.helpers.HttpResponseHandlers
import uk.ac.warwick.tabula.scheduling.services.AutowiringMessageAuthenticationCodeGeneratorComponent
import uk.ac.warwick.tabula.scheduling.web.controllers.sync.{DownloadFileController, ListFilesController}
import uk.ac.warwick.tabula.services.{AutowiringFileAttachmentServiceComponent, SHAFileHasherComponent}
import uk.ac.warwick.util.core.StopWatch
import uk.ac.warwick.util.core.spring.FileUtils
import uk.ac.warwick.util.httpclient.httpclient4.HttpMethodExecutor.Method
import uk.ac.warwick.util.httpclient.httpclient4.SimpleHttpMethodExecutor
import uk.ac.warwick.util.web.{Uri, UriBuilder}

import scala.util.control.Breaks._

/**
 * This is a ReadOnly command because it runs in Maintenance mode on the replica
 */
class SyncReplicaFilesystemCommand extends Command[SyncReplicaResult] with ReadOnly with Logging with HttpResponseHandlers
	with AutowiringFileAttachmentServiceComponent with SHAFileHasherComponent with AutowiringMessageAuthenticationCodeGeneratorComponent {
	import SyncReplicaFilesystemCommand._

	PermissionCheck(Permissions.ReplicaSyncing)

	// Back once again
	var replicaMaster = Wire[String]("${tabula.sync.replica.master}")

	var dataDir = Wire[String]("${base.data.dir}")

	lazy val listFilesUrl = Uri.parse(replicaMaster + "/scheduling/sync/listFiles.json")
	lazy val getFileUrl = Uri.parse(replicaMaster + "/scheduling/sync/getFile")

	override def applyInternal(): SyncReplicaResult = timed("Data sync") { timer =>
		// Loop until either no files are received, or we receive the same ones as last time
		var startDate = lastCreatedDate
		var files: JSONArray = null
		var lastRetrievedCreationDate: DateTime = null

		val result = new SyncReplicaResult

		try {
			breakable { while (true) {
				logger.debug("Getting list of files: " + listFilesUrl + " : " + startDate.getMillis)

				val jsonOption = listFiles(startDate)

				// We return here because this is a fault. We should not continue to write the sync log at the end of execution
				jsonOption.foreach { json =>

					try {
						lastRetrievedCreationDate = new DateTime(json.getLong("lastFileReceived"))
					} catch {
						case e: Throwable =>
							logger.debug("No files received")
							break
					}

					if (json.getJSONArray("files") == files && lastRetrievedCreationDate != startDate) {
						logger.debug("We're getting the same files again - stop!")
						break
					}

					files = json.getJSONArray("files")

					val allFiles = for (i <- 0 to files.length - 1) yield files.getJSONObject(i)
					copyMissingFiles(allFiles, startDate, result)

					val maxResponses = json.getInt("maxResponses")
					if (files.length < maxResponses) {
						logger.debug("There should be no more files, as the number returned: " + files.length + " is lower than the maxResponses: " + maxResponses)
						break
					}

					if (lastRetrievedCreationDate == startDate) {
						// We've already got the initial set, now need to get those onwards (on this date)
						syncSameDateFiles(allFiles, startDate, result)

						// Once we've got all of the same-date files, increment the date by one millisecond, so we don't get the same files all over again
						lastRetrievedCreationDate = lastRetrievedCreationDate.plusMillis(1)
					}

					// There are more left to fetch
					startDate = lastRetrievedCreationDate

				}
			} }

			// Store the last created date of the last updated file
			updateLastSynchedDateFile(result)

			updateSyncLogFile(result, timer, success = true)

			result
		} catch {
			case e: Exception =>
				logger.error("Couldn't sync replica - error reading file: " + lastCreatedFile.getPath, e)

				// if we have synched any files, should update the last-created date file
				// Store the Last-Created Date of the last-updated file
				updateLastSynchedDateFile(result)

				updateSyncLogFile(result, timer, success = false)

				null
		}
	}

	lazy val lastCreatedFile = new File(new File(dataDir), LastSyncedDateFilename)

	private def lastCreatedDate = {
		if (lastCreatedFile.exists) {
			val line = FileCopyUtils.copyToString(new FileReader(lastCreatedFile)).trim
			new DateTime(line.toLong).minus(LastJobOverlapMillis)
		} else {
			// Start at the beginning of time
			FirstTimeStartDate
		}
	}

	private def updateLastSynchedDateFile(result: SyncReplicaResult) {
		if (Option(result.lastCreatedDateBeforeAnyFailures).isDefined) {
			try {
				FileCopyUtils.copy(result.lastCreatedDateBeforeAnyFailures.getMillis.toString, new FileWriter(lastCreatedFile))
			} catch {
				case e: IOException =>
					logger.error("Failed to update last created date" + result.lastCreatedDateBeforeAnyFailures + "to file: " + lastCreatedFile, e)
			}
		}
	}

	lazy val lastSyncJobDetailsFile = new File(new File(dataDir), LastSyncJobDetailsFilename)

	private def updateSyncLogFile(result: SyncReplicaResult, timer: StopWatch, success: Boolean) {
		val lastCreatedDateBeforeFailures = Option(result.lastCreatedDateBeforeAnyFailures) map { _.getMillis } getOrElse(-1)
		val logString =
			"filesTransferred," + result.filesTransferred +
			",failedTransfers," + result.failedTransfers +
			",filesAlreadyExisted," + result.filesAlreadyExist +
			",fileTransferRetries," + result.fileTransferRetries +
			",timeTaken," + timer.getTotalTimeMillis +
			",successfullyCompleted," + success +
			",lastCreatedDateBeforeAnyFailures," + lastCreatedDateBeforeFailures +
			",lastRun," + new DateTime().getMillis

		try {
			FileCopyUtils.copy(logString, new FileWriter(lastSyncJobDetailsFile))
		} catch {
			case e: IOException =>
				logger.error("Failed to update last sync job details: " + logString + " to file: " + lastSyncJobDetailsFile, e)
		}
	}

	private def listFiles(startDate: DateTime, startFromId: String = null) = {
		logger.debug("Getting all files created since: " + startDate)

		val url = new UriBuilder(listFilesUrl).addQueryParameter(StartParam, startDate.getMillis.toString)

		if (startFromId.hasText) url.addQueryParameter(StartFromIdParam, startFromId)

		val ex = new SimpleHttpMethodExecutor(Method.post)
		ex.setUrl(url.toUri)
		ex.setConnectionTimeout(HttpTimeout)
		ex.setRetrievalTimeout(HttpTimeout)

		try {
			ex.execute(handle({ response =>
				try {
					response.getStatusLine.getStatusCode match {
						case HttpStatus.SC_OK => Option(response.getEntity) match {
							case Some(entity) => Some(new JSONObject(EntityUtils.toString(entity)))
							case _ => None
						}
						case _ => None
					}
				} catch {
					case e: JSONException =>
						logger.error("Invalid JSON received from " + url)
						None
				}
			})).getRight
		} catch {
			case e: IOException =>
				logger.error("Couldn't get files from " + url)
				None
		}
	}

	private def syncSameDateFiles(theLastFiles: Seq[JSONObject], startDate: DateTime, result: SyncReplicaResult) {
		var lastFiles = theLastFiles
		breakable {
			while(true) {
				// we've got a whole list of hashes with the same date
				// so now get the ones with this date, from a certain hash onwards
				logger.debug("Found a whole list of hashes with the last created date:" + startDate)

				val lastId = lastFiles.last.getString("id")

				val jsonOption = listFiles(startDate, lastId)

				jsonOption.foreach { json =>

					val theseFilesJSON = json.getJSONArray("files")
					val theseFiles = for (i <- 0 to theseFilesJSON.length - 1) yield theseFilesJSON.getJSONObject(i)

					if (theseFiles == lastFiles) {
						logger.debug("we're getting the same files again; break!")
						break
					}

					lastFiles = theseFiles

					val maxResponses = json.getInt("maxResponses")
					copyMissingFiles(lastFiles, startDate, result)

					// Break when there's no more to fetch
					if (lastFiles.length < maxResponses) {
						logger.debug("There should be no more hashes with this date: " + startDate
							+ " as the number returned: " + lastFiles.length
							+ " is lower than the maxResponses: " + maxResponses
						)
					}

				}
			}
		}
	}

	private def copyMissingFiles(files: Seq[JSONObject], startDate: DateTime, result: SyncReplicaResult) {
		for (file <- files) try {
			val id = file.getString("id")
			val hash = Option(file.optString("hash", null))

			val createdDate = new DateTime(file.getLong("createdDate"))
			val authCode = messageAuthenticationCodeGenerator.generateMessageAuthenticationCode(id)

			// There are two possible outcomes here. The first is that the database sync is up to date and we get
			// a valid attachment, for which the file may or may not exist on the filesystem. It's also possible
			// that the database isn't up to date yet, in which case we will get None - we may as well still
			// write the file at this point.
			val outputFile = fileAttachmentService.getData(id) getOrElse fileAttachmentService.targetFile(id)

			// Does the attachment already exist on the filesystem?
			if (outputFile.exists) {
				result.alreadyExists()
				result.lastCreated(createdDate)

				logger.debug("Output file already exists: " + outputFile)

				// SBTWO-4156 :: touch the file, so we don't over-zealously delete it before the DB has synched across
                Assert.isTrue(outputFile.setLastModified(startDate.getMillis))
			} else if ((!outputFile.getParentFile.exists || !outputFile.getParentFile.isDirectory) && !outputFile.getParentFile.mkdirs()) {
				throw new IllegalStateException("Couldn't create parent directory for " + outputFile)
			} else {
				fetchFile(id, hash, createdDate, authCode, outputFile, result)
			}
		} catch {
			case e: IOException =>
				logger.debug("Error trying to sync file: " + file)
				result.failedTransfer()
		}
	}

	private def fetchFile(id: String, hash: Option[String], createdDate: DateTime, authCode: String, outputFile: File, result: SyncReplicaResult) {
		val url =
			new UriBuilder(getFileUrl)
			.addQueryParameter(IdParam, id)
			.addQueryParameter(MacParam, authCode)
			.toUri

		var retries = 0
		var successful = false
		while (!successful & retries <= FetchFileRetries) {
			if (retries > 0) result.retryTransfer()
			retries += 1

			val ex = new SimpleHttpMethodExecutor(Method.post)
			ex.setUrl(url)

			successful = ex.execute(handle({ response =>
				response.getStatusLine.getStatusCode match {
					case HttpStatus.SC_OK =>
						// Create a temporary file and stream it to there, so that we can re-use the stream
						val tmpFile = File.createTempFile(id, ".tmp")
						tmpFile.deleteOnExit()

						FileCopyUtils.copy(response.getEntity.getContent, new FileOutputStream(tmpFile))

						val hashMismatch =
							hash.exists { expectedHash =>
								val actualHash = fileHasher.hash(new FileInputStream(tmpFile))
								val mismatch = expectedHash != actualHash

								if (mismatch) {
									logger.info("Error retrieving file " + outputFile + " - hash mismatch, expected " + expectedHash + " but was " + actualHash)
								}

								mismatch
							}

						if (hashMismatch) {
							false
						} else {
							val is = new FileInputStream(tmpFile)
							val os = new FileOutputStream(outputFile)

							try {
								val bytes = FileCopyUtils.copy(is, os)
								logger.info("New file created: " + id)

								result.fileTransferred(bytes)
								result.lastCreated(createdDate)

								true
							} catch {
								case e: FileNotFoundException =>
									logger.info("File not found: " + outputFile)
									false

								case e: IOException =>
									logger.info("Error copying file: " + e.getMessage + " - " + outputFile)
									false
							} finally {
								IOUtils.closeQuietly(is)
								IOUtils.closeQuietly(os)
								FileUtils.recursiveDelete(tmpFile, false)
							}
						}

					case code =>
						logger.info("Didn't receive a 200 retrieving file: " + id + "(" + code + ")")
						false
				}
			})).getRight
		}

		if (!successful) {
			result.failedTransfer()
			logger.error("Failed to transfer file: " + id)
			if (outputFile.delete()) logger.info("Output file deleted successfully")
			else logger.error("Failed to delete output file: " + outputFile)
		}
	}

	// TODO
	override def describe(d: Description) {}

}

class SyncReplicaResult {
	var failedTransfers = 0
	var filesTransferred = 0
	var fileTransferRetries = 0
	var filesAlreadyExist = 0
	var bytesTransferred = 0
	var lastCreatedDateBeforeAnyFailures: DateTime = null

	def alreadyExists() { filesAlreadyExist += 1 }
	def failedTransfer() { failedTransfers += 1 }
	def retryTransfer() { fileTransferRetries += 1 }
	def fileTransferred(bytes: Int) {
		filesTransferred += 1
		bytesTransferred += bytes
	}

	def lastCreated(createdDate: DateTime) {
		if (failedTransfers == 0) lastCreatedDateBeforeAnyFailures = createdDate
	}
}

object SyncReplicaFilesystemCommand {

	val FetchFileRetries = 2

	val LastSyncJobDetailsFilename = "last_sync_job_details"

	val LastSyncedDateFilename = "last_synced_date"

	// 2 hours
	val LastJobOverlapMillis = 2 * 60 * 60 * 1000

	val IdParam = DownloadFileController.IdParam

	val MacParam = DownloadFileController.MacParam

	val StartParam = ListFilesController.StartParam

	val StartFromIdParam = ListFilesController.StartFromIdParam

	val FirstTimeStartDate = new DateTime(LastJobOverlapMillis)

	// 30 seconds
	val HttpTimeout = 30 * 1000

}