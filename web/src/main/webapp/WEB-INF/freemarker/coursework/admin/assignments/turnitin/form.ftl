<#escape x as x?html>

<p>
	This will send all the submissions in this assignment to Turnitin to be scored for similarity.
	It will take a while so you will get an email when it's done. We'll only upload files that aren't
	already in Turnitin, so it's okay to do this more than once for an assignment.
</p>

<#if errors?? && errors.allErrors?size gt 0>
	<div class="alert alert-error">
		<#list errors.allErrors as error>
			<p><@spring.message code=error.code arguments=error.arguments/></p>
		</#list>
	</div>
<#else>
	<#if incompatibleFiles?size gt 0>
		<div class="alert alert-warning">
			<p>Some attachments won't be sent to Turnitin because they aren't a type that Turnitin is able
				to process (supported types are <strong>MS Word, Acrobat PDF, Postscript, Text, HTML, WordPerfect (WPD) and Rich Text Format</strong>),
				or they are too big to send (maximum Turnitin file size is 20MB)</p>

			<ul class="file-list">
				<#list incompatibleFiles as f>
					<li><i class="icon-file"></i> ${f.name?html}</li>
				</#list>
			</ul>

			<p>You can still continue but these files will be ignored.</p>
		</div>
	</#if>

	<form action="" method="POST">
		<button type="submit" class="btn btn-primary">Send to Turnitin</button>
	</form>
</#if>

</#escape>