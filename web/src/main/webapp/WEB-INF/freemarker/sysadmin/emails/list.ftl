<#escape x as x?html>

	<#assign page = page!1 />

	<#macro paginator>
		<#if page gt 1>
			<a href="?page=${page - 1}">Newer</a>
		</#if>
		<a href="?page=${page + 1}">Older</a>
	</#macro>

	<#macro emailsTable recipients>
		<table class="table table-striped emails-list" width="100%">
			<thead>
			<tr>
				<th>Created</th>
				<th>Subject</th>
				<th>User</th>
				<th>Last attempted to send</th>
				<th>Status</th>
			</tr>
			</thead>
			<tbody>
				<#list recipients as email>
					<tr class="type-${email.notification.class.simpleName}">
						<td><@fmt.date date=email.notification.created seconds=true /></td>
						<td>
							<#if email.notification.safeTitle?has_content>
								${email.notification.safeTitle}
							<#else>
								<span class="subtle">[deleted entity: ${email.notification.class.simpleName}]</span>
							</#if>
						</td>
						<td>
							<#if email.recipient.foundUser>
								${email.recipient.userId}<br>
								<span class="very-subtle">${email.recipient.email}</span>
							</#if>
						</td>
						<td>
							<#if email.attemptedAt?? >
								<@fmt.date date=email.attemptedAt seconds=true />
							<#else>
								Never
							</#if>
						</td>
						<td>
							<#if email.emailSent>
								<span class="label label-success">Sent</span>
							<#else>
								<span class="label label-warning">Waiting</span>
							</#if>
						</td>
					</tr>
				</#list>
			</tbody>
		</table>
	</#macro>

	<h1>Notification emails</h1>

	<div class="alert alert-warning alert-block">
		<h2>Disclaimer</h2>

		<ul>
			<li>The subjects displayed for emails are calculated and displayed at the time of viewing this screen (like how activity
			   streams work) so they may not match up to the email that was sent, if the subject changes based on the current
			   time. For example, a notification that says your assignment is due in 3 days may now say your assignment is 1 day
			   late if this page is viewed 4 days after it's sent.</li>
			<li>The "Created" time is not necessarily the "Sent" time (which we don't record at the moment) - it is the time that
			   the email was pushed onto the queue.</li>
		</ul>
	</div>

	<p>Results ${((page - 1) * 100) + 1} - ${page * 100} <@paginator /></p>

	<@emailsTable emails />
</#escape>