package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupFormat, SmallGroupSetFilter, SmallGroupSetFilters, WeekRange}
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, SmallGroupService}
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.util.Try

class SmallGroupSetFilterConverter extends TwoWayConverter[String, SmallGroupSetFilter] {

	@Autowired var moduleService: ModuleAndDepartmentService = _
	@Autowired var smallGroupService: SmallGroupService = _

	override def convertRight(source: String): SmallGroupSetFilter = source match {
		case r"Module\(([^\)]+)${moduleCode}\)" =>
			SmallGroupSetFilters.Module(moduleService.getModuleByCode(sanitise(moduleCode)).getOrElse { moduleService.getModuleById(moduleCode).orNull })
		case r"AllocationMethod\.Linked\(([^\)]+)${id}\)" =>
			SmallGroupSetFilters.AllocationMethod.Linked(smallGroupService.getDepartmentSmallGroupSetById(id).getOrElse { throw new IllegalArgumentException })
		case r"Term\(([^\)]+)${name}, ([0-9]+)${minWeek}, ([0-9]+)${maxWeek}\)" =>
			SmallGroupSetFilters.Term(name, WeekRange(minWeek.toInt, maxWeek.toInt))
		case _ =>
			Try(SmallGroupFormat.fromCode(source)).map(SmallGroupSetFilters.Format).getOrElse(
				SmallGroupSetFilters.of(source)
			)

	}

	override def convertLeft(source: SmallGroupSetFilter): String = Option(source).map { _.getName }.orNull

	def sanitise(code: String): String = {
		if (code == null) throw new IllegalArgumentException
		else code.toLowerCase
	}

}
