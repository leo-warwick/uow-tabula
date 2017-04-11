<#escape x as x?html>
<#import "../attendance_macros.ftl" as attendance_macros />

<h1>Delete scheme</h1>

<@f.form id="deleteScheme" method="POST" commandName="command" class="form-horizontal">

	<p>Are you sure you want to delete scheme: ${command.scheme.displayName}?</p>

	<#if command.scheme.points?size == 0>
		<p>There are no points</p>
	<#else>
		<p>There are <@fmt.p command.scheme.points?size "point" /> associated with this scheme.</p>
	</#if>

	<@f.errors cssClass="error form-errors" />

	<p>
		<input
			type="submit"
			class="btn btn-primary<#if command.scheme.hasRecordedCheckpoints> use-tooltip disabled</#if>"
			name="submit"
			value="Delete"
			<#if command.scheme.hasRecordedCheckpoints>
				title="This scheme cannot be removed as it has attendance marks against some of its points."
			</#if>
		/>
		<a class="btn" href="<@routes.attendance.manageHomeForYear command.scheme.department command.scheme.academicYear />">Cancel</a>
	</p>

</@f.form>

</#escape>