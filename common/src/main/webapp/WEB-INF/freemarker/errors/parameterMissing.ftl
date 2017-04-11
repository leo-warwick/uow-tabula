<#escape x as x?html>
<h1>Your request could not be handled</h1>

<p>
The request to the server was missing a parameter that was required <#compress>
	<#if exception?? && exception.cause?? && exception.cause.parameterName??>
		("${exception.cause.parameterName}", of type ${exception.cause.parameterType}).
	</#if>
</#compress>
</p>
</#escape>