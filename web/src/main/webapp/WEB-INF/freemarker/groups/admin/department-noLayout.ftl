<#import "*/group_components.ftl" as components />
<#escape x as x?html>
	<#if sets?has_content>
		<#-- This is the big list of sets -->
		<@components.sets_info sets false />

		<#if hasMoreSets>
			Tabula only shows the first 10 groups before filtering. To see more use the filter above.
		</#if>
	<#else>
	<p class="alert alert-info">No small groups sets found.</p>
	</#if>
</#escape>
