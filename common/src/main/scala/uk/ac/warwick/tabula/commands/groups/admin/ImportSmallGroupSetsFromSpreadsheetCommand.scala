package uk.ac.warwick.tabula.commands.groups.admin

import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.groups.admin.ImportSmallGroupSetsFromSpreadsheetCommand._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.groups.EventDeliveryMethod.Hybrid
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.data.model.{AliasedMapLocation, Department, MapLocation, NamedLocation}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.groups.docconversion._
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._

object ImportSmallGroupSetsFromSpreadsheetCommand {
  val RequiredPermission: Permission = Permissions.SmallGroups.ImportFromExternalSystem
  type CommandType = Appliable[Seq[SmallGroupSet]] with SelfValidating with BindListener with ImportSmallGroupSetsFromSpreadsheetRequest with ImportSmallGroupSetsFromSpreadsheetWarnings

  type ModifySetCommand = ModifySmallGroupSetCommand.Command
  type ModifyGroupCommand = ModifySmallGroupCommand.Command
  type DeleteGroupCommand = DeleteSmallGroupCommand.Command
  type ModifyEventCommand = ModifySmallGroupEventCommand.Command
  type DeleteEventCommand = DeleteSmallGroupEventCommand.Command

  def apply(department: Department, academicYear: AcademicYear): CommandType =
    new ImportSmallGroupSetsFromSpreadsheetCommandInternal(department, academicYear)
      with ImportSmallGroupSetsFromSpreadsheetWarnings
      with ComposableCommand[Seq[SmallGroupSet]]
      with ImportSmallGroupSetsFromSpreadsheetPermissions
      with ImportSmallGroupSetsFromSpreadsheetDescription
      with ImportSmallGroupSetsFromSpreadsheetValidation
      with ImportSmallGroupSetsFromSpreadsheetBinding
      with AutowiringSmallGroupSetSpreadsheetHandlerComponent
      with AutowiringSmallGroupServiceComponent
}

abstract class ImportSmallGroupSetsFromSpreadsheetCommandInternal(val department: Department, val academicYear: AcademicYear) extends CommandInternal[Seq[SmallGroupSet]]
  with ImportSmallGroupSetsFromSpreadsheetRequest {

  override def applyInternal(): Seq[SmallGroupSet] = commands.asScala.toSeq.map { sHolder =>
    val set = sHolder.command.apply()

    // don't apply the delete group command if linked - the set command will have deleted the groups
    if (set.allocationMethod != SmallGroupAllocationMethod.Linked) {
      sHolder.deleteGroupCommands.asScala.foreach(_.apply())
    }
    sHolder.modifyGroupCommands.asScala.foreach { gHolder =>
      gHolder.command match {
        case c: CreateSmallGroupCommandInternal => c.set = set
        case _ =>
      }

      // don't apply the group command if linked - the set command will have created the groups
      val group = if (set.allocationMethod != SmallGroupAllocationMethod.Linked) {
        gHolder.command.apply()
      } else {
        set.groups.asScala.find(_.name == gHolder.command.name).orNull
      }

      gHolder.deleteEventCommands.asScala.foreach(_.apply())
      gHolder.modifyEventCommands.asScala.foreach { eHolder =>
        eHolder.command match {
          case c: CreateSmallGroupEventCommandInternal =>
            c.set = set
            c.group = group
          case _ =>
        }

        eHolder.command.apply()
      }
    }

    set
  }

}

trait ImportSmallGroupSetsFromSpreadsheetValidation extends SelfValidating {
  self: ImportSmallGroupSetsFromSpreadsheetRequest =>

