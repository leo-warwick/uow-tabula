<#escape x as x?html>
	<#import "admin_components.ftl" as components />

	<#assign exams = mapGet(examMap, module)![] />
	<#if exams?has_content>
		<@components.admin_exams module exams/>
	<#else>
		<div class="item-info clearfix">
			<p>There are no exams for <@fmt.module_name module false /></p>
		</div>
	</#if>
</#escape>