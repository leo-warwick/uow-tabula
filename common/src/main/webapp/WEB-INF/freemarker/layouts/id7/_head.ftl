<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">

<!-- Include any favicons here -->
<link rel="apple-touch-icon" sizes="57x57" href="<@url resource="/static/images/id7/favicons/apple-touch-icon-57x57.png" />">
<link rel="apple-touch-icon" sizes="60x60" href="<@url resource="/static/images/id7/favicons/apple-touch-icon-60x60.png" />">
<link rel="apple-touch-icon" sizes="72x72" href="<@url resource="/static/images/id7/favicons/apple-touch-icon-72x72.png" />">
<link rel="apple-touch-icon" sizes="76x76" href="<@url resource="/static/images/id7/favicons/apple-touch-icon-76x76.png" />">
<link rel="apple-touch-icon" sizes="114x114" href="<@url resource="/static/images/id7/favicons/apple-touch-icon-114x114.png" />">
<link rel="apple-touch-icon" sizes="120x120" href="<@url resource="/static/images/id7/favicons/apple-touch-icon-120x120.png" />">
<link rel="apple-touch-icon" sizes="144x144" href="<@url resource="/static/images/id7/favicons/apple-touch-icon-144x144.png" />">
<link rel="apple-touch-icon" sizes="152x152" href="<@url resource="/static/images/id7/favicons/apple-touch-icon-152x152.png" />">
<link rel="apple-touch-icon" sizes="180x180" href="<@url resource="/static/images/id7/favicons/apple-touch-icon-180x180.png" />">
<link rel="icon" type="image/png" href="<@url resource="/static/images/id7/favicons/favicon-32x32.png" />" sizes="32x32">
<link rel="icon" type="image/png" href="<@url resource="/static/images/id7/favicons/android-chrome-192x192.png" />" sizes="192x192">
<link rel="icon" type="image/png" href="<@url resource="/static/images/id7/favicons/favicon-96x96.png" />" sizes="96x96">
<link rel="icon" type="image/png" href="<@url resource="/static/images/id7/favicons/favicon-16x16.png" />" sizes="16x16">
<link rel="manifest" href="<@url resource="/static/images/id7/favicons/manifest.json" />">
<link rel="shortcut icon" href="<@url resource="/static/images/id7/favicons/favicon.ico" />">
<meta name="msapplication-TileColor" content="#239b92">
<meta name="msapplication-TileImage" content="<@url resource="/static/images/id7/favicons/mstile-144x144.png" />">
<meta name="msapplication-config" content="<@url resource="/static/images/id7/favicons/browserconfig.xml" />">
<meta name="theme-color" content="#239b92">
<!-- Use the brand colour of the site -->

<title><#compress>
	<#if component.subsite>Tabula - </#if>
	${component.title?default('Tabula')}
	<#if breadcrumbs??>
		<#if siblingBreadcrumbs!false>
			- ${breadcrumbs?first.title}
		<#else>
			<#list breadcrumbs as crumb> - ${crumb.title}</#list></#if><#if pageTitle??> - ${pageTitle}
		</#if>
	</#if>
</#compress></title>

<!-- Lato web font -->
<link href="//fonts.googleapis.com/css?family=Lato:300,400,700,300italic,400italic,700italic&amp;subset=latin,latin-ext"
	  rel="stylesheet" type="text/css">

<@stylesheet "/static/id7/css/id7.min.css" />
<@stylesheet "/static/css/id7/render.css" />
<@stylesheet "/static/css/id7/${component.name?default('common')}.css" />

<@script "/static/js/id7/render.js" />
<#if info?? && info.requestedUri?? && info.requestedUri.getQueryParameter("debug")??>
	<#include "components/${component.name?default('common')}.ftl" />
<#else>
	<@script "/static/js/id7/${component.name?default('common')}.js" />
</#if>

<!-- HTML5 shim for IE8 support of HTML5 elements -->

<!--[if lt IE 9]>
<@script "/static/js/id7/vendor/html5shiv-3.7.2.min.js" />
<![endif]-->