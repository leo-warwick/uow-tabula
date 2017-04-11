<#escape x as x?html>

<h1>Add points</h1>

<div class="fix-area add-points-to-schemes">
	<form method="POST">
		<input name="returnTo" value="<@routes.attendance.manageAddPoints command.department command.academicYear />" type="hidden" />

		<#if newPoints == 0>
			<p>Choose which schemes to add points to</p>
		<#else>
			<div class="alert alert-info">
				<strong><@fmt.p newPoints "point" /></strong> added to <strong><@fmt.p changedSchemes "scheme"/></strong>
				<a class="btn" href="<@routes.attendance.manageEditPoints command.department command.academicYear schemesParam />">Edit points</a>
			</div>
		</#if>

		<#if (schemeMaps.weekSchemes?keys?size > 0)>
			<table class="week table table-striped table-condensed table-hover sb-no-wrapper-table-popout">
				<thead>
					<tr>
						<th class="for-check-all" style="width: 20px;"></th>
						<th>Monitoring schemes - term week points</th>
					</tr>
				</thead>
				<tbody>
					<#list schemeMaps.weekSchemes?keys?sort_by("displayName") as scheme>
						<tr>
							<td>
								<input name="schemes" value="${scheme.id}" type="checkbox" <#if mapGet(schemeMaps.weekSchemes, scheme)>checked</#if>/>
							</td>
							<td>
								${scheme.displayName}
								<span class="very-subtle">(<@fmt.p scheme.members.members?size "student" />, <@fmt.p scheme.points?size "point" />)</span>
							</td>
						</tr>
					</#list>
				</tbody>
			</table>
		</#if>

		<#if (schemeMaps.dateSchemes?keys?size > 0)>
			<table class="date table table-striped table-condensed table-hover sb-no-wrapper-table-popout">
				<thead>
				<tr>
					<th class="for-check-all" style="width: 20px;"></th>
					<th>Monitoring schemes - calendar date points</th>
				</tr>
				</thead>
				<tbody>
					<#list schemeMaps.dateSchemes?keys?sort_by("displayName") as scheme>
					<tr>
						<td>
							<input name="schemes" value="${scheme.id}" type="checkbox" <#if mapGet(schemeMaps.dateSchemes, scheme)>checked</#if>/>
						</td>
						<td>
							${scheme.displayName}
							<span class="very-subtle">(<@fmt.p scheme.members.members?size "student" />, <@fmt.p scheme.points?size "point" />)</span>
						</td>
					</tr>
					</#list>
				</tbody>
			</table>
		</#if>

		<p>
			<button type="button" class="btn btn-default add-blank-point" data-href="<@routes.attendance.manageAddPointsBlank command.department command.academicYear/>">Add a point</button>
			<button type="button" class="btn btn-default copy-points" data-href="<@routes.attendance.manageAddPointsCopy command.department command.academicYear/>">Copy points</button>
			<button type="button" class="btn btn-default use-template" data-href="<@routes.attendance.manageAddPointsTemplate command.department command.academicYear/>">Use template</button>

			<span class="alert alert-danger" style="display: none;">You cannot add the same point to schemes which use different date formats</span>
		</p>
	</form>

	<div class="fix-footer form-group">
		<a class="btn btn-default" href="<@routes.attendance.manageHomeForYear command.department command.academicYear />">Done</a>
	</div>
</div>

<script>
	jQuery(function($){
		$('.fix-area').fixHeaderFooter();
	});
</script>
</#escape>