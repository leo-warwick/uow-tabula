package uk.ac.warwick.tabula.data.model.groups

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.services.TermService

import scala.collection.JavaConverters._

sealed trait SmallGroupSetFilter {
	def description: String
	val getName: String = SmallGroupSetFilters.shortName(getClass.asInstanceOf[Class[_ <: SmallGroupSetFilter]])
	def apply(set: SmallGroupSet): Boolean
}

object SmallGroupSetFilters {
	private val ObjectClassPrefix = SmallGroupSetFilters.getClass.getName

	def shortName(clazz: Class[_ <: SmallGroupSetFilter]): String
	= clazz.getName.substring(ObjectClassPrefix.length, clazz.getName.length - 1).replace('$', '.')

	def of(name: String): SmallGroupSetFilter = {
		try {
			// Go through the magical hierarchy
			val clz = Class.forName(ObjectClassPrefix + name.replace('.', '$') + "$")
			clz.getDeclaredField("MODULE$").get(null).asInstanceOf[SmallGroupSetFilter]
		} catch {
			case e: ClassNotFoundException => throw new IllegalArgumentException("SmallGroupSetFilter " + name + " not recognised")
			case e: ClassCastException => throw new IllegalArgumentException("SmallGroupSetFilter " + name + " is not an endpoint of the hierarchy")
		}
	}

	case class Module(module: model.Module) extends SmallGroupSetFilter {
		val description: String = module.code.toUpperCase() + " " + module.name
		override val getName: String = "Module(" + module.code + ")"
		def apply(set: SmallGroupSet): Boolean = set.module == module
	}

	def allModuleFilters(modules: Seq[model.Module]): Seq[Module] = modules.map { Module }

	case class Format(format: SmallGroupFormat) extends SmallGroupSetFilter {
		val description: String = format.description
		override val getName: String = format.code
		def apply(set: SmallGroupSet): Boolean = set.format == format
	}

	def allFormatFilters: Seq[Format] = SmallGroupFormat.members.map { Format }

	object Status {
		case object NeedsGroupsCreating extends SmallGroupSetFilter {
			val description = "Needs groups creating"
			def apply(set: SmallGroupSet): Boolean = set.groups.asScala.isEmpty
		}
		case object UnallocatedStudents extends SmallGroupSetFilter {
			val description = "Contains unallocated students"
			def apply(set: SmallGroupSet): Boolean = set.unallocatedStudentsCount > 0
		}
		case object NeedsEventsCreating extends SmallGroupSetFilter {
			val description = "Needs events creating"
			def apply(set: SmallGroupSet): Boolean = set.groups.asScala.forall { _.events.isEmpty }
		}
		case object OpenForSignUp extends SmallGroupSetFilter {
			val description = "Open for sign up"
			def apply(set: SmallGroupSet): Boolean = set.openForSignups
		}
		case object ClosedForSignUp extends SmallGroupSetFilter {
			val description = "Closed for sign up"
			def apply(set: SmallGroupSet): Boolean = !set.openForSignups
		}
		case object NeedsNotificationsSending extends SmallGroupSetFilter {
			val description = "Needs notifications sending"
			def apply(set: SmallGroupSet): Boolean = !set.fullyReleased
		}
		case object Completed extends SmallGroupSetFilter {
			val description = "Complete"
			def apply(set: SmallGroupSet): Boolean = set.fullyReleased
		}

		val all = Seq(NeedsGroupsCreating, UnallocatedStudents, NeedsEventsCreating, OpenForSignUp, ClosedForSignUp, NeedsNotificationsSending, Completed)
	}

	object AllocationMethod {
		case object ManuallyAllocated extends SmallGroupSetFilter {
			val description = "Manually allocated"
			def apply(set: SmallGroupSet): Boolean = set.allocationMethod == SmallGroupAllocationMethod.Manual
		}
		case object StudentSignUp extends SmallGroupSetFilter {
			val description = "Self sign-up"
			def apply(set: SmallGroupSet): Boolean = set.allocationMethod == SmallGroupAllocationMethod.StudentSignUp
		}
		case class Linked(linked: DepartmentSmallGroupSet) extends SmallGroupSetFilter {
			val description: String = linked.name
			override val getName: String = "AllocationMethod.Linked(" + linked.id + ")"
			def apply(set: SmallGroupSet): Boolean = set.linked && set.linkedDepartmentSmallGroupSet == linked
		}

		def all(linked: Seq[DepartmentSmallGroupSet]): Seq[SmallGroupSetFilter with Product with Serializable] = Seq(ManuallyAllocated, StudentSignUp) ++ linked.map { Linked }
	}

	case class Term(termName: String, weekRange: WeekRange) extends SmallGroupSetFilter {
		val description: String = termName
		override val getName = s"Term($termName, ${weekRange.minWeek}, ${weekRange.maxWeek})"
		def apply(set: SmallGroupSet): Boolean =
			set.groups.asScala
				.flatMap { _.events }
				.flatMap { _.weekRanges }
				.flatMap { _.toWeeks }
				.exists(weekRange.toWeeks.contains)
	}
	def allTermFilters(year: AcademicYear, termService: TermService): Seq[Term] = {
		val weeks = termService.getAcademicWeeksForYear(year.dateInTermOne).toMap

		val terms =
			weeks
				.map { case (weekNumber, dates) =>
					(weekNumber, termService.getTermFromAcademicWeekIncludingVacations(weekNumber, year))
				}
				.groupBy { _._2 }
				.map { case (term, weekNumbersAndTerms) =>
					(term, WeekRange(weekNumbersAndTerms.keys.min, weekNumbersAndTerms.keys.max))
				}
				.toSeq
				.sortBy { case (_, weekRange) => weekRange.minWeek }

		TermService.orderedTermNames.zip(terms).map { case (name, (term, weekRange)) => Term(name, weekRange) }
	}
}
