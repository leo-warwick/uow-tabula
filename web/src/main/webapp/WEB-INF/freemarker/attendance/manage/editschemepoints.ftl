<#escape x as x?html>
<#import "../attendance_macros.ftl" as attendance_macros />

<#assign filterQuery = findCommand.serializeFilter />
<#assign returnTo><@routes.attendance.manageEditSchemePoints scheme.department scheme.academicYear scheme /></#assign>

<h1>Edit scheme: ${scheme.displayName}</h1>

<div class="add-points-to-schemes fix-area">

	<p class="progress-arrows">
		<span class="arrow-right use-tooltip" title="Edit properties"><a href="<@routes.attendance.manageEditScheme findCommand.department findCommand.academicYear scheme />">Properties</a></span>
		<span class="arrow-right arrow-left use-tooltip" title="Edit students"><a href="<@routes.attendance.manageEditSchemeStudents findCommand.department findCommand.academicYear scheme />">Students</a></span>
		<span class="arrow-right arrow-left active">Points</span>
	</p>

	<#if newPoints == 0>
		<p><@fmt.p scheme.points?size "point" /> on this scheme</p>
	<#else>
		<div class="alert alert-info">
			<strong><@fmt.p newPoints "point" /></strong> ${actionCompleted!"added to this scheme"}
			<a class="btn btn-default" href="<@routes.attendance.manageEditPoints findCommand.department findCommand.academicYear schemesParam />">Edit points</a>
		</div>

		<p><@fmt.p scheme.points?size "point" /> on this scheme</p>
	</#if>

	<form method="POST">
		<input name="schemes" value="${scheme.id}" type="hidden" />
		<input name="returnTo" value="<@routes.attendance.manageEditSchemePoints findCommand.department findCommand.academicYear scheme />" type="hidden" />
		<button type="button" class="btn btn-default add-blank-point" data-href="<@routes.attendance.manageAddPointsBlank findCommand.department findCommand.academicYear />">Add a point</button>
		<button type="button" class="btn btn-default copy-points" data-href="<@routes.attendance.manageAddPointsCopy findCommand.department findCommand.academicYear/>">Copy points</button>
		<button type="button" class="btn btn-default use-template" data-href="<@routes.attendance.manageAddPointsTemplate findCommand.department findCommand.academicYear/>">Use template</button>
	</form>

	<#include "_findpointsresult.ftl" />

	<div class="fix-footer submit-buttons">
		<a class="btn btn-default" href="<@routes.attendance.manageHomeForYear findCommand.department findCommand.academicYear />">Done</a>
	</div>

</div>

<script>
	jQuery(function($) {
		$('.fix-area').fixHeaderFooter();
	});
</script>

</#escape>