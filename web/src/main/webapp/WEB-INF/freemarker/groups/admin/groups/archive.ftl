<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />
<@modal.wrapper>
	<#assign submitAction><@routes.groups.archiveset smallGroupSet /></#assign>
	<@f.form method="post" action="${submitAction}" commandName="archiveSmallGroupSetCommand" cssClass="double-submit-protection">
		<@modal.header>
			<#if smallGroupSet.archived>
				<h3 class="modal-title">Unarchive these groups</h3>
			<#else>
				<h3 class="modal-title">Archive these groups</h3>
			</#if>
		</@modal.header>

		<@modal.body>
			<#if smallGroupSet.archived>
				<p>You should only unarchive groups that you've archived by mistake.
					If you want to do anything new, you should create new groups.</p>
			<#else>
				<p>Archiving groups will hide them from most lists of things. Students
					will still be able to access their group allocations from archived
					groups.</p>
			</#if>
		</@modal.body>

		<@modal.footer>
			<div class="submit-buttons">
				<#if smallGroupSet.archived>
					<input type="hidden" name="unarchive" value="true" />
					<input class="btn btn-primary spinnable spinner-auto" type="submit" value="Unarchive" data-loading-text="Loading&hellip;">
					<button class="btn btn-default" data-dismiss="modal">Cancel</button>

				<#else>
					<input class="btn btn-primary spinnable spinner-auto" type="submit" value="Archive" data-loading-text="Loading&hellip;">
					<button class="btn btn-default" data-dismiss="modal">Cancel</button>
				</#if>
			</div>
		</@modal.footer>
	</@f.form>
</@modal.wrapper>
</#escape>