  override def validate(errors: Errors): Unit = {
    commands.asScala.zipWithIndex.foreach { case (set, i) =>
      errors.pushNestedPath(s"commands[$i]")
      errors.pushNestedPath("command")
      set.command.validate(errors)
      // need to just pop command from commands[$i].command leaving setCommandHolder -commands[$i]
      errors.popNestedPath()
      set.deleteGroupCommands.asScala.zipWithIndex.foreach { case (command, j) =>
        errors.pushNestedPath(s"deleteGroupCommands[$j]")
        command.validate(errors)
        errors.popNestedPath()
      }

      set.modifyGroupCommands.asScala.zipWithIndex.foreach { case (group, j) =>
        errors.pushNestedPath(s"modifyGroupCommands[$j]")
        errors.pushNestedPath("command")
        group.command.validate(errors)

        // pop command only with remaining structure as  commands[$i].modifyGroupCommands[$j]
        errors.popNestedPath()

        group.deleteEventCommands.asScala.zipWithIndex.foreach { case (command, k) =>
          // commands[$i].modifyGroupCommands[$j].deleteEventCommands[$k]
          errors.pushNestedPath(s"deleteEventCommands[$k]")
          command.validate(errors)
          errors.popNestedPath()
        }

        group.modifyEventCommands.asScala.zipWithIndex.foreach { case (event, k) =>
          // commands[$i].modifyGroupCommands[$j].modifyEventCommands[$k].command
          errors.pushNestedPath(s"modifyEventCommands[$k].command")
          event.command.validate(errors)
          errors.popNestedPath()
        }

        // remove modifyGroupCommands[$j] from commands[$i].modifyGroupCommands[$j]
        errors.popNestedPath()
      }

      // remove commands[$i]
      errors.popNestedPath()
    }
  }

}

trait ImportSmallGroupSetsFromSpreadsheetBinding extends BindListener with Logging {
  self: ImportSmallGroupSetsFromSpreadsheetRequest
    with SmallGroupSetSpreadsheetHandlerComponent
    with SmallGroupServiceComponent =>

