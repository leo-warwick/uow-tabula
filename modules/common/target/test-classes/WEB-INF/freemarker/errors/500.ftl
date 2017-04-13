<#escape x as x?html>
<h1>Error</h1>

<p>Sorry, there's been a problem and we weren't able to complete your request.</p>

<#if token??>
	<p>The token for this error is <strong>${token}</strong></p>
</#if>

<p>If the problem persists, please contact the <a href="mailto:webteam@warwick.ac.uk">ITS Web Team</a><#if token??>, quoting the token above and any additional details</#if>.</p>

<#if exception??>
	<p><button type="button" class="btn btn-danger" data-toggle="collapse" data-target="#dev">
		Show technical details about this error
	</button></p>

	<pre id="dev" class="collapse" style="overflow-x:scroll;">${stackTrace}</pre>
</#if>
</#escape>