<#escape x as x?html>
<div class="row-fluid">
	<div id="maintenance-message" class="span8 offset2">
		<img src="<@url resource="/static/images/cogs.png" />">

		<h1>API Authentication Method Required</h1>

		<p>
			API requests can only be made via HTTP Basic Auth or OAuth.
		</p>

		<#if token??>
			<p>The token for this error is <strong>${token}</strong>.</p>
		</#if>
	</div>
</div>
</#escape>