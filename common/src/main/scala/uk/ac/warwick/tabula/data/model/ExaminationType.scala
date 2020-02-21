package uk.ac.warwick.tabula.data.model

import enumeratum.{Enum, EnumEntry}
import uk.ac.warwick.tabula.system.EnumTwoWayConverter

import scala.collection.immutable

// full list of types taken from here - https://repo.elab.warwick.ac.uk/projects/MAPP/repos/app/browse/app/domain/fields/Assessment.scala#493-520
// TODO - fetch the list via an API call to module approval rather than maintaining it twice
sealed abstract class ExaminationType(override val entryName: String, val name: String, val description: String) extends EnumEntry
object ExaminationType extends Enum[ExaminationType] {
  case object Standard extends ExaminationType("STAN", "Standard", "A standard written exam paper, not seen in advance, with students not permitted to bring any texts.")
  case object SeenPaper extends ExaminationType("SEEN", "Seen paper", "Paper is available to candidates in advance.")
  case object OpenBook extends ExaminationType("OPEN", "Open book", "Students are permitted to bring any materials to the examination.")
  case object Restricted extends ExaminationType("REST", "Restricted", "Students are permitted to bring specific materials to the examination, eg annotated case study, 2 x A4 sides of handwritten notes, 2 x A4 sides of typed notes.")

  // Legacy?
  case object Sectioned extends ExaminationType("S", "Sectioned", "The paper has more than one section, each of which examines a different module.")
  case object NonSectioned extends ExaminationType("NS", "Not sectioned", "The paper examines only one module.")
  case object OpenBookDictionary extends ExaminationType("OD", "Open book - dictionary", "Students are permitted a dictionary in the exam (as specified and approved by their department).")
  case object OpenBookRestricted extends ExaminationType("OR", "Open book restricted", "Students are permitted specific text(s) in the exam (as specified by their department).")
  case object OpenBookUnrestricted extends ExaminationType("OU", "Open book unrestricted", "Students are permitted any text(s) in the exam (excluding University of Warwick Library books).")

  override def values: immutable.IndexedSeq[ExaminationType] = findValues
}

class ExaminationTypeUserType extends EnumUserType(ExaminationType)
class ExaminationTypeConverter extends EnumTwoWayConverter(ExaminationType)
