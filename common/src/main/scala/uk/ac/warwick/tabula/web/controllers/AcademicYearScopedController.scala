package uk.ac.warwick.tabula.web.controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.tabula.services.{MaintenanceModeServiceComponent, UserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.{BreadCrumb, Breadcrumbs}
import uk.ac.warwick.tabula.{AcademicYear, Features}

import scala.collection.mutable.ArrayBuffer

trait AcademicYearScopedController {

	self: BaseController with UserSettingsServiceComponent with MaintenanceModeServiceComponent =>

	@Autowired var features: Features  = _

	@ModelAttribute("availableAcademicYears")
	def availableAcademicYears: Seq[AcademicYear] = {
		val years = ArrayBuffer[AcademicYear]()
		if (features.academicYear2012) years += AcademicYear(2012)
		if (features.academicYear2013) years += AcademicYear(2013)
		if (features.academicYear2014) years += AcademicYear(2014)
		if (features.academicYear2015) years += AcademicYear(2015)
		if (features.academicYear2016) years += AcademicYear(2016)
		if (features.academicYear2017) years += AcademicYear(2017)
		if (features.academicYear2018) years += AcademicYear(2018)
		years
	}

	protected def retrieveActiveAcademicYear(academicYearOption: Option[AcademicYear]): Option[AcademicYear] = {
		academicYearOption match {
			case Some(academicYear) if maintenanceModeService.enabled =>
				// Don't store if maintenance mode is enabled
				Some(academicYear)
			case Some(academicYear) if user.apparentUser.isFoundUser =>
				// Store the new active department and return it
				val settings = new UserSettings(user.apparentId)
				settings.activeAcademicYear = academicYear
				transactional() {
					userSettingsService.save(user, settings)
				}
				Some(academicYear)
			case Some(academicYear) => Some(academicYear) // just return the academic year if there is no user to save to
			case _ =>
				userSettingsService.getByUserId(user.apparentId).flatMap(_.activeAcademicYear)
		}
	}

	/**
	 * One of these should be overriden to just call retrieveActiveAcademicYear,
	 * but with the PathVariable-provided academic year as an argument (or null),
	 * and annotated with @ModelAttribute("activeAcademicYear").
	 */
	def activeAcademicYear(academicYear: AcademicYear): Option[AcademicYear] = { None }
	def activeAcademicYear: Option[AcademicYear] = { None }

	def academicYearBreadcrumbs(activeAcademicYear: AcademicYear)(urlGenerator: (AcademicYear) => String): Seq[BreadCrumb] = {
		availableAcademicYears.map(year =>
			Breadcrumbs.Standard(year.getLabel, Some(urlGenerator(year)), "").setActive(year.startYear == activeAcademicYear.startYear)
		)
	}

}
