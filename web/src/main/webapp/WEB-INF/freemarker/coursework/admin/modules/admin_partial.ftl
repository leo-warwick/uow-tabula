<#escape x as x?html>
	<#import "admin_components.ftl" as components />

	<#if (module.assignments!?size gt 0)>
		<@components.admin_assignments module />
	<#else>
		<div class="item-info clearfix">
			<p>There are no assignments for <@fmt.module_name module false /></p>
		</div>
	</#if>
</#escape>