<#if ((info.requestedUri.query)!"")?contains("id7=true")>
	<#include "id7/base.ftl" />
<#else>
	<#include "id6/base.ftl" />
</#if>