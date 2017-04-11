<#escape x as x?html>

<#assign filterQuery = findCommand.serializeFilter />
<#assign returnTo = (info.requestedUri!"")?url />

<h1>Create scheme: ${scheme.displayName}</h1>

<div class="add-points-to-schemes fix-area">

	<p class="progress-arrows">
		<span class="arrow-right use-tooltip" title="Edit properties">Properties</span>
		<span class="arrow-right arrow-left use-tooltip" title="Edit students"><a href="<@routes.attendance.manageAddStudents scheme />">Students</a></span>
		<span class="arrow-right arrow-left active">Points</span>
	</p>

	<#if newPoints == 0>
		<p><@fmt.p scheme.points?size "point" /> on this scheme</p>
	<#else>
		<div class="alert alert-info">
			<strong><@fmt.p newPoints "point" /></strong> added to this scheme
		</div>

		<p><@fmt.p scheme.points?size "point" /> on this scheme</p>
	</#if>

	<form method="POST">
		<input name="schemes" value="${scheme.id}" type="hidden" />
		<input name="returnTo" value="<@routes.attendance.manageNewSchemeAddPoints scheme />" type="hidden" />
		<button type="button" class="btn btn-default add-blank-point" data-href="<@routes.attendance.manageAddPointsBlank findCommand.department findCommand.academicYear/>">Add a point</button>
		<button type="button" class="btn btn-default copy-points" data-href="<@routes.attendance.manageAddPointsCopy findCommand.department findCommand.academicYear/>">Copy points</button>
		<button type="button" class="btn btn-default use-template" data-href="<@routes.attendance.manageAddPointsTemplate findCommand.department findCommand.academicYear/>">Use template</button>
	</form>

	<#include "_findpointsresult.ftl" />

	<div class="fix-footer submit-buttons">
		<a class="btn btn-default" href="<@routes.attendance.manageHomeForYear findCommand.department findCommand.academicYear />">Done</a>
	</div>

</div>
</#escape>