<#import "*/modal_macros.ftl" as modal />
<#escape x as x?html>

<#function route_function dept>
	<#local result><@routes.admin.notificationsettings dept /></#local>
	<#return result />
</#function>
<@fmt.id7_deptheader "Notification settings" route_function "for" />

<#assign submitUrl><@routes.admin.notificationsettings department /></#assign>
<@f.form method="post" class="notification-settings-form" action=submitUrl commandName="command">
	<input type="hidden" name="returnTo" value="${returnTo}">

	<div class="modal fade" id="small-group-attendance-modal">
		<@modal.wrapper>
			<@modal.header>
				<h3 class="modal-title">Small group notifications</h3>
			</@modal.header>
			<@modal.body>
				<h5>Why are they sent?</h5>

				<p>Attendance for a small group event must be recorded within 5 working days in order
					for it to qualify as evidence for monitoring purposes.</p>

				<p>Tabula will send a notification reminding people to record attendance. Tabula will only
					send these notifications if the groups are set to collect attendance, and if the register
					still needs some attendance recording.</p>

				<h5>When are they sent?</h5>

				<ul>
					<li>When the event has finished</li>
					<li>3 days after the event</li>
					<li>6 days after the event</li>
				</ul>

				<h5>Who will receive them?</h5>

				<p>You can select who will receive notifications when they are turned on, unless an individual
					has opted out of small group attendance notifications.</p>
			</@modal.body>
		</@modal.wrapper>
	</div>

	<div class="striped-section collapsible checkbox-toggle <#if command.smallGroupEventAttendanceReminderEnabled>expanded</#if>">
		<div class="row">
			<div class="col-md-10">
				<h4 class="section-title" style="display: inline-block;">
					Small group attendance notifications
				</h4>
				<a data-toggle="modal" data-target="#small-group-attendance-modal"><i class="fa fa-question-circle"></i></a>
			</div>
			<div class="col-md-2">
				<@bs3form.checkbox path="smallGroupEventAttendanceReminderEnabled">
					<@f.checkbox path="smallGroupEventAttendanceReminderEnabled" id="smallGroupEventAttendanceReminderEnabled" cssClass="toggle-collapsible" />
					Currently
					<span class="toggle-collapsible-on <#if !command.smallGroupEventAttendanceReminderEnabled>hidden</#if>">on</span>
					<span class="toggle-collapsible-off <#if command.smallGroupEventAttendanceReminderEnabled>hidden</#if>">off</span>
				</@bs3form.checkbox>
			</div>
		</div>
		<div class="striped-section-contents">
			<div class="item-info row">
				<div class="col-md-6">
					<p>Send notifications to:</p>

					<@bs3form.checkbox path="smallGroupEventAttendanceReminderNotifyTutors">
						<@f.checkbox path="smallGroupEventAttendanceReminderNotifyTutors" id="smallGroupEventAttendanceReminderNotifyTutors" />
						Tutors for the small group event
					</@bs3form.checkbox>

					<@bs3form.checkbox path="smallGroupEventAttendanceReminderNotifyModuleAssistants">
						<@f.checkbox path="smallGroupEventAttendanceReminderNotifyModuleAssistants" id="smallGroupEventAttendanceReminderNotifyModuleAssistants" />
						Module Assistants
					</@bs3form.checkbox>

					<@bs3form.checkbox path="smallGroupEventAttendanceReminderNotifyModuleManagers">
						<@f.checkbox path="smallGroupEventAttendanceReminderNotifyModuleManagers" id="smallGroupEventAttendanceReminderNotifyModuleManagers" />
						Module Managers
					</@bs3form.checkbox>

					<@bs3form.checkbox path="smallGroupEventAttendanceReminderNotifyDepartmentAdministrators">
						<@f.checkbox path="smallGroupEventAttendanceReminderNotifyDepartmentAdministrators" id="smallGroupEventAttendanceReminderNotifyDepartmentAdministrators" />
						Departmental Administrators
					</@bs3form.checkbox>
				</div>
				<div class="col-md-6">
					<p>Send to all selected groups or only to the first group with someone in it (e.g. Module Managers will only be notified if an event has no tutor).</p>
					<@bs3form.radio>
						<@f.radiobutton path="smallGroupEventAttendanceReminderNotifyFirstNonEmptyGroupOnly" value="false" />
						Notify all selected groups of people
					</@bs3form.radio>
					<@bs3form.radio>
						<@f.radiobutton path="smallGroupEventAttendanceReminderNotifyFirstNonEmptyGroupOnly" value="true" />
						Only notify the first matching group of people
					</@bs3form.radio>
				</div>
			</div>
		</div>
	</div>

	<div class="modal fade" id="coursework-marking-workflow-admin-modal">
		<@modal.wrapper>
			<@modal.header>
				<h3 class="modal-title">Coursework marking workflow notifications</h3>
			</@modal.header>
			<@modal.body>
				<h5>Why are they sent?</h5>

				<p>When marking workflows are used to mark coursework submissions, it can be difficult to
					keep track of when feedback has been "completed" by the final marker and has been sent
					to an administrator, so that it can be released to students.</p>

				<p>Tabula will send a notification when the final marker selects relevant submissions and
					clicks on the "Send to administrator" button.</p>

				<h5>When are they sent?</h5>

				<p>Each time a final marker selects one or more submissions and chooses "send to administrator".</p>

				<h5>Who will receive them?</h5>

				<p>You can select who will receive notifications when they are turned on, unless an individual
					has opted out of marking completed notifications.</p>
			</@modal.body>
		</@modal.wrapper>
	</div>

	<div class="striped-section collapsible checkbox-toggle <#if command.finaliseFeedbackNotificationEnabled>expanded</#if>">
		<div class="row">
			<div class="col-md-10">
				<h4 class="section-title" style="display: inline-block;">
					Coursework marking workflow "send to administrator" notifications
				</h4>
				<a data-toggle="modal" data-target="#coursework-marking-workflow-admin-modal"><i class="fa fa-question-circle"></i></a>
			</div>
			<div class="col-md-2">
				<@bs3form.checkbox path="finaliseFeedbackNotificationEnabled">
					<@f.checkbox path="finaliseFeedbackNotificationEnabled" id="finaliseFeedbackNotificationEnabled" cssClass="toggle-collapsible" />
					Currently
					<span class="toggle-collapsible-on <#if !command.finaliseFeedbackNotificationEnabled>hidden</#if>">on</span>
					<span class="toggle-collapsible-off <#if command.finaliseFeedbackNotificationEnabled>hidden</#if>">off</span>
				</@bs3form.checkbox>
			</div>
		</div>
		<div class="striped-section-contents">
			<div class="item-info row">
				<div class="col-md-6">
					<p>Send notifications to:</p>

					<@bs3form.checkbox path="finaliseFeedbackNotificationNotifyModuleManagers">
						<@f.checkbox path="finaliseFeedbackNotificationNotifyModuleManagers" id="finaliseFeedbackNotificationNotifyModuleManagers" />
						Module Managers
					</@bs3form.checkbox>

					<@bs3form.checkbox path="finaliseFeedbackNotificationNotifyDepartmentAdministrators">
						<@f.checkbox path="finaliseFeedbackNotificationNotifyDepartmentAdministrators" id="finaliseFeedbackNotificationNotifyDepartmentAdministrators" />
						Departmental Administrators
					</@bs3form.checkbox>
				</div>
				<div class="col-md-6">
					<p>Send to all selected groups or only to the first group with someone in it (e.g. Departmental Adminsitrators will only be notified if there are no Module Managers for that module).</p>
					<@bs3form.radio>
						<@f.radiobutton path="finaliseFeedbackNotificationNotifyFirstNonEmptyGroupOnly" value="false" />
						Notify all selected groups of people
					</@bs3form.radio>
					<@bs3form.radio>
						<@f.radiobutton path="finaliseFeedbackNotificationNotifyFirstNonEmptyGroupOnly" value="true" />
						Only notify the first matching group of people
					</@bs3form.radio>
				</div>
			</div>
		</div>
	</div>

	<@bs3form.form_group>
		<input type="submit" value="Save" class="btn btn-primary">
		<a class="btn btn-default" href="${returnTo}">Cancel</a>
	</@bs3form.form_group>
</@f.form>

</#escape>