<#import "*/group_components.ftl" as components />
<#escape x as x?html>

<#function route_function dept>
	<#local result><@routes.groups.crossmodulegroups dept academicYear /></#local>
	<#return result />
</#function>
<@fmt.id7_deptheader title="Manage reusable small group allocations" route_function=route_function preposition="in" />

<#if sets?size == 0>

	<p class="muted">There are no reusable small groups in your department for ${academicYear.toString}.</p>

	<p>
		<a class="btn btn-primary" href="<@routes.groups.createcrossmodulegroups department academicYear />">Create reusable small groups</a>
	</p>

<#else>

	<h2 style="display: inline-block;">Reusable small group sets</h2>
	<span class="very-subtle">There <@fmt.p number=sets?size singular="is" plural="are" shownumber=false/> <@fmt.p sets?size "reusable small group" /> in your department for ${academicYear.toString}.</span>

	<p>
		<a class="btn btn-default" href="<@routes.groups.createcrossmodulegroups department academicYear />">Create reusable small groups</a>
	</p>

	<#list sets?sort_by("name") as set>
		<div class="row">
			<div class="col-md-9 hover-highlight">
				<div class="pull-right" style="line-height:30px">
					<a class="btn btn-primary btn-xs" href="<@routes.groups.editcrossmodulegroups set />">Edit</a>
					<a class="btn btn-danger btn-xs" href="<@routes.groups.deletecrossmodulegroups set />"><i class="fa fa-times"></i></a>
				</div>
				<span class="lead">${set.name}</span>
				<span class="very-subtle">
					(<a href="<@routes.groups.editcrossmodulegroupsstudents set />"><@fmt.p set.members.members?size "student" /></a>,
					<a href="<@routes.groups.editcrossmodulegroupsgroups set />"><@fmt.p set.groups?size "group" /></a>)
				</span>
			</div>
		</div>
	</#list>

</#if>

</#escape>