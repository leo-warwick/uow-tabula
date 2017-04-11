package uk.ac.warwick.tabula.data.model

import javax.persistence.Entity
import javax.validation.constraints.NotNull

object GradeBoundary {
	def apply(marksCode: String, grade: String, minimumMark: Int, maximumMark: Int, signalStatus: String): GradeBoundary = {
		val gb = new GradeBoundary()
		gb.grade = grade
		gb.marksCode = marksCode
		gb.minimumMark = minimumMark
		gb.maximumMark = maximumMark
		gb.signalStatus = signalStatus
		gb
	}

	private val byGradeOrdering = Ordering.by[GradeBoundary, String]( _.grade )

	implicit val defaultOrdering = new Ordering[GradeBoundary]() {
		override def compare(x: GradeBoundary, y: GradeBoundary): Int = {
			if (x.isDefault && y.isDefault) byGradeOrdering.compare(x,y)
			else if (x.isDefault) -1
			else if (y.isDefault) 1
			else byGradeOrdering.compare(x,y)
		}
	}
}

@Entity
class GradeBoundary extends GeneratedId {

	@NotNull
	var marksCode: String = _

	@NotNull
	var grade: String = _

	@NotNull
	var minimumMark: Int = 0

	@NotNull
	var maximumMark: Int = 100

	@NotNull
	var signalStatus: String = _

	def isDefault: Boolean = signalStatus == "N"

}
