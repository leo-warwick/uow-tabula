<#escape x as x?html>

<h1>Upload missed monitoring points</h1>

<@f.form commandName="command" action="" method="POST">

	<input type="hidden" name="period" value="${command.period}" />
	<input type="hidden" name="filterString" value="${command.filterString}" />
	<#list command.students as student>
		<input type="hidden" name="students" value="${student.universityId}" />
	</#list>

	<#if command.currentPeriod == command.period && command.currentAcademicYear.toString == command.academicYear.toString>
		<div class="alert alert-info">
			<p>You have chosen to upload points for the current monitoring period, which has not yet finished.</p>
		</div>
	</#if>

	<div class="alert alert-info">

		<p>When all missed monitoring points for the current reporting period have been entered and uploaded to SITS,
		your Head of Department should be asked to sign	the Missed Monitoring Report, which can be obtained from the
		<a href="http://warwick.ac.uk/evision/" >SITS:eVision portal</a>.
		On this report, all students who have been reported as having missed one or more monitoring report will be listed.
		Please print this report, obtain the signature of your Head of Department,
		and return it to the Academic Office contact noted on the report screen. For further guidance please consult the guidance notes.</p>

		<p>
			Once these points are uploaded to SITS you won't be able to change this information via Tabula.
			If information does need to be amended later, please contact the Administrative Officer for <a href="http://warwick.ac.uk/studentrecords">Student Records</a>.
		</p>

		<p>Are you sure you wish to upload these missed points?</p>

		<p>
			<@bs3form.checkbox>
				<@f.checkbox path="confirm" /> I confirm that I want to upload these missed points.
			</@bs3form.checkbox>
			<@bs3form.errors path="confirm"/>
		</p>

	</div>

	<div class="submit-buttons">
		<div class="pull-right">
			<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit-confirm" data-loading-text="Loading&hellip;">
				Upload
			</button>
			<a class="btn btn-default" href="<@routes.attendance.viewStudents department academicYear command.filterString />">Cancel</a>
		</div>
	</div>

</@f.form>

</#escape>