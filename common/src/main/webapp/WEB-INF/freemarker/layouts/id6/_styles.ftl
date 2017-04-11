<#-- Styles that should be included both in the app and embedded in to Sitebuilder -->
<@stylesheet "/static/css/bootstrap.css" />

<#if info?? && info.requestedUri?? && info.requestedUri.getQueryParameter("debug")??>
	<@stylesheet "/static/css/concat6.css" />
	<@stylesheet "/static/css/main.css" />
	<@stylesheet "/static/libs/bootstrap-editable/css/bootstrap-editable.css" />
	<@stylesheet "/static/libs/bootstrap-datetimepicker/css/datetimepicker.css" />
	<@stylesheet "/static/libs/popup/popup.css" />
	<@stylesheet "/static/libs/jquery-rating/jquery.rating.css" />
<#else>
	<@stylesheet "/static/css/render.css" />
</#if>

<link href='//fonts.googleapis.com/css?family=Bitter:400,700,400italic&subset=latin,latin-ext' rel='stylesheet' type='text/css'>

<#--
	Uncomment these lines to use our Typekit font set for Tabula, which has museo-slab:700

	<script type="text/javascript" src="//use.typekit.net/yym6hpx.js"></script>
	<script type="text/javascript">try{Typekit.load();}catch(e){}</script>
-->

<link rel="stylesheet" title="No Accessibility" href="<@url resource="/static/css/noaccessibility.css"/>" type="text/css">
<link rel="alternate stylesheet" title="Show Accessibility" href="<@url resource="/static/css/showaccessibility.css"/>" type="text/css">

<!--[if lt IE 8]>
<@stylesheet "/static/css/ielt8.css" />
<![endif]-->
<!--[if lt IE 9]>
<style type="text/css">
	#container {
	behavior: url(/static/css/pie.htc);
	}
</style>
<![endif]-->