<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign warwick=JspTaglibs["/WEB-INF/tld/warwick.tld"]>
<#escape x as x?html>

<h1>${module.code?upper_case} (${module.name})</h1>

<#if assignments?size gt 0>

<#list assignments as assignment>

	<div class="assignment ${assignment.alive?string("active", "inactive")}">
		<h2>
		<#if assignment.alive>
			<a href="${url('/coursework/module/${module.code}/${assignment.id}')}">${assignment.name}</a>
		<#else>
			${assignment.name}
		</#if>
		</h2>
		<div class="date end-date">
			<#if assignment.openEnded>
				Open-ended submission
			<#else>
				Submission ${assignment.alive?string("closes", "closed")}
				<@fmt.date date=assignment.closeDate seconds=true />
			</#if>
		</div>
	</div>

</#list>
<#else>
<p>It seems that you're on this module, but no assignments have been set up here.</p>

<p>See your module tutor if you have an assignment to submit and aren't sure how you
should be submitting it.</p>
</#if>

</#escape>