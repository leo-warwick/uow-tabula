<title><#if component.subsite>Tabula - </#if>${component.title?default('Tabula')}<#if breadcrumbs??><#list breadcrumbs as crumb> - ${crumb.title}</#list></#if><#if pageTitle??> - ${pageTitle}</#if></title>

<meta http-equiv="X-UA-Compatible" content="IE=Edge,chrome=1" >
<meta charset="utf-8">
<meta name="robots" content="noindex,nofollow">

<meta name="HandheldFriendly" content="True">
<meta id="meta-mobile-optimized" name="MobileOptimized" content="320">
<meta id="meta-viewport" name="viewport" content="width=device-width">
<meta http-equiv="cleartype" content="on">

<#include "_styles.ftl" />
<@stylesheet "/static/css/${component.name?default('common')}.css" />

<#include "_scripts.ftl" />
<#if info?? && info.requestedUri?? && info.requestedUri.getQueryParameter("debug")??>
	<#include "components/${component.name?default('common')}.ftl" />
<#else>
	<@script "/static/js/${component.name?default('common')}.js" />
</#if>