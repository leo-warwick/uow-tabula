package uk.ac.warwick.tabula.helpers.scheduling

import org.springframework.beans.BeanWrapper
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.scheduling.imports.ImportMemberHelpers._
import uk.ac.warwick.tabula.data.model.{Course, Department, Route, SitsStatus}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.services.scheduling.SitsStatusImporter
import uk.ac.warwick.tabula.JavaImports._

trait PropertyCopying extends Logging {
	var sitsStatusImporter: SitsStatusImporter = Wire[SitsStatusImporter]
	var moduleAndDepartmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]

	/* Basic properties are those that use primitive types + String + DateTime etc, so can be updated with a simple equality check and setter */
	 def copyBasicProperties(properties: Set[String], commandBean: BeanWrapper, destinationBean: BeanWrapper): Boolean = {
		// Transform the set of properties to a set of booleans saying whether the value has changed
		// It's okay to yield a Set here because we only care if it has any values that are true, no point in storing 300 trues
		val changedProperties = for (property <- properties) yield {
			val oldValue = destinationBean.getPropertyValue(property)
			val newValue = commandBean.getPropertyValue(property)

			// null == null in Scala so this is safe for unset values
			if (oldValue != newValue) {
				logger.debug(s"Detected property change for $property: $oldValue -> $newValue; setting value")

				newValue match {
					case Some(value) => destinationBean.setPropertyValue(property, value)
					case None	=> destinationBean.setPropertyValue(property, null)
					case _ => destinationBean.setPropertyValue(property, newValue)
				}
				true
			} else false
		}

		// Fold the set of booleans left with an || of false; this uses foldLeft rather than reduceLeft to handle the empty set
		changedProperties.foldLeft(false)(_ || _)
	}

	// returns true if there is a change, false if no change
	def copyObjectProperty(property: String, code: String, memberBean: BeanWrapper, optionObj: Option[Object]): Boolean = {

		val oldValue = memberBean.getPropertyValue(property)

		optionObj match {
			case None =>
				if (oldValue == null) false // null before and still null - no change
				else {
					memberBean.setPropertyValue(property, null)  // change from non-null to null
					true
				}
			case Some(obj) =>
				if (oldValue == null) { // changed from null to non-null
					memberBean.setPropertyValue(property, obj)
					true
				}
				else {
					val oldCode = oldValue match {
						case route: Route => route.code
						case course: Course => course.code
						case dept: Department => dept.code
						case sitsStatus: SitsStatus => sitsStatus.code
						case _ => null
					}
					if (oldCode == code.toLowerCase) { // same non-null value
						false
					}
					else { // different non-null value
						memberBean.setPropertyValue(property, obj)
						true
					}
				}
		}
	}

	def markAsSeenInSits(bean: BeanWrapper): Boolean = {
		val propertyName = "missingFromImportSince"
		if (bean.getPropertyValue(propertyName) == null)
			false
		else {
			bean.setPropertyValue(propertyName, null)
			true
		}
	}

	def toSitsStatus(code: String): Option[SitsStatus] = {
		if (code == null || code == "") {
			None
		} else {
			sitsStatusImporter.getSitsStatusForCode(code)
		}
	}

	def toDepartment(departmentCode: String): Option[Department] = {
		if (departmentCode == null || departmentCode == "") {
			None
		} else {
			moduleAndDepartmentService.getDepartmentByCode(departmentCode)
		}
	}

	def copyAcademicYear(property: String, acYearString: String, memberBean: BeanWrapper): Boolean = {
		val oldValue = memberBean.getPropertyValue(property) match {
			case value: AcademicYear => value
			case _ => null
		}

		val newValue = toAcademicYear(acYearString)

		if (oldValue == null && acYearString == null) {
			false
		}	else if (oldValue == null) {
			// From no academic year to having an academic year
			memberBean.setPropertyValue(property, newValue)
			true
		} else if (acYearString == null) {
			// Record had an academic year but now doesn't
			memberBean.setPropertyValue(property, null)
			true
		} else if (oldValue == newValue) {
			false
		} else {
			memberBean.setPropertyValue(property, newValue)
			true
		}
	}

	def copyBigDecimal(destinationBean: BeanWrapper, property: String, optionalBigDecimal: Option[JBigDecimal]): Boolean = {
		val oldValue = destinationBean.getPropertyValue(property)

		optionalBigDecimal match {
			case Some(newValue: JBigDecimal) =>
				if (oldValue != newValue) {
					destinationBean.setPropertyValue(property, newValue)
					true
				}
				else false
			case None =>
				if (oldValue != null) {
					destinationBean.setPropertyValue(property, null)
					true
				}
				else false
		}
	}
}
