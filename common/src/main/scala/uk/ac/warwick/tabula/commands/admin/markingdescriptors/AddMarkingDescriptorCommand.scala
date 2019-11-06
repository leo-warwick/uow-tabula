package uk.ac.warwick.tabula.commands.admin.markingdescriptors

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Department, DepartmentMarkingDescriptor, MarkingDescriptor}
import uk.ac.warwick.tabula.services.{AutowiringMarkingDescriptorServiceComponent, MarkingDescriptorServiceComponent}

import scala.jdk.CollectionConverters._

object AddMarkingDescriptorCommand {
  def apply(department: Department): Appliable[MarkingDescriptor] with ModifyMarkingDescriptorState =
    new AddMarkingDescriptorCommandInternal(department)
      with ComposableCommand[MarkingDescriptor]
      with AddMarkingDescriptorValidation
      with ModifyMarkingDescriptorState
      with ModifyMarkingDescriptorPermissions
      with AddMarkingDescriptorDescription
      with AutowiringMarkingDescriptorServiceComponent
}

class AddMarkingDescriptorCommandInternal(val department: Department) extends CommandInternal[MarkingDescriptor] {
  self: ModifyMarkingDescriptorState with MarkingDescriptorServiceComponent =>

  override def applyInternal(): MarkingDescriptor = {
    val markingDescriptor = new DepartmentMarkingDescriptor()

    markingDescriptor.department = department
    markingDescriptor.minMarkPoint = sortedMarkPoints.min
    markingDescriptor.maxMarkPoint = sortedMarkPoints.max
    markingDescriptor.text = text

    markingDescriptorService.save(markingDescriptor)

    markingDescriptor
  }
}

trait AddMarkingDescriptorValidation extends ModifyMarkingDescriptorValidation {
  self: ModifyMarkingDescriptorState with MarkingDescriptorServiceComponent =>
  override def markPointsAlreadyExist: Boolean = markPoints.asScala.exists(mp => markingDescriptorService.getDepartmentMarkingDescriptors(department).exists(_.isForMarkPoint(mp)))
}

trait AddMarkingDescriptorDescription extends Describable[MarkingDescriptor] {
  self: ModifyMarkingDescriptorState =>

  override lazy val eventName = "AddMarkingDescriptor"

  override def describe(d: Description): Unit = {
    d.department(department)
      .properties(
        "markPoints" -> markPoints.asScala.map { markPoint =>
          Map(
            "mark" -> markPoint.mark,
            "markClass" -> markPoint.markClass.name,
            "name" -> markPoint.name
          )
        },
        "text" -> text
      )
  }
}

