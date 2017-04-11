<#escape x as x?html>
<#if JspTaglibs??>
	<#import "../formatters.ftl" as fmt />
</#if>
<div class="row-fluid">
	<div id="maintenance-message">

		<h1>Tabula system status</h1>

		<p>Tabula is currently in 'view only' mode while we carry out some maintenance work. You can view your profile, assignments, groups and attendance as usual. However, it is not possible to submit assignments or edit any information in Tabula.</p>
		<p>
		<#if exception.until??></p>
			We expect to restore the full service by <strong><@fmt.date date=exception.until capitalise=false at=true relative=true /></strong>.
		<#else>
			Normal access should be restored soon.
		</#if>
		</p>

		<h2>Assignment submissions</h2>

		<p>Don't worry if you're trying to submit an assignment. Any assignment deadlines which occur while Tabula is unavailable will be extended, or alternative provision made, so you can submit your assignment without being penalised. Either the Web Team or you Assignment Tutor will contact you via email to explain what you should do next.</p>

		<#if exception.messageOrEmpty != "">
			<p>${exception.messageOrEmpty}</p>
		</#if>

		<p>Need more help? Please contact <a href="mailto:webteam@warwick.ac.uk">webteam@warwick.ac.uk</a></p>
	</div>
</div>
</#escape>