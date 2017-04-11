<#escape x as x?html>

<h1>Delete monitoring point</h1>

<#assign popoverContent><#noescape>
	<ul>
		<#list command.schemesToEdit?sort_by("displayName") as scheme>
			<li>${scheme.displayName}</li>
		</#list>
	</ul>
</#noescape></#assign>

<p>
	You are deleting this point on
	<a href="#" class="use-popover" data-content="${popoverContent}" data-html="true" data-placement="top">
		<@fmt.p command.schemesToEdit?size "scheme" />:
	</a>
</p>
<p>
	${command.templatePoint.name}
	(<@fmt.interval command.templatePoint.startDate command.templatePoint.endDate />)
</p>

<@f.form action="" method="POST" commandName="command">

	<@f.errors cssClass="error form-errors" />

	<input name="returnTo" value="${returnTo}" type="hidden" />

	<div class="form-actions">
		<button class="btn btn-danger spinnable spinner-auto" type="submit" name="submit" data-loading-text="Deleting&hellip;">
			Delete
		</button>
		<a href="${returnTo}" class="btn btn-default">Cancel</a>
	</div>
</@f.form>


</#escape>