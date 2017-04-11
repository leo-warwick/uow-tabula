<#import "*/group_components.ftl" as components />
<#escape x as x?html>
<div id="student-groups-view">
	<h4>${title}</h4>
	<#if data.moduleItems?size == 0>
		<em>There are no groups to show right now</em>
	</#if>
	<@components.module_info data />
</div>
</#escape>
