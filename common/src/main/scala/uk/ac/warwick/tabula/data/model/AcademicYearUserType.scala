package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._

/**
 * Stores an AcademicYear as an integer (which is the 4-digit start year)
 */
final class AcademicYearUserType extends ConvertibleIntegerUserType[AcademicYear]