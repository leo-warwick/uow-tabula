<#escape x as x?html>
<div class="row-fluid">
	<div id="maintenance-message" class="span8 offset2">
		<img src="<@url resource="/static/images/cogs.png" />">

		<h1>Files too large to compress</h1>

		<p>
			The files that you're trying to download are too large to compress into one file.
			It may be possible to download the files individually.
		</p>

		<#if token??>
			<p>The token for this error is <strong>${token}</strong>.</p>
		</#if>
	</div>
</div>
</#escape>