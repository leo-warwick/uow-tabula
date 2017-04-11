<#escape x as x?html>
	<#import "*/group_components.ftl" as components />

	<div class="deptheader">
		<div class="pull-right">
			<@fmt.bulk_email_group smallGroup.students "Email the students attending this event" />
		</div>
		<h1 class="with-settings">Create event</h1>
		<h4 class="with-related"><span class="muted">for</span> ${smallGroup.name}</h4>
	</div>

	<@f.form method="post" action="" commandName="createSmallGroupEventCommand">
		<@f.errors cssClass="error form-errors" />

		<#assign newRecord=true />
		<#include "_event_fields.ftl" />

		<div class="submit-buttons">
			<input
				type="submit"
				class="btn btn-primary"
				name="create"
				value="Save"
			/>
			<a class="btn btn-default" href="${cancelUrl}">Cancel</a>
		</div>
	</@f.form>
</#escape>