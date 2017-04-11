<#ftl strip_text=true />

<#-- Default to ID7 -->
<#assign brand_name = "id7" />

<#assign requestPath = (info.requestedUri.path!"") />

<#if requestPath == '/coursework' || requestPath?starts_with('/coursework/')>
	<#assign brand_name = "id6" />
</#if>

<#-- Query string overrides -->
<#if ((info.requestedUri.query)!"")?contains("id7=true")>
	<#include "id7/base.ftl" />
<#elseif ((info.requestedUri.query)!"")?contains("id6=true")>
	<#include "id6/base.ftl" />
<#else>
	<#include "${brand_name}/base.ftl" />
</#if>