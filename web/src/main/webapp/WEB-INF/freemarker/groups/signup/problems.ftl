<#-- Displays validation errors for self sign-up. -->

<h1>
<#if action="signup">
	Sign up to
<#else>
	Leave
</#if>
${(command.group.name)!'group'}
</h1>

<p>We couldn't do this due to these problems:</p>

<p>
<@f.errors path="command.*" cssClass="error" />
</p>

<p>
	<a class="btn" href="<@routes.groups.home />">Back</a>
</p>