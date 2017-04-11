<#escape x as x?html>
<h1>${module.name} (${module.code?upper_case})
<br><strong>${assignment.name}</strong></h1>

<#if submission??>
<#if sent>

<p>I've sent a fresh copy of the email to <strong>${user.email}</strong>. If it doesn't show up in your inbox after
  a couple of minutes, check your spam folder in case it has been put in there.</p>

<#elseif hasEmail>

<p>I was unable to send an email to <strong>${user.email}</strong>. Please try again later.</p>

<#else>

<p>I was unable to send a receipt email because you don't have any email address registered. Please contact the
ITS helpdesk who will help you </p>

</#if>
<#else>

<p>You haven't submitted to this assignment.</p>

</#if>

<p><a href="<@routes.coursework.assignment assignment=assignment />">Back to assignment page</a></p>
</#escape>