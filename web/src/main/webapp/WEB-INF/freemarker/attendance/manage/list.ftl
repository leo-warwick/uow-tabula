<#import "../attendance_macros.ftl" as attendance_macros />
<#escape x as x?html>

<#function route_function dept>
	<#local result><@routes.attendance.manageHomeForYear dept academicYear /></#local>
	<#return result />
</#function>
<@fmt.id7_deptheader title="Manage monitoring points for ${academicYear.toString}" route_function=route_function preposition="in" />

<#if schemes?size == 0>

	<p class="muted">There are no monitoring schemes for ${academicYear.toString} in your department</p>

	<p>
		<a class="btn btn-primary" href="<@routes.attendance.manageNewScheme command.department academicYear />">Create scheme</a>
	</p>

<#else>

	<h2 style="display: inline-block;">Schemes</h2>
	<span class="hint">There <@fmt.p number=schemes?size singular="is" plural="are" shownumber=false/> <@fmt.p schemes?size "monitoring scheme" /> in your department</span>

	<p>
		<a class="btn btn-default" href="<@routes.attendance.manageNewScheme command.department academicYear />">Create scheme</a>
		<a class="btn btn-default" href="<@routes.attendance.manageAddPoints command.department academicYear />">Add points</a>
		<#if havePoints>
			<a class="btn btn-default" href="<@routes.attendance.manageEditPoints command.department academicYear />">Edit points</a>
		<#else>
			<a class="btn btn-default disabled">Edit points</a>
		</#if>
	</p>

<div class="striped-section no-title expanded">
	<div class="striped-section-contents">
		<#list schemes?sort_by("displayName") as scheme>
			<div class="row item-info">
				<div class="col-md-9">
					<div class="pull-right">
						<a class="btn btn-primary btn-sm" href="<@routes.attendance.manageEditScheme command.department academicYear scheme/>">Edit</a>
						<a class="btn btn-danger btn-sm<#if scheme.hasRecordedCheckpoints> disabled use-tooltip</#if>" <#if scheme.hasRecordedCheckpoints>title="This scheme cannot be removed as it has attendance marks against some of its points."</#if> href="<@routes.attendance.manageDeleteScheme command.department academicYear scheme/>">Delete</a>
					</div>
					<strong>${scheme.displayName}</strong>
					<span class="muted">
						<#if scheme.members.members?size == 0>
							(0 students,
						<#else>
							(<a href="<@routes.attendance.manageEditSchemeStudents command.department academicYear scheme />"><@fmt.p scheme.members.members?size "student" /></a>,
						</#if>
						<#if scheme.points?size == 0>
							0 points)
						<#else>
							<a href="<@routes.attendance.manageEditSchemePoints command.department academicYear scheme />"><@fmt.p scheme.points?size "point" /></a>)
						</#if>
					</span>
				</div>
			</div>
		</#list>
	</div>
</div>

</#if>

</#escape>