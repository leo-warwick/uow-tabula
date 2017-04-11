<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />
    <#assign smallGroupSet = releaseGroupSetCommand.singleGroupToPublish/>
    <#assign submitAction><@routes.groups.releaseset smallGroupSet /></#assign>
    <#if smallGroupSet.releasedToStudents >
        <#assign studentReadonly="disabled"/>
    </#if>

	<@modal.wrapper>
		<@f.form method="post" action="${submitAction}" commandName="releaseGroupSetCommand" cssClass="double-submit-protection">

			<@modal.header>
				<h3 class="modal-title">Publish</h3>
			</@modal.header>

			<@modal.body>
				<p>Publish allocations for ${smallGroupSet.name} so they are shown in Tabula to:</p>

				<@bs3form.checkbox "notifyStudents">
					<@f.checkbox path="notifyStudents" disabled=smallGroupSet.releasedToStudents />
					<span class="${smallGroupSet.releasedToStudents?string('disabled use-tooltip" title="Already published','') }">Students</span>
				</@bs3form.checkbox>

				<@bs3form.checkbox "notifyTutors">
					<@f.checkbox path="notifyTutors" disabled=smallGroupSet.releasedToTutors />
					<span class="${smallGroupSet.releasedToTutors?string('disabled','')}">Tutors</span>
				</@bs3form.checkbox>

				<hr>

				<@bs3form.checkbox "sendEmail">
					<@f.checkbox path="sendEmail" />
					Send an email about this and any future changes to group allocation
				</@bs3form.checkbox>
			</@modal.body>

			<@modal.footer>
				<div class="submit-buttons">
					<input class="btn btn-primary spinnable spinner-auto" type="submit" value="Publish" data-loading-text="Loading&hellip;">
					<button class="btn btn-default" data-dismiss="modal">Cancel</button>
				</div>
			</@modal.footer>
		</@f.form>
	</@modal.wrapper>
</#escape>