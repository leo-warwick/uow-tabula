package uk.ac.warwick.tabula.commands.attendance.manage

trait SetsFindPointsResultOnCommandState {

	self: FindPointsResultCommandState =>

	def setFindPointsResult(result: FindPointsResult) {
		findPointsResult = result
	}

}

trait FindPointsResultCommandState {
	var findPointsResult: FindPointsResult = _
}