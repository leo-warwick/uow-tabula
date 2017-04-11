<#escape x as x?html>
<h1>User settings for ${user.firstName}</h1>

<#if success!false>
	<div class="alert alert-info">Your preferred settings have been updated.</div>
</#if>
<@f.form method="post" action="${url('/settings')}" commandName="userSettingsCommand">

	<#if isCourseworkModuleManager || isDepartmentalAdmin>
		<@bs3form.labelled_form_group path="alertsSubmission" labelText="Submission email alerts">
			<@bs3form.radio>
				<@f.radiobutton path="alertsSubmission" value="allSubmissions" />
				All submissions
			</@bs3form.radio>
			<@bs3form.radio>
				<@f.radiobutton path="alertsSubmission" value="lateSubmissions" />
				Noteworthy submissions (late, late within extension, suspected plagiarism)
			</@bs3form.radio>
			<@bs3form.radio>
				<@f.radiobutton path="alertsSubmission" value="none" />
				No alerts
			</@bs3form.radio>
		</@bs3form.labelled_form_group>
	</#if>

	<@bs3form.labelled_form_group path="weekNumberingSystem" labelText="Week numbering system">
		<@bs3form.radio>
				<@f.radiobutton path="weekNumberingSystem" value="" />
				Use the department's choice of week numbering system
		</@bs3form.radio>
		<@bs3form.radio>
			<@f.radiobutton path="weekNumberingSystem" value="term" />
			Count weeks from 1-10 for each term (the first week of the Spring term is Term 2, week 1)
		</@bs3form.radio>
		<@bs3form.radio>
			<@f.radiobutton path="weekNumberingSystem" value="cumulative" />
			Count term weeks cumulatively (the first week of the Spring term is Term 2, week 11)
		</@bs3form.radio>
		<@bs3form.radio>
			<@f.radiobutton path="weekNumberingSystem" value="academic" />
			Use academic week numbers, including vacations (the first week of the Spring term is week 15)
		</@bs3form.radio>
		<@bs3form.radio>
			<@f.radiobutton path="weekNumberingSystem" value="none" />
			Use no week numbers, displaying dates instead
		</@bs3form.radio>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="bulkEmailSeparator" labelText="Bulk email separator">
		<@bs3form.radio>
			<@f.radiobutton path="bulkEmailSeparator" value=";" />
			Semi-colon (Microsoft Outlook)
		</@bs3form.radio>
		<@bs3form.radio>
			<@f.radiobutton path="bulkEmailSeparator" value="," />
			Comma (Thunderbird, Gmail, Apple Mail)
		</@bs3form.radio>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group labelText="Notifications">
		<p>Send me notifications about:</p>

		<@bs3form.checkbox>
			<@f.checkbox path="smallGroupEventAttendanceReminderEnabled" />
			Small group attendance
		</@bs3form.checkbox>

		<@bs3form.checkbox>
			<@f.checkbox path="finaliseFeedbackNotificationEnabled" />
			Coursework submissions which have been marked
		</@bs3form.checkbox>

	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group>
		<input type="submit" value="Save" class="btn btn-primary">
		<a class="btn btn-default" href="${url('/')}">Cancel</a>
	</@bs3form.labelled_form_group>

</@f.form>
</#escape>