  override def onBind(result: BindingResult): Unit = {
    if (file.isMissing) {
      result.rejectValue("file", "NotEmpty")
    } else {
      val fileNames = file.fileNames map (_.toLowerCase)
      val invalidFiles = fileNames.filter(s => !SmallGroupSetSpreadsheetHandler.AcceptedFileExtensions.exists(s.endsWith))

      if (invalidFiles.nonEmpty) {
        if (invalidFiles.size == 1) result.rejectValue("file", "file.wrongtype.one", Array(invalidFiles.mkString(""), SmallGroupSetSpreadsheetHandler.AcceptedFileExtensions.mkString(", ")), "")
        else result.rejectValue("file", "file.wrongtype", Array(invalidFiles.mkString(", "), SmallGroupSetSpreadsheetHandler.AcceptedFileExtensions.mkString(", ")), "")
      }
    }

    if (!result.hasErrors) {
      transactional() {
        result.pushNestedPath("file")
        file.onBind(result)
        result.popNestedPath()

        commands = file.attached.asScala.filter(_.hasData).flatMap { attachment =>
          val extractedSets = smallGroupSetSpreadsheetHandler.readXSSFExcelFile(department, academicYear, attachment.asByteSource.openStream(), result)

          // Convert to commands
          extractedSets.map { extracted =>
            val existing =
              smallGroupService.getSmallGroupSets(extracted.module, academicYear)
                .find { s => s.name.equalsIgnoreCase(extracted.name) }

            val (setCommand, setCommandType) = existing match {
              case Some(set) => (ModifySmallGroupSetCommand.edit(set.module, set), "Edit")
              case _ => (ModifySmallGroupSetCommand.create(extracted.module), "Create")
            }

            setCommand.name = extracted.name
            setCommand.format = extracted.format
            setCommand.allocationMethod = extracted.allocationMethod
            setCommand.allowSelfGroupSwitching = extracted.studentsCanSwitchGroup
            setCommand.studentsCanSeeTutorName = extracted.studentsSeeTutor
            setCommand.studentsCanSeeOtherMembers = extracted.studentsSeeStudents
            setCommand.collectAttendance = extracted.collectAttendance
            setCommand.linkedDepartmentSmallGroupSet = extracted.linkedSmallGroupSet.orNull
            setCommand.academicYear = academicYear

            def matchesGroup(extractedGroup: ExtractedSmallGroup)(smallGroup: SmallGroup) =
              smallGroup.name.equalsIgnoreCase(extractedGroup.name)

            def matchesEvent(extractedEvent: ExtractedSmallGroupEvent)(smallGroupEvent: SmallGroupEvent) =
              (extractedEvent.title.nonEmpty && extractedEvent.title.contains(smallGroupEvent.title)) ||
                (extractedEvent.weekRanges == smallGroupEvent.weekRanges && extractedEvent.dayOfWeek == Option(smallGroupEvent.day) && extractedEvent.startTime == Option(smallGroupEvent.startTime))

            val groupCommands = extracted.groups.map { extractedGroup =>
              val existingGroup = existing.toSeq.flatMap(_.groups.asScala).find(matchesGroup(extractedGroup))

              val (groupCommand, groupCommandType) = existingGroup match {
                case Some(group) => (ModifySmallGroupCommand.edit(group.groupSet.module, group.groupSet, group), "Edit")
                case _ => (ModifySmallGroupCommand.create(extracted.module, new SmallGroupSet {
                  name = extracted.name // Could be needed if validation fails
                }), "Create")
              }

              groupCommand.name = extractedGroup.name
              groupCommand.maxGroupSize = JInteger(extractedGroup.limit)

              def duplicate(a: ExtractedSmallGroupEvent, b: ExtractedSmallGroupEvent): Boolean =
                a != b && (
                  (a.title.nonEmpty && a.title == b.title) ||
                    (a.weekRanges == b.weekRanges && a.dayOfWeek == b.dayOfWeek && a.startTime == b.startTime)
                  )

              groupCommand.hasDuplicateEvents =
                extractedGroup.events.exists(a => extractedGroup.events.exists(b => duplicate(a, b)))

              val eventCommands = extractedGroup.events.map { extractedEvent =>
                val existingEvent = existingGroup.toSeq.flatMap(_.events).find(matchesEvent(extractedEvent))

                val (eventCommand, eventCommandType) = existingEvent match {
                  case Some(event) => (ModifySmallGroupEventCommand.edit(event.group.groupSet.module, event.group.groupSet, event.group, event, isImport = true), "Edit")
                  case _ => (ModifySmallGroupEventCommand.create(extracted.module, new SmallGroupSet, new SmallGroup, isImport = true), "Create")
                }

                eventCommand.title = extractedEvent.title.orNull
                eventCommand.tutors = extractedEvent.tutors.map(_.getUserId).asJava
                eventCommand.weekRanges = extractedEvent.weekRanges
                eventCommand.day = extractedEvent.dayOfWeek.orNull
                eventCommand.startTime = extractedEvent.startTime.orNull
                eventCommand.endTime = extractedEvent.endTime.orNull

                extractedEvent.location.foreach {
                  case NamedLocation(name) if locationMappings.asScala.get(name).exists(_.hasText) =>
                    eventCommand.locationAlias = null
                    eventCommand.location = name
                    eventCommand.locationId = locationMappings.asScala(name)
                  case NamedLocation(name) =>
                    eventCommand.locationAlias = null
                    eventCommand.location = name
                    eventCommand.locationId = null

                    if (extractedEvent.possibleMapLocations.nonEmpty) {
                      locationMappings.putIfAbsent(name, null)
                    }
                  case MapLocation(name, lid, _) =>
                    eventCommand.locationAlias = null
                    eventCommand.location = name
                    eventCommand.locationId = lid
                  case AliasedMapLocation(displayName, MapLocation(name, lid, _)) =>
                    eventCommand.locationAlias = displayName
                    eventCommand.location = name
                    eventCommand.locationId = lid
                }
                eventCommand.possibleMapLocations = extractedEvent.possibleMapLocations
                eventCommand.relatedUrl = extractedEvent.relatedUrl.orNull
                eventCommand.relatedUrlTitle = extractedEvent.relatedUrlTitle.orNull

                eventCommand.deliveryMethod = extractedEvent.deliveryMethod.getOrElse(Hybrid)
                eventCommand.onlineDeliveryUrl = extractedEvent.onlineDeliveryUrl.orNull
                eventCommand.onlinePlatform = extractedEvent.onlinePlatform.orNull

                new ModifySmallGroupEventCommandHolder(eventCommand, eventCommandType)
              }

              val deleteEventCommands =
                existingGroup.toSeq.flatMap(_.events.sorted)
                  .filterNot { e => extractedGroup.events.exists(matchesEvent(_)(e)) }
                  .map { e =>
                    logger.info(s"DeleteSmallGroupEventCommand event details- ${e.id} / ${e.group.id} ")
                    DeleteSmallGroupEventCommand(e.group, e)
                  }

              new ModifySmallGroupCommandHolder(groupCommand, groupCommandType, eventCommands.asJava, deleteEventCommands.asJava)
            }

            // no need for extra delete group commands if we are using linked groups. These are deleted by the set command
            val deleteGroupCommands = existing.toSeq.flatMap(_.groups.asScala.sorted)
              .filterNot { g => extracted.groups.exists(matchesGroup(_)(g)) }
              .map { g => DeleteSmallGroupCommand(g.groupSet, g, isSpreadsheetUpload = true) }


            new ModifySmallGroupSetCommandHolder(setCommand, setCommandType, groupCommands.asJava, deleteGroupCommands.asJava)
          }

        }.asJava
      }
    }
  }
}

