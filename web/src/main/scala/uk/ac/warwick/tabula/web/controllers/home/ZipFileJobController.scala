package uk.ac.warwick.tabula.web.controllers.home

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.jobs.zips.ZipFileJob
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.ZipCreator
import uk.ac.warwick.tabula.services.fileserver.FileServer
import uk.ac.warwick.tabula.services.jobs.{AutowiringJobServiceComponent, JobInstance}
import uk.ac.warwick.tabula.services.objectstore.AutowiringObjectStorageServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.{ItemNotFoundException, PermissionDeniedException}

@Controller
@RequestMapping(Array("/zips/{jobId}"))
class ZipFileJobController extends BaseController with AutowiringJobServiceComponent with AutowiringObjectStorageServiceComponent {

	var fileServer: FileServer = Wire[FileServer]

	private def jobAndInstance(jobId: String) = {
		jobService.getInstance(jobId) match {
			case Some(jobInstance: JobInstance) => jobService.findJob(jobInstance.jobType) match {
				case Some(zipJob: ZipFileJob) =>
					if (jobInstance.user.apparentUser == user.apparentUser)
						(zipJob, jobInstance)
					else
						throw new PermissionDeniedException(user, Permissions.DownloadZipFromJob, null)
				case _ => throw new ItemNotFoundException()
			}
			case _ => throw new ItemNotFoundException()
		}
	}

	@RequestMapping
	def home(@PathVariable jobId: String): Mav = {
		val (job, jobInstance) = jobAndInstance(jobId)
		if (ajax)
			Mav(new JSONView(Map(
				"progress" -> jobInstance.progress.toString,
				"status" -> jobInstance.status,
				"succeeded" -> jobInstance.succeeded
			)))
		else
			Mav("home/zips",
				"zipType" -> job.itemDescription,
				"returnTo" -> getReturnTo("/"),
				"progress" -> jobInstance.progress.toString,
				"status" -> jobInstance.status,
				"succeeded" -> jobInstance.succeeded
			)
	}

	@RequestMapping(Array("/zip"))
	def serveZip(@PathVariable jobId: String)(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
		val (job, jobInstance) = jobAndInstance(jobId)
		val objectStoreKey = ZipCreator.objectKey(jobInstance.getString(ZipFileJob.ZipFilePathKey))

		objectStorageService.renderable(objectStoreKey, Some(job.zipFileName)) match {
			case Some(f) => fileServer.serve(f, Some(job.zipFileName))
			case _ => throw new ItemNotFoundException()
		}
	}

}
