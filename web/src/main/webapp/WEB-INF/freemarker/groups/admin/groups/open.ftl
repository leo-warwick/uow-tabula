<#escape x as x?html>
	<#import "*/modal_macros.ftl" as modal />
	<#assign setState = openGroupSetCommand.setState/>
    <#assign smallGroupSet = openGroupSetCommand.singleSetToOpen/>
    <#if openGroupSetCommand.setState.name == 'open'>
        <#assign submitAction><@routes.groups.openset smallGroupSet /></#assign>
        <#assign setState = "Open"/>
    <#else>
        <#assign submitAction><@routes.groups.closeset smallGroupSet /></#assign>
        <#assign setState = "Close"/>
    </#if>

	<@modal.wrapper>
		<@f.form method="post" action="${submitAction}" commandName="openGroupSetCommand" cssClass="double-submit-protection">

			<@modal.header>
				<h3 class="modal-title">${setState}</h3>
			</@modal.header>

			<@modal.body>
				<p>${setState} ${smallGroupSet.format.description} for ${smallGroupSet.module.code?upper_case} for self sign-up.
					<#if setState == "Open"> Students will be notified via email that they can now sign up for these groups in Tabula.</#if>
				</p>
			</@modal.body>

			<@modal.footer>
				<div class="submit-buttons">
					<input class="btn btn-primary spinnable spinner-auto" type="submit" value="${setState}" data-loading-text="Loading&hellip;">
					<button class="btn btn-default" data-dismiss="modal">Cancel</button>
				</div>
			</@modal.footer>
		</@f.form>
	</@modal.wrapper>

</#escape>