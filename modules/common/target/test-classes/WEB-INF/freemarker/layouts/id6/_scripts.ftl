<#-- Scripts that should be included both in the app and embedded in to Sitebuilder -->
<#if info?? && info.requestedUri?? && info.requestedUri.getQueryParameter("debug")??>
	<@script "/static/libs/jquery/jquery-1.8.3.js" />
	<@script "/static/js/id6scripts.js" />
	<@script "/static/libs/jquery-ui/js/jquery-ui-1.9.2.custom.js" />
	<@script "/static/libs/jquery.delayedObserver.js" />
	<@script "/static/libs/jquery-caret/jquery.caret.1.02.js" />
	<@script "/static/libs/jquery-fixedheadertable/jquery.fixedheadertable.js" />
	<@script "/static/libs/jquery-rating/jquery.rating.pack.js" />
	<@script "/static/libs/jquery-tablesorter/jquery.tablesorter.js" />
	<@script "/static/js/moment.js" />
	<@script "/static/js/moment-timezone-london.js" />
	<@script "/static/libs/popup/popup.js" />
	<@script "/static/libs/bootstrap/js/bootstrap.js" />
	<@script "/static/libs/bootstrap-editable/js/bootstrap-editable.js" />
	<@script "/static/libs/bootstrap-datetimepicker/js/bootstrap-datetimepicker.js" />
	<@script "/static/libs/spin-js/spin.js" />
	<@script "/static/libs/spin-js/jquery.spin.js" />
	<@script "/static/js/modernizr.js" />
	<@script "/static/js/browser-info.js" />
	<@script "/static/js/activity-streams.js" />
<#else>
	<@script "/static/js/render.js" />
</#if>