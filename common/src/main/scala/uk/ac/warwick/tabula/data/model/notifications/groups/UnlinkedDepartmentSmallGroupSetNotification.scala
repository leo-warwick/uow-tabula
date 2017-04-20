package uk.ac.warwick.tabula.data.model.notifications.groups

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.data.model.{Department, FreemarkerModel, MyWarwickActivity, NotificationWithTarget}
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import scala.collection.mutable

@Entity
@DiscriminatorValue(value="UnlinkedDepartmentSmallGroupSet")
class UnlinkedDepartmentSmallGroupSetNotification extends NotificationWithTarget[DepartmentSmallGroupSet, Department]
	with MyWarwickActivity {

	@transient
	lazy val department: Department = target.entity
	@transient
	lazy val sets: mutable.Buffer[DepartmentSmallGroupSet] = items.asScala.map(_.entity)
	@transient
	lazy val academicYear: AcademicYear = sets.head.academicYear

	@transient
	var moduleAndDepartmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]
	@transient
	var topLevelUrl: String = Wire.property("${toplevel.url}")

	@transient
	override def verb: String = "view"

	override def urlTitle: String = "view the reusable small group sets in your department"

	@transient
	override def url: String = Routes.admin.reusable.apply(target.entity, items.get(0).entity.academicYear)

	@transient
	override def title: String = "%s: %d reusable small group set%s have been unlinked from SITS".format(
		department.name,
		sets.size,
		if (sets.size != 1) "s" else ""
	)

	@transient
	override def content: FreemarkerModel = FreemarkerModel("/WEB-INF/freemarker/notifications/groups/department_small_group_sets_unlinked.ftl", Map(
		"department" -> department,
		"academicYear" -> academicYear,
		"sets" -> sets,
		"setLinks" -> sets.sortBy(_.name).map(set => s"${set.name}: $topLevelUrl${Routes.admin.reusable.editAddStudents(set)}")
	))

	@transient
	override def recipients: Seq[User] =
		// department.owners is not populated correctly if department not fetched directly
		moduleAndDepartmentService.getDepartmentById(department.id).get.owners.users
}
