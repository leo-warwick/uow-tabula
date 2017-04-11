<#assign heading>
<h2>Record a meeting</h2>
</#assign>

<#assign body>
	<div class="alert alert-danger">
		The meeting record that you are trying to convert has been deleted.
	</div>
</#assign>

<#if modal!false>
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
		${heading}
	</div>
<#elseif iframe!false>
	<div id="container">
<#else>
	${heading}
</#if>
<#if modal!false>
	<div class="modal-body">${body}</div>
	<div class="modal-footer">
		<form class="double-submit-protection">
			<span class="submit-buttons">
				<button class="btn" data-dismiss="modal" aria-hidden="true">Close</button>
			</span>
		</form>
	</div>
<#else>
	${body}
</#if>
<#if iframe!false>
	</div> <#--container -->
</#if>