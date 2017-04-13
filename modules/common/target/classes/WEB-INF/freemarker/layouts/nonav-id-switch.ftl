<#if ((info.requestedUri.query)!"")?contains("id7=true")>
	<#include "id7/nonav.ftl" />
<#else>
	<#include "id6/nonav.ftl" />
</#if>