class ModifySmallGroupSetCommandHolder(var command: ModifySetCommand, val commandType: String, var modifyGroupCommands: JList[ModifySmallGroupCommandHolder], var deleteGroupCommands: JList[DeleteGroupCommand])

class ModifySmallGroupCommandHolder(var command: ModifyGroupCommand, val commandType: String, var modifyEventCommands: JList[ModifySmallGroupEventCommandHolder], var deleteEventCommands: JList[DeleteEventCommand])

class ModifySmallGroupEventCommandHolder(var command: ModifyEventCommand, val commandType: String)

trait ImportSmallGroupSetsFromSpreadsheetRequest extends ImportSmallGroupSetsFromSpreadsheetState {
  var file: UploadedFile = new UploadedFile
  var locationMappings: JMap[String, String] = JHashMap() // A map of names to location IDs
  var confirm: Boolean = _

  // Bound by BindListener (ImportSmallGroupSetsFromSpreadsheetBinding)
  var commands: JList[ModifySmallGroupSetCommandHolder] = JArrayList()
}

trait ImportSmallGroupSetsFromSpreadsheetState {
  def department: Department

  def academicYear: AcademicYear
}

trait ImportSmallGroupSetsFromSpreadsheetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ImportSmallGroupSetsFromSpreadsheetState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = p.PermissionCheck(RequiredPermission, mandatory(department))
}

trait ImportSmallGroupSetsFromSpreadsheetDescription extends Describable[Seq[SmallGroupSet]] {
  self: ImportSmallGroupSetsFromSpreadsheetState =>

  override def describe(d: Description): Unit =
    d.department(department)
}

trait ImportSmallGroupSetsFromSpreadsheetWarnings {
  self: ImportSmallGroupSetsFromSpreadsheetRequest =>
  def warnings(): Seq[String] = {
    commands.asScala.toSeq
      .flatMap { setHolder =>
        val set = setHolder.command
        setHolder.modifyGroupCommands.asScala.flatMap { groupHolder =>
          val group = groupHolder.command
          val groupDescription = s"""${set.module.code.toUpperCase} ${set.format.plural}: "${set.name}", Group "${group.name}""""

          groupHolder.modifyEventCommands.asScala.map(_.command)
            .filterNot(_.locationId.hasText).filter(_.location.hasText)
            .map { event =>
              val maybeTitle = event.title.maybeText.map(t => s"""event "$t"""").getOrElse("untitled event")

              val reason =
                if (event.possibleMapLocations.isEmpty) "was not found on the campus map"
                else s"matched multiple locations on the campus map - ${event.possibleMapLocations.map { location => s"${location.name} (${location.building}, ${location.floor})" }.mkString(", ")}"

              s"""$groupDescription, $maybeTitle: the location "${event.location}" $reason"""
            }
        }
      }
  }
}
