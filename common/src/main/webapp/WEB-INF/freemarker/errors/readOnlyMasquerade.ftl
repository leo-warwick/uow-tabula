<#escape x as x?html>
<#if JspTaglibs??>
	<#import "../formatters.ftl" as fmt />
</#if>
<div class="row-fluid">
	<div id="maintenance-message" class="span8 offset2">
		<img src="<@url resource="/static/images/cogs.png" />">

		<h1>Cannot perform write operations while masquerading</h1>

		<p>It's not possible to perform any operation that modifies Tabula while masquerading.</p>

	</div>
</div>
</#escape>