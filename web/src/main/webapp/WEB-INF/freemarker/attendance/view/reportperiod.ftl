<#escape x as x?html>

<h1>Upload missed monitoring points to SITS:eVision</h1>

<@f.form commandName="command" action="" method="POST">

	<#if command.availablePeriods?size == 0>
		<div class="alert alert-info">
			There are no monitoring periods available for the students you have chosen.
		</div>
	<#else>
		<p>Choose the monitoring period to upload:</p>
		<@bs3form.labelled_form_group path="period" labelText="">

			<#list command.availablePeriods as period>
				<#assign disabledClass><#if !period._2()>disabled</#if></#assign>
				<@bs3form.radio>
					<input id="period_${period_index}" name="period" type="radio" value="${period._1()}" ${disabledClass}>
					<#if disabledClass?has_content>
						<a class="use-tooltip" data-content="All of the chosen students have already reported for this period">
							${period._1()}
						</a>
					<#else>
						${period._1()}
					</#if>
				</@bs3form.radio>
			</#list>
		</@bs3form.labelled_form_group>

		<div class="submit-buttons">
			<div class="pull-right">
				<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Loading&hellip;">
					Next
				</button>
				<a class="btn btn-default" href="<@routes.attendance.viewStudents department academicYear command.serializeFilter />">Cancel</a>
			</div>
		</div>
	</#if>

</@f.form>

</#